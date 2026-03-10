package com.seuapp.notificationautomator.ml.classification

import android.content.Context
import com.seuapp.notificationautomator.ml.config.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import java.io.File
import java.io.FileOutputStream

/**
 * Classificador de notificações usando TensorFlow Lite
 * Versão simplificada - sem opções complexas
 */
class NotificationClassifier(private val context: Context) {
    
    enum class Category(val value: String, val displayName: String) {
        URGENT("urgent", "🔴 Urgente"),
        TRANSACTION("transaction", "💰 Transação"),
        PROMOTION("promotion", "🎯 Promoção"),
        SOCIAL("social", "👥 Social"),
        OTHER("other", "📋 Outro");
        
        companion object {
            fun fromString(value: String): Category {
                return entries.find { it.value.equals(value, ignoreCase = true) } ?: OTHER
            }
        }
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
    
    private var classifier: BertNLClassifier? = null
    private var isInitialized = false
    
    // Inicializa o classificador (só se ML estiver ativo)
    suspend fun initialize(modelPath: String = "classifier.tflite"): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            if (!FeatureFlags.CLASSIFICATION_ENABLED) {
                if (FeatureFlags.ML_DEBUG) {
                    println("🔇 ML Classifier desativado")
                }
                return@runCatching
            }
            
            val startTime = System.currentTimeMillis()
            
            // 👇 Versão mais simples possível
            classifier = BertNLClassifier.createFromFile(context, modelPath)
            isInitialized = true
            
            if (FeatureFlags.ML_DEBUG) {
                println("🤖 ML Classifier inicializado em ${System.currentTimeMillis() - startTime}ms")
            }
        }
    }
    
    // Classifica uma notificação
    suspend fun classify(
        title: String?,
        text: String?
    ): ClassificationResult = withContext(Dispatchers.Default) {
        
        if (!FeatureFlags.CLASSIFICATION_ENABLED || !isInitialized) {
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
            
            if (inputText.isBlank()) {
                return@withContext ClassificationResult(
                    category = Category.OTHER,
                    confidence = 0f,
                    inferenceTimeMs = System.currentTimeMillis() - startTime
                )
            }
            
            val results = classifier?.classify(inputText) ?: emptyList()
            val topResult = results.maxByOrNull { it.getScore() }
            
            val category = when (topResult?.getDisplayName()?.lowercase()) {
                "urgent", "urgente" -> Category.URGENT
                "transaction", "transação" -> Category.TRANSACTION
                "promotion", "promoção" -> Category.PROMOTION
                "social" -> Category.SOCIAL
                else -> Category.OTHER
            }
            
            ClassificationResult(
                category = category,
                confidence = topResult?.getScore() ?: 0f,
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
            
        } catch (e: Exception) {
            ClassificationResult.error(e.message ?: "Erro desconhecido")
        }
    }
    
    fun close() {
        classifier?.close()
        classifier = null
        isInitialized = false
    }
}