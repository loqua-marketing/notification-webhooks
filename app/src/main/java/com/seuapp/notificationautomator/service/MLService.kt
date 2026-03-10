package com.seuapp.notificationautomator.service

import android.content.Context
import android.util.Log
import com.seuapp.notificationautomator.ml.classification.NotificationClassifier
import com.seuapp.notificationautomator.ml.config.FeatureFlags
import kotlinx.coroutines.*

class MLService(private val context: Context) {
    
    private val TAG = "MLService"
    private val classifier = NotificationClassifier(context)
    private var isInitialized = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun initialize() {
        // Verifica se ML está ativo
        if (!FeatureFlags.ML_ENABLED) {
            // Debug comentado temporariamente
            // if (FeatureFlags.ML_DEBUG) {
            //     Log.d(TAG, "🔇 ML Service desativado")
            // }
            return
        }
        
        // Inicializa em background
        withContext(Dispatchers.IO) {
            try {
                classifier.initialize()
                isInitialized = true
                // Debug comentado
                // if (FeatureFlags.ML_DEBUG) {
                //     Log.d(TAG, "🤖 ML Service inicializado")
                // }
            } catch (e: Exception) {
                // Log.e(TAG, "❌ Erro ao inicializar ML: ${e.message}")
            }
        }
    }
    
    fun classifyNotificationAsync(
        id: Long,
        title: String?,
        text: String?,
        onResult: (String?, Float?) -> Unit
    ) {
        // Early returns
        if (!FeatureFlags.CLASSIFICATION_ENABLED) return
        if (!isInitialized) return
        
        coroutineScope.launch {
            try {
                val result = classifier.classify(title, text)
                
                if (result.success && result.confidence > 0.3f) {
                    withContext(Dispatchers.Main) {
                        onResult(result.category.value, result.confidence)
                    }
                    // Debug comentado
                    // if (FeatureFlags.ML_DEBUG) {
                    //     Log.d(TAG, "🤖 Notificação $id: ${result.category.value} (${result.confidence})")
                    // }
                }
            } catch (e: Exception) {
                // Debug comentado
                // if (FeatureFlags.ML_DEBUG) {
                //     Log.e(TAG, "❌ Erro: ${e.message}")
                // }
            }
        }
    }
    
    suspend fun classifyNotificationSync(
        title: String?,
        text: String?
    ): NotificationClassifier.ClassificationResult {
        
        if (!FeatureFlags.CLASSIFICATION_ENABLED) {
            return NotificationClassifier.ClassificationResult.error("ML desativado")
        }
        if (!isInitialized) {
            return NotificationClassifier.ClassificationResult.error("ML não inicializado")
        }
        return classifier.classify(title, text)
    }
    
    fun close() {
        classifier.close()
        coroutineScope.cancel()
    }
}