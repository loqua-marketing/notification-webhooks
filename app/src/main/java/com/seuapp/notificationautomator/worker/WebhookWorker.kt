package com.seuapp.notificationautomator.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.data.model.WebhookStatus
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

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
        val webhookUrl = inputData.getString("webhookUrl")
        val headersJson = inputData.getString("headers")
        val notificationId = inputData.getLong("notificationId", 0)
        
        val repository = NotificationRepository(applicationContext)

        Log.d(TAG, "======================================")
        Log.d(TAG, "🚀 A enviar webhook...")
        Log.d(TAG, "📦 URL: $webhookUrl")
        Log.d(TAG, "📝 Notification ID: $notificationId")

        if (webhookUrl.isNullOrEmpty()) {
            Log.e(TAG, "❌ URL do webhook vazio")
            atualizarStatusErro(repository, notificationId, "URL vazio")
            return@withContext Result.failure()
        }

        try {
            val notification = notificationJson?.let { Gson().fromJson(it, Notification::class.java) }
            val rule = ruleJson?.let { Gson().fromJson(it, Rule::class.java) }
            
            val payload = mapOf(
                "notification" to mapOf(
                    "id" to (notification?.id),
                    "packageName" to (notification?.packageName),
                    "title" to (notification?.title),
                    "text" to (notification?.text),
                    "timestamp" to (notification?.timestamp),
                    "status" to (notification?.status?.toString())
                ),
                "rule" to mapOf(
                    "id" to (rule?.id),
                    "name" to (rule?.name)
                ),
                "timestamp" to System.currentTimeMillis()
            )

            val jsonPayload = Gson().toJson(payload)
            Log.d(TAG, "📦 Payload: $jsonPayload")
            
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder()
                .url(webhookUrl)
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))

            if (!headersJson.isNullOrEmpty()) {
                try {
                    val headers = Gson().fromJson<Map<String, String>>(headersJson, Map::class.java)
                    headers.forEach { (key, value) ->
                        requestBuilder.addHeader(key, value)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Headers inválidos: ${e.message}")
                }
            }

            val request = requestBuilder.build()
            Log.d(TAG, "⏳ A enviar...")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "📥 Response code: ${response.code}")
            Log.d(TAG, "📥 Response body: $responseBody")
            
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
