package com.nunomonteiro.notificationwebhooks.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nunomonteiro.notificationwebhooks.data.model.*
import com.nunomonteiro.notificationwebhooks.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class WebhookWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WebhookWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notificationJson = inputData.getString("notification")
        val ruleJson = inputData.getString("rule")
        val notificationId = inputData.getLong("notificationId", 0)
        
        val repository = NotificationRepository(applicationContext)

        Log.d(TAG, "======================================")
        Log.d(TAG, "🚀 A enviar webhook...")

        if (notificationJson.isNullOrEmpty() || ruleJson.isNullOrEmpty()) {
            Log.e(TAG, "❌ Dados incompletos")
            return@withContext Result.failure()
        }

        try {
            val notification = Gson().fromJson(notificationJson, Notification::class.java)
            val rule = Gson().fromJson(ruleJson, Rule::class.java)
            
            // Carregar configurações avançadas
            val securityConfig = if (!rule.securityConfig.isNullOrEmpty()) {
                Gson().fromJson(rule.securityConfig, SecurityConfig::class.java)
            } else SecurityConfig()
            
            val advancedConfig = if (!rule.advancedConfig.isNullOrEmpty()) {
                Gson().fromJson(rule.advancedConfig, AdvancedWebhookConfig::class.java)
            } else AdvancedWebhookConfig()
            
            Log.d(TAG, "📦 URL: ${rule.webhookUrl}")
            Log.d(TAG, "🔐 HMAC: ${securityConfig.signWithHmac}")
            Log.d(TAG, "🔑 Auth: ${securityConfig.auth.type}")
            Log.d(TAG, "⏱️ Timeout: ${advancedConfig.timeoutSeconds}s")
            
            // Construir payload
            val payload = construirPayload(notification, rule, advancedConfig)
            val jsonPayload = Gson().toJson(payload)
            Log.d(TAG, "📦 Payload: $jsonPayload")
            
            // Configurar cliente HTTP com timeout personalizado
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(advancedConfig.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .writeTimeout(advancedConfig.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            
            if (advancedConfig.waitForResponse) {
                clientBuilder.readTimeout(30, TimeUnit.SECONDS)
            } else {
                clientBuilder.readTimeout(5, TimeUnit.SECONDS)
            }
            
            val client = clientBuilder.build()

            // Construir request com headers
            val requestBuilder = Request.Builder()
                .url(rule.webhookUrl ?: "")
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))

            // Adicionar headers personalizados
            adicionarHeaders(requestBuilder, rule, securityConfig)
            
            // Adicionar HMAC se ativo
            if (securityConfig.signWithHmac && !securityConfig.hmacSecret.isNullOrEmpty()) {
                val signature = gerarHmacSignature(jsonPayload, securityConfig.hmacSecret)
                requestBuilder.addHeader("X-Signature-SHA256", signature)
                Log.d(TAG, "🔏 HMAC adicionado")
            }

            val request = requestBuilder.build()
            Log.d(TAG, "⏳ A enviar...")
            
            val response = client.newCall(request).execute()
            val responseBody = if (advancedConfig.waitForResponse) response.body?.string() else null
            
            Log.d(TAG, "📥 Response code: ${response.code}")
            if (advancedConfig.waitForResponse) {
                Log.d(TAG, "📥 Response body: $responseBody")
            }
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Webhook enviado com sucesso!")
                atualizarStatusSucesso(repository, notificationId, responseBody)
                Log.d(TAG, "======================================")
                return@withContext Result.success()
            } else {
                Log.e(TAG, "❌ Erro no webhook: ${response.code}")
                atualizarStatusErro(repository, notificationId, "HTTP ${response.code}")
                Log.d(TAG, "======================================")
                return@withContext Result.retry()
            }

        } catch (e: IOException) {
            Log.e(TAG, "❌ Erro de rede: ${e.message}")
            atualizarStatusErro(repository, notificationId, "Erro de rede: ${e.message}")
            return@withContext Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro inesperado: ${e.message}")
            e.printStackTrace()
            atualizarStatusErro(repository, notificationId, "Erro: ${e.message}")
            return@withContext Result.failure()
        }
    }
    
    /**
     * Constrói o payload com base na configuração (padrão ou personalizado)
     */
    private fun construirPayload(notification: Notification, rule: Rule, config: AdvancedWebhookConfig): Any {
        return if (config.useCustomPayload && !config.customPayloadTemplate.isNullOrEmpty()) {
            // Substituir variáveis no template personalizado
            var template = config.customPayloadTemplate!!
            template = template.replace("{{notification.id}}", notification.id.toString())
            template = template.replace("{{notification.title}}", notification.title ?: "")
            template = template.replace("{{notification.text}}", notification.text ?: "")
            template = template.replace("{{notification.package}}", notification.packageName)
            template = template.replace("{{rule.id}}", rule.id.toString())
            template = template.replace("{{rule.name}}", rule.name)
            template = template.replace("{{payload}}", Gson().toJson(payloadPadrao(notification, rule)))
            
            // Tentar converter para Map ou manter como String
            try {
                Gson().fromJson(template, object : TypeToken<Map<String, Any>>() {}.type)
            } catch (e: Exception) {
                template // Se não for JSON válido, enviar como string
            }
        } else {
            // Payload padrão
            payloadPadrao(notification, rule)
        }
    }
    
    /**
     * Payload padrão do sistema
     */
    private fun payloadPadrao(notification: Notification, rule: Rule): Map<String, Any> {
        return mapOf(
            "notification" to mapOf(
                "id" to notification.id,
                "packageName" to notification.packageName,
                "title" to (notification.title ?: ""),
                "text" to (notification.text ?: ""),
                "timestamp" to notification.timestamp,
                "status" to (notification.status.toString())
            ),
            "rule" to mapOf(
                "id" to rule.id,
                "name" to rule.name
            ),
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    /**
     * Adiciona headers de autenticação conforme configuração
     */
    private fun adicionarHeaders(requestBuilder: Request.Builder, rule: Rule, config: SecurityConfig) {
        // Headers personalizados da regra
        if (!rule.webhookHeaders.isNullOrEmpty()) {
            try {
                val headers = Gson().fromJson<Map<String, String>>(rule.webhookHeaders, Map::class.java)
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Headers inválidos: ${e.message}")
            }
        }
        
        // Headers de autenticação
        when (config.auth.type) {
            AuthType.BASIC -> {
                if (!config.auth.username.isNullOrEmpty() && !config.auth.password.isNullOrEmpty()) {
                    val credentials = "${config.auth.username}:${config.auth.password}"
                    val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                    requestBuilder.addHeader("Authorization", "Basic $encoded")
                }
            }
            AuthType.BEARER -> {
                if (!config.auth.token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer ${config.auth.token}")
                }
            }
            AuthType.API_KEY -> {
                if (!config.auth.apiKeyName.isNullOrEmpty() && !config.auth.apiKeyValue.isNullOrEmpty()) {
                    requestBuilder.addHeader(config.auth.apiKeyName, config.auth.apiKeyValue)
                }
            }
            else -> { /* Sem autenticação */ }
        }
    }
    
    /**
     * Gera assinatura HMAC-SHA256 do payload
     */
    private fun gerarHmacSignature(payload: String, secret: String): String {
        return try {
            val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(key)
            val signatureBytes = mac.doFinal(payload.toByteArray())
            Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar HMAC: ${e.message}")
            ""
        }
    }
    
    private suspend fun atualizarStatusSucesso(
        repository: NotificationRepository,
        notificationId: Long,
        response: String?
    ) {
        if (notificationId > 0) {
            repository.atualizarWebhookStatus(
                notificationId,
                WebhookStatus.SUCCESS,
                response
            )
        }
    }
    
    private suspend fun atualizarStatusErro(
        repository: NotificationRepository,
        notificationId: Long,
        error: String
    ) {
        if (notificationId > 0) {
            repository.atualizarWebhookStatus(
                notificationId,
                WebhookStatus.FAILED,
                null,
                error
            )
        }
    }
}