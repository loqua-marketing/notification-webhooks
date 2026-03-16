package com.nunomonteiro.notificationwebhooks.service

import com.nunomonteiro.notificationwebhooks.data.dao.NotificationDao
import android.content.Context
import android.util.Log
import com.nunomonteiro.notificationwebhooks.ml.classification.NotificationClassifier
import com.nunomonteiro.notificationwebhooks.ml.config.FeatureFlags
import kotlinx.coroutines.*

class MLService(private val context: Context) {
    
    private val TAG = "MLService"
    private val classifier = NotificationClassifier(context)
    private var isInitialized = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun initialize(notificationDao: NotificationDao? = null) {
        // Verifica se ML está ativo
        if (!FeatureFlags.ML_ENABLED) {
            return
        }
        
        // Inicializa em background
        withContext(Dispatchers.IO) {
            try {
                classifier.initialize()
                isInitialized = true
                if (FeatureFlags.ML_DEBUG) {
                    Log.d(TAG, "🤖 ML Service inicializado")
                }
                
                // Reprocessar notificações se tivermos o DAO
                notificationDao?.let {
                    reprocessRecentNotifications(it)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao inicializar ML: ${e.message}")
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
        if (!FeatureFlags.CLASSIFICATION_ENABLED) {
            Log.d(TAG, "⚠️ CLASSIFICATION_ENABLED = false")
            return
        }
        if (!isInitialized) {
            Log.d(TAG, "⚠️ ML não inicializado")
            return
        }
        
        Log.d(TAG, "🚀 A classificar notificação $id em background")
        
        coroutineScope.launch {
            try {
                val result = classifier.classify(title, text)
                
                if (result.success && result.confidence > 0.3f) {
                    withContext(Dispatchers.Main) {
                        onResult(result.category.value, result.confidence)
                    }
                    // 🔥 LOG ATIVADO
                    if (FeatureFlags.ML_DEBUG) {
                        Log.d(TAG, "🤖 Notificação $id: ${result.category.value} (${result.confidence})")
                    }
                } else {
                    Log.d(TAG, "⚠️ Confiança baixa ou erro: ${result.confidence}")
                }
            } catch (e: Exception) {
                // 🔥 LOG ATIVADO
                if (FeatureFlags.ML_DEBUG) {
                    Log.e(TAG, "❌ Erro: ${e.message}")
                }
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
    // Adicionar no final da classe MLService.kt

    /**
     * Reprocessa notificações recentes que não têm categoria ML
     */
    fun reprocessRecentNotifications(notificationDao: NotificationDao) {
        if (!FeatureFlags.CLASSIFICATION_ENABLED || !isInitialized) return
        
        Log.d(TAG, "🔄 A reprocessar notificações recentes sem ML...")
        
        coroutineScope.launch {
            try {
                // Buscar notificações sem categoria dos últimos 5 minutos
                val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
                val notifications = notificationDao.getUnclassifiedRecentNotifications(fiveMinutesAgo)
                
                Log.d(TAG, "📊 Encontradas ${notifications.size} notificações para reprocessar")
                
                notifications.forEach { notification ->
                    classifyNotificationAsync(
                        notification.id,
                        notification.title,
                        notification.text
                    ) { category, confidence ->
                        // Atualizar a notificação (já é feito no classifyNotificationAsync)
                        Log.d(TAG, "🔄 Reprocessada notificação ${notification.id}: $category ($confidence)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao reprocessar notificações", e)
            }
        }
    }
}