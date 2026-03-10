package com.seuapp.notificationautomator.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.os.Bundle
import android.app.Notification
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import kotlinx.coroutines.*

class NotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListener"
        private const val DEBUG_MODE = false
    }
    
    private lateinit var repository: NotificationRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(this)
        Log.d(TAG, "📱 NotificationListener criado!")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val originalPackage = sbn.packageName
        val extras = sbn.notification.extras
        val notificationId = sbn.id
        
        val title = extractTitle(extras)
        val text = extractText(extras)
        
        // Processar tudo dentro da corrotina
        serviceScope.launch {
            processNotification(originalPackage, title, text, notificationId, sbn)
        }
    }
    
    private suspend fun processNotification(
        packageName: String,
        title: String?,
        text: String?,
        notificationId: Int,
        sbn: StatusBarNotification
    ) {
        // SOLUÇÃO DA COMUNIDADE: Ignorar notificações de resumo de grupo
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(TAG, "📦 Notificação de GRUPO ignorada (FLAG_GROUP_SUMMARY)")
            return
        }
        
        // Para Gmail, verificar duplicatas individuais
        if (packageName == "com.google.android.gm") {
            // Verificar se já existe notificação com mesmo título/texto nos últimos 5 segundos
            val recentCount = repository.getRecentNotificationCount(packageName, title, text, 5000)
            if (recentCount > 0) {
                Log.d(TAG, "⚠️ Duplicata ignorada (já existe notificação recente)")
                return
            }
        }
        
        Log.d(TAG, "======================================")
        Log.d(TAG, "📱 Notificação recebida de: $packageName")
        Log.d(TAG, "🆔 ID: $notificationId")
        Log.d(TAG, "📝 Título: $title")
        Log.d(TAG, "💬 Texto: $text")
        Log.d(TAG, "🚩 Flags: ${sbn.notification.flags}")
        
        try {
            repository.insertNotification(packageName, title, text)
            Log.d(TAG, "💾 Notificação guardada")
            
            val count = repository.getNotificationCount()
            Log.d(TAG, "📊 Total na BD: $count")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro: ${e.message}")
        }
        
        Log.d(TAG, "======================================")
    }
    
    private fun extractTitle(extras: Bundle): String? {
        extras.getString(Notification.EXTRA_TITLE)?.let {
            if (it.isNotBlank()) return it
        }
        extras.getString("android.title")?.let {
            if (it.isNotBlank()) return it
        }
        return null
    }
    
    private fun extractText(extras: Bundle): String? {
        // 1. Tentar lines (InboxStyle - múltiplas mensagens)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { lines ->
            if (lines.isNotEmpty()) {
                val text = lines.joinToString("\n")
                Log.d(TAG, "📝 Usando EXTRA_TEXT_LINES: $text")
                return text
            }
        }
        
        // 2. Tentar big text (BigTextStyle - mensagem longa)
        extras.getString(Notification.EXTRA_BIG_TEXT)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "📝 Usando EXTRA_BIG_TEXT: $it")
                return it
            }
        }
        
        // 3. Tentar texto normal
        extras.getString(Notification.EXTRA_TEXT)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "📝 Usando EXTRA_TEXT: $it")
                return it
            }
        }
        
        // 4. Tentar sub text (remetente)
        extras.getString(Notification.EXTRA_SUB_TEXT)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "📝 Usando EXTRA_SUB_TEXT: $it")
                return it
            }
        }
        
        // 5. Último recurso: usar o título
        extras.getString(Notification.EXTRA_TITLE)?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "📝 Usando TITLE como fallback: $it")
                return it
            }
        }
        
        return null
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "🗑️ Notificação removida: ${sbn.packageName}")
    }
    
    override fun onListenerConnected() {
        Log.d(TAG, "======================================")
        Log.d(TAG, "✅✅✅ CONECTADO! ✅✅✅")
        
        serviceScope.launch {
            try {
                val count = repository.getNotificationCount()
                Log.d(TAG, "📊 Total na BD: $count")
            } catch (e: Exception) {
                Log.e(TAG, "Erro: ${e.message}")
            }
        }
        
        Log.d(TAG, "======================================")
    }
    
    override fun onListenerDisconnected() {
        Log.d(TAG, "❌❌❌ DESCONECTADO! ❌❌❌")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "👋 Destroy")
    }
}
