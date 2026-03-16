package com.nunomonteiro.notificationwebhooks.ml.classification

import android.content.Context
import android.util.Log
import com.nunomonteiro.notificationwebhooks.ml.config.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class NotificationClassifier(private val context: Context) {
    
    private val TAG = "NotificationClassifier"
    
    enum class Category(val value: String, val displayName: String) {
        URGENT("urgent", "🔴 Urgente"),
        TRANSACTION("transaction", "💰 Transação"),
        PROMOTION("promotion", "🎯 Promoção"),
        SOCIAL("social", "👥 Social"),
        OTHER("other", "📋 Outro")
    }
    
    data class ClassificationResult(
        val category: Category,
        val confidence: Float,
        val inferenceTimeMs: Long,
        val success: Boolean = true,
        val error: String? = null
    ) {
        companion object {
            fun error(errorMessage: String): ClassificationResult {
                return ClassificationResult(
                    category = Category.OTHER,
                    confidence = 0f,
                    inferenceTimeMs = 0,
                    success = false,
                    error = errorMessage
                )
            }
        }
    }
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    suspend fun initialize(modelPath: String = "classifier.tflite"): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            if (!FeatureFlags.CLASSIFICATION_ENABLED) {
                Log.d(TAG, "🔇 ML Classifier desativado")
                return@runCatching
            }
            
            Log.d(TAG, "📂 A carregar modelo: $modelPath")
            val startTime = System.currentTimeMillis()
            
            // Carregar modelo usando FileUtil (mais robusto)
            val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(modelBuffer)
            
            isInitialized = true
            val initTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ ML Classifier inicializado em ${initTime}ms")
        }
    }
    
    suspend fun classify(
        title: String?,
        text: String?
    ): ClassificationResult = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "🔍 A classificar: título='$title', texto='$text'")
        
        if (!FeatureFlags.CLASSIFICATION_ENABLED || !isInitialized || interpreter == null) {
            Log.d(TAG, "⚠️ ML não disponível (enabled=$FeatureFlags.CLASSIFICATION_ENABLED, initialized=$isInitialized)")
            return@withContext ClassificationResult(
                category = Category.OTHER,
                confidence = 0f,
                inferenceTimeMs = 0
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            val inputText = buildString {
                if (!title.isNullOrBlank()) append("$title ")
                if (!text.isNullOrBlank()) append(text)
            }.trim()
            
            Log.d(TAG, "📝 Texto para classificar: '$inputText'")
            
            if (inputText.isBlank()) {
                Log.d(TAG, "⚠️ Texto vazio, categoria OTHER")
                return@withContext ClassificationResult(
                    category = Category.OTHER,
                    confidence = 0f,
                    inferenceTimeMs = System.currentTimeMillis() - startTime
                )
            }
            
            // Classificação baseada em palavras-chave
            val category = when {
                inputText.contains("urgente", ignoreCase = true) ||
                inputText.contains("emergência", ignoreCase = true) ||
                inputText.contains("vencido", ignoreCase = true) -> {
                    Log.d(TAG, "✅ Palavra-chave URGENT encontrada")
                    Category.URGENT
                }
                
                inputText.contains("transferência", ignoreCase = true) ||
                inputText.contains("pagamento", ignoreCase = true) ||
                inputText.contains("recebido", ignoreCase = true) -> {
                    Log.d(TAG, "✅ Palavra-chave TRANSACTION encontrada")
                    Category.TRANSACTION
                }
                
                inputText.contains("promoção", ignoreCase = true) ||
                inputText.contains("desconto", ignoreCase = true) ||
                inputText.contains("oferta", ignoreCase = true) -> {
                    Log.d(TAG, "✅ Palavra-chave PROMOTION encontrada")
                    Category.PROMOTION
                }
                
                inputText.contains("whatsapp", ignoreCase = true) ||
                inputText.contains("mensagem", ignoreCase = true) ||
                inputText.contains("comentário", ignoreCase = true) -> {
                    Log.d(TAG, "✅ Palavra-chave SOCIAL encontrada")
                    Category.SOCIAL
                }
                
                else -> {
                    Log.d(TAG, "❌ Nenhuma palavra-chave encontrada")
                    Category.OTHER
                }
            }
            
            val confidence = if (category != Category.OTHER) 0.8f else 0.3f
            val inferenceTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "🎯 Resultado: ${category.value} (confiança=$confidence) em ${inferenceTime}ms")
            
            ClassificationResult(
                category = category,
                confidence = confidence,
                inferenceTimeMs = inferenceTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na classificação", e)
            ClassificationResult.error(e.message ?: "Erro desconhecido")
        }
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}