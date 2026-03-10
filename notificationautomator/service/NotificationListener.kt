package com.seuapp.notificationautomator.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import kotlinx.coroutines.*

class NotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListener"
    }
    
    private lateinit var repository: NotificationRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(this)
        Log.d(TAG, "📱 NotificationListener criado!")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        val title = extras.getString(android.app.Notification.EXTRA_TITLE, "")
        val text = extras.getString(android.app.Notification.EXTRA_TEXT, "")
        
        // Mostrar nos logs
        Log.d(TAG, "======================================")
        Log.d(TAG, "📱 Notificação recebida!")
        Log.d(TAG, "📦 App: $packageName")
        Log.d(TAG, "📝 Título: $title")
        Log.d(TAG, "💬 Texto: $text")
        
        // Guardar na base de dados
        serviceScope.launch {
            try {
                repository.insertNotification(packageName, title, text)
                Log.d(TAG, "💾 Notificação guardada com sucesso!")
                
                // Verificar quantas notificações temos
                val count = repository.getNotificationCount()
                Log.d(TAG, "📊 Total na BD: $count")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao guardar notificação: ${e.message}")
                e.printStackTrace()
            }
        }
        
        Log.d(TAG, "======================================")
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "🗑️ Notificação removida: ${sbn.packageName}")
    }
    
    override fun onListenerConnected() {
        Log.d(TAG, "======================================")
        Log.d(TAG, "✅✅✅ SERVIÇO DE NOTIFICAÇÃO CONECTADO! ✅✅✅")
        
        // Verificar quantas notificações temos guardadas
        serviceScope.launch {
            try {
                val count = repository.getNotificationCount()
                Log.d(TAG, "📊 Notificações na BD: $count")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao contar notificações: ${e.message}")
            }
        }
        
        Log.d(TAG, "======================================")
    }
    
    override fun onListenerDisconnected() {
        Log.d(TAG, "❌❌❌ SERVIÇO DE NOTIFICAÇÃO DESCONECTADO! ❌❌❌")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "👋 NotificationListener destruído")
    }
}
