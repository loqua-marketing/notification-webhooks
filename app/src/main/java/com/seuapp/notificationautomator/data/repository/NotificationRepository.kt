package com.seuapp.notificationautomator.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.google.gson.Gson
import com.seuapp.notificationautomator.data.dao.NotificationDao
import com.seuapp.notificationautomator.data.dao.RuleDao
import com.seuapp.notificationautomator.data.database.AppDatabase
import com.seuapp.notificationautomator.data.model.ActionType
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.data.model.WebhookStatus
import com.seuapp.notificationautomator.worker.WebhookWorker
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationRepository(context: Context) {
    
    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val notificationDao: NotificationDao = database.notificationDao()
    private val ruleDao: RuleDao = database.ruleDao()
    private val workManager = WorkManager.getInstance(context)
    private val gson = Gson()
    private val appContext = context.applicationContext
    private val TAG = "NotificationRepo"
    
    private val _allNotifications = MutableLiveData<List<Notification>>()
    val allNotifications: LiveData<List<Notification>> = _allNotifications
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        carregarNotificacoes()
    }
    
    fun carregarNotificacoes() {
        coroutineScope.launch {
            try {
                val notifications = notificationDao.getAllNotifications()
                _allNotifications.postValue(notifications)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun insertNotification(
        packageName: String,
        title: String?,
        text: String?
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "==========================================")
                Log.d(TAG, "📥 NOVA NOTIFICAÇÃO")
                Log.d(TAG, "📦 Package: $packageName")
                Log.d(TAG, "📝 Título: $title")
                Log.d(TAG, "💬 Texto: $text")
                
                val regras = ruleDao.getActiveRulesSync()
                Log.d(TAG, "📋 Regras ativas: ${regras.size}")
                
                // Listar todas as regras para debug
                regras.forEachIndexed { index, rule ->
                    Log.d(TAG, "   Regra $index: ${rule.name} - Package: ${rule.appPackage ?: "QUALQUER APP"}")
                }
                
                val regraAplicavel = encontrarRegraAplicavel(packageName, title, text, regras)
                
                val statusInicial = if (regraAplicavel != null) {
                    Log.d(TAG, "✅ REGRA ENCONTRADA: ${regraAplicavel.name}")
                    when (regraAplicavel.actionType) {
                        ActionType.WEBHOOK_AUTO -> {
                            Log.d(TAG, "⚡ Ação: Webhook Automático")
                            NotificationStatus.PROCESSED
                        }
                        ActionType.WEBHOOK_AUTH -> {
                            Log.d(TAG, "🔐 Ação: Webhook com Autorização")
                            NotificationStatus.PENDING_AUTH
                        }
                    }
                } else {
                    Log.d(TAG, "❌ Nenhuma regra encontrada")
                    NotificationStatus.RECEIVED
                }
                
                val notification = Notification(
                    packageName = packageName,
                    title = title,
                    text = text,
                    status = statusInicial,
                    ruleId = regraAplicavel?.id,
                    webhookUrl = regraAplicavel?.webhookUrl,
                    webhookStatus = if (regraAplicavel != null) WebhookStatus.PENDING else null
                )
                
                val id = notificationDao.insert(notification)
                Log.d(TAG, "💾 Notificação guardada com ID: $id, Status: $statusInicial")
                
                regraAplicavel?.let {
                    ruleDao.incrementTriggerCount(it.id, System.currentTimeMillis())
                }
                
                if (regraAplicavel != null && regraAplicavel.actionType == ActionType.WEBHOOK_AUTO) {
                    Log.d(TAG, "🚀 A executar webhook automático...")
                    executarWebhook(regraAplicavel, notification.copy(id = id))
                }
                
                carregarNotificacoes()
                Log.d(TAG, "==========================================")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao processar notificação", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun encontrarRegraAplicavel(
        packageName: String,
        title: String?,
        text: String?,
        regras: List<Rule>
    ): Rule? {
        Log.d(TAG, "🔍 Procurando regras para:")
        Log.d(TAG, "   Package: $packageName")
        Log.d(TAG, "   Título: $title")
        Log.d(TAG, "   Texto: $text")
        
        return regras.firstOrNull { regra ->
            var matches = true
            val matchDetails = mutableListOf<String>()
            
            // Verificar package
            if (regra.appPackage != null) {
                val packageMatch = regra.appPackage == packageName
                matches = matches && packageMatch
                matchDetails.add("Package: ${regra.appPackage} == $packageName? $packageMatch")
            } else {
                matchDetails.add("Package: QUALQUER APP")
            }
            
            // Verificar título
            if (regra.titleContains != null && title != null) {
                val titleMatch = title.contains(regra.titleContains, ignoreCase = true)
                matches = matches && titleMatch
                matchDetails.add("Título contém '${regra.titleContains}'? $titleMatch")
            } else if (regra.titleContains != null && title == null) {
                matches = false
                matchDetails.add("Título contém '${regra.titleContains}'? false (título nulo)")
            }
            
            // Verificar texto
            if (regra.textContains != null && text != null) {
                val textMatch = text.contains(regra.textContains, ignoreCase = true)
                matches = matches && textMatch
                matchDetails.add("Texto contém '${regra.textContains}'? $textMatch")
            } else if (regra.textContains != null && text == null) {
                matches = false
                matchDetails.add("Texto contém '${regra.textContains}'? false (texto nulo)")
            }
            
            // Verificar horário
            if (regra.hourFrom != null && regra.hourTo != null) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentTime = currentHour * 60 + currentMinute
                
                val fromParts = regra.hourFrom!!.split(":")
                val toParts = regra.hourTo!!.split(":")
                
                val fromTime = fromParts[0].toInt() * 60 + fromParts[1].toInt()
                val toTime = toParts[0].toInt() * 60 + toParts[1].toInt()
                
                val timeMatch = currentTime in fromTime..toTime
                matches = matches && timeMatch
                matchDetails.add("Horário $currentTime entre $fromTime-$toTime? $timeMatch")
            }
            
            // Verificar dias da semana
            if (regra.daysOfWeek != null) {
                val calendar = Calendar.getInstance()
                val currentDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "MONDAY"
                    Calendar.TUESDAY -> "TUESDAY"
                    Calendar.WEDNESDAY -> "WEDNESDAY"
                    Calendar.THURSDAY -> "THURSDAY"
                    Calendar.FRIDAY -> "FRIDAY"
                    Calendar.SATURDAY -> "SATURDAY"
                    Calendar.SUNDAY -> "SUNDAY"
                    else -> ""
                }
                val dayMatch = regra.daysOfWeek!!.contains(currentDay)
                matches = matches && dayMatch
                matchDetails.add("Dia $currentDay em [${regra.daysOfWeek}]? $dayMatch")
            }
            
            Log.d(TAG, "   Regra '${regra.name}': ${if (matches) "✅" else "❌"} - ${matchDetails.joinToString(", ")}")
            
            matches
        }
    }
    
    fun executarWebhook(regra: Rule, notification: Notification) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "🌐 Preparando webhook para ${regra.name}")
                
                val data = workDataOf(
                    "notification" to gson.toJson(notification),
                    "rule" to gson.toJson(regra),
                    "webhookUrl" to (regra.webhookUrl ?: ""),
                    "headers" to (regra.webhookHeaders ?: ""),
                    "notificationId" to notification.id
                )
                
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                
                val request = OneTimeWorkRequestBuilder<WebhookWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10, TimeUnit.SECONDS
                    )
                    .build()
                
                workManager.enqueue(request)
                
                Log.d(TAG, "🌐 Webhook enfileirado para ${regra.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao enfileirar webhook", e)
            }
        }
    }
    
    suspend fun approveNotification(notification: Notification) {
        try {
            Log.d(TAG, "✅ Aprovando notificação ${notification.id}")
            val rule = ruleDao.getRuleById(notification.ruleId ?: return)
            notificationDao.update(notification.copy(status = NotificationStatus.APPROVED))
            rule?.let {
                executarWebhook(it, notification)
            }
            carregarNotificacoes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun rejectNotification(notification: Notification) {
        try {
            Log.d(TAG, "❌ Rejeitando notificação ${notification.id}")
            notificationDao.update(notification.copy(status = NotificationStatus.REJECTED))
            carregarNotificacoes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getNotificationById(id: Long): Notification? {
        return notificationDao.getNotificationById(id)
    }
    
    suspend fun executarWebhookComNotificacao(notification: Notification) {
        try {
            val rule = ruleDao.getRuleById(notification.ruleId ?: return)
            rule?.let {
                executarWebhook(it, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun processarNotificacao(notification: Notification) {
        try {
            Log.d(TAG, "🔄 Processando notificação ${notification.id} manualmente")
            
            val regras = ruleDao.getActiveRulesSync()
            Log.d(TAG, "📋 Regras ativas: ${regras.size}")
            
            val regraAplicavel = encontrarRegraAplicavel(
                notification.packageName,
                notification.title,
                notification.text,
                regras
            )
            
            if (regraAplicavel != null) {
                Log.d(TAG, "✅ Regra encontrada: ${regraAplicavel.name}")
                
                val novoStatus = when (regraAplicavel.actionType) {
                    ActionType.WEBHOOK_AUTO -> {
                        Log.d(TAG, "⚡ Ação: Webhook Automático")
                        NotificationStatus.PROCESSED
                    }
                    ActionType.WEBHOOK_AUTH -> {
                        Log.d(TAG, "🔐 Ação: Webhook com Autorização")
                        NotificationStatus.PENDING_AUTH
                    }
                }
                
                notificationDao.update(notification.copy(
                    status = novoStatus,
                    ruleId = regraAplicavel.id,
                    webhookUrl = regraAplicavel.webhookUrl,
                    webhookStatus = WebhookStatus.PENDING
                ))
                
                ruleDao.incrementTriggerCount(regraAplicavel.id, System.currentTimeMillis())
                
                if (regraAplicavel.actionType == ActionType.WEBHOOK_AUTO) {
                    Log.d(TAG, "🚀 A executar webhook...")
                    executarWebhook(regraAplicavel, notification)
                }
                
                Toast.makeText(appContext, "Regra aplicada com sucesso!", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "❌ Nenhuma regra encontrada para esta notificação")
                Toast.makeText(appContext, "Nenhuma regra encontrada", Toast.LENGTH_SHORT).show()
            }
            
            carregarNotificacoes()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar notificação", e)
            e.printStackTrace()
        }
    }
    

    suspend fun getRecentNotificationCount(
        packageName: String,
        title: String?,
        text: String?,
        timeWindowMs: Long
    ): Int {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        return notificationDao.getRecentNotificationCount(packageName, title, text, cutoffTime)
    }


    suspend fun atualizarWebhookStatus(
        id: Long,
        webhookStatus: WebhookStatus,
        response: String? = null,
        error: String? = null
    ) {
        try {
            val notification = notificationDao.getNotificationById(id)
            notification?.let {
                notificationDao.update(it.copy(
                    webhookStatus = webhookStatus,
                    webhookResponse = response,
                    webhookError = error
                ))
                if (webhookStatus == WebhookStatus.FAILED) {
                    notificationDao.update(it.copy(status = NotificationStatus.ERROR))
                }
                carregarNotificacoes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun updateNotificationStatus(id: Long, status: NotificationStatus) {
        coroutineScope.launch {
            try {
                val notification = notificationDao.getNotificationById(id)
                if (notification != null) {
                    notificationDao.update(notification.copy(status = status))
                    carregarNotificacoes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun getNotificationCount(): Int {
        return runBlocking {
            try {
                notificationDao.getCount()
            } catch (e: Exception) {
                0
            }
        }
    }
    
    fun deleteOldNotifications(daysToKeep: Int = 7) {
        coroutineScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000)
                notificationDao.deleteOldNotifications(cutoffTime)
                carregarNotificacoes()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun clearAllNotifications() {
        coroutineScope.launch {
            try {
                val notifications = notificationDao.getAllNotifications()
                notifications.forEach { notificationDao.delete(it) }
                carregarNotificacoes()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
