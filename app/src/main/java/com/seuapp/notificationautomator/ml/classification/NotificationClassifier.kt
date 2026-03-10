package com.seuapp.notificationautomator.ml.classification

import android.content.Context
import com.seuapp.notificationautomator.ml.config.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifierOptions
import java.io.File
import java.io.FileOutputStream

/**
 * Classificador de notificações usando TensorFlow Lite
 * 
 * NOTA: Todas as funcionalidades estão protegidas por FeatureFlags
 * Atualmente: DESLIGADO (FeatureFlags.CLASSIFICATION_ENABLED = false)
 */
class NotificationClassifier(private val context: Context) {
    
    // Categorias possíveis para notificações
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
    
    // Resultado da classificação
    data class ClassificationResult(
        val category: Category,
        val confidence: Float,
        val inferenceTimeMs: Long,
        val modelUsed: String,
        val success: Boolean = true,
        val error: String? = null
    ) {
        companion object {
            fun error(errorMessage: String): ClassificationResult {
                return ClassificationResult(
                    category = Category.OTHER,
                    confidence = 0f,
                    inferenceTimeMs = 0,
                    modelUsed = "none",
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
            // Se ML não estiver ativo, não faz nada
            if (!FeatureFlags.CLASSIFICATION_ENABLED) {
                if (FeatureFlags.ML_DEBUG) {
                    println("🔇 ML Classifier desativado (FeatureFlags.CLASSIFICATION_ENABLED = false)")
                }
                return@runCatching
            }
            
            val startTime = System.currentTimeMillis()
            
            // Copiar modelo dos assets para um ficheiro temporário
            val modelFile = copyModelFromAssets(modelPath)
            
            // Configurar opções do classificador
            val options = BertNLClassifierOptions.builder()
                .setMaxSeqLen(128)  // Máximo de tokens a processar
                .build()
            
            // Criar classificador
            classifier = BertNLClassifier.createFromFile(modelFile, options)
            isInitialized = true
            
            val initTime = System.currentTimeMillis() - startTime
            
            if (FeatureFlags.ML_DEBUG) {
                println("🤖 ML Classifier inicializado em ${initTime}ms")
            }
        }
    }
    
    // Classifica uma notificação (título + texto)
    suspend fun classify(
        title: String?,
        text: String?,
        packageName: String? = null
    ): ClassificationResult = withContext(Dispatchers.Default) {
        
        // Se ML não estiver ativo, retorna resultado default
        if (!FeatureFlags.CLASSIFICATION_ENABLED || !isInitialized) {
            return@withContext ClassificationResult(
                category = Category.OTHER,
                confidence = 0f,
                inferenceTimeMs = 0,
                modelUsed = "none",
                success = true
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // Combinar título e texto para classificação
            val inputText = buildString {
                if (!title.isNullOrBlank()) append("$title ")
                if (!text.isNullOrBlank()) append(text)
                if (!packageName.isNullOrBlank() && (title.isNullOrBlank() && text.isNullOrBlank())) {
                    append(packageName)
                }
            }.trim().take(500)  // Limitar tamanho
            
            // Se não houver texto, retorna OTHER
            if (inputText.isBlank()) {
                return@withContext ClassificationResult(
                    category = Category.OTHER,
                    confidence = 0f,
                    inferenceTimeMs = System.currentTimeMillis() - startTime,
                    modelUsed = "classifier.tflite"
                )
            }
            
            // Classificar
            val results = classifier?.classify(inputText) ?: emptyList()
            
            // Encontrar categoria com maior confiança
            val topResult = results.maxByOrNull { it.getScore() }
            
            val category = when (topResult?.getDisplayName()?.lowercase()) {
                "urgent", "urgente", "emergency" -> Category.URGENT
                "transaction", "payment", "transfer", "transação" -> Category.TRANSACTION
                "promotion", "offer", "sale", "promoção", "desconto" -> Category.PROMOTION
                "social", "message", "chat", "comment" -> Category.SOCIAL
                else -> Category.OTHER
            }
            
            val confidence = topResult?.getScore() ?: 0f
            val inferenceTime = System.currentTimeMillis() - startTime
            
            if (FeatureFlags.ML_DEBUG && confidence > 0.5f) {
                println("🤖 Classificado: ${category.value} (${(confidence*100).toInt()}%) em ${inferenceTime}ms")
            }
            
            ClassificationResult(
                category = category,
                confidence = confidence,
                inferenceTimeMs = inferenceTime,
                modelUsed = "classifier.tflite"
            )
            
        } catch (e: Exception) {
            if (FeatureFlags.ML_DEBUG) {
                println("❌ Erro na classificação: ${e.message}")
            }
            ClassificationResult.error(e.message ?: "Erro desconhecido")
        }
    }
    
    // Fecha recursos
    fun close() {
        classifier?.close()
        classifier = null
        isInitialized = false
    }
    
    // Método auxiliar para copiar modelo dos assets
    private fun copyModelFromAssets(modelName: String): File {
        val modelFile = File(context.cacheDir, modelName)
        
        if (!modelFile.exists()) {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        
        return modelFile
    }
}