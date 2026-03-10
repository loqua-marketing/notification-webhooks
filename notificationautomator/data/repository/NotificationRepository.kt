package com.seuapp.notificationautomator.data.repository

import android.content.Context
import android.util.Log
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
                // 1. Verificar regras aplicáveis primeiro
                val regras = ruleDao.getActiveRulesSync()
                
                // 2. Determinar status inicial
                val regraAplicavel = encontrarRegraAplicavel(packageName, title, text, regras)
                val statusInicial = if (regraAplicavel != null) {
                    when (regraAplicavel.actionType) {
                        ActionType.WEBHOOK_AUTO -> NotificationStatus.PROCESSED
                        ActionType.WEBHOOK_AUTH -> NotificationStatus.PENDING_AUTH
                    }
                } else {
                    NotificationStatus.RECEIVED
                }
                
                // 3. Criar notificação com status já definido
                val notification = Notification(
                    packageName = packageName,
                    title = title,
                    text = text,
                    status = statusInicial,
                    ruleId = regraAplicavel?.id,
                    webhookUrl = regraAplicavel?.webhookUrl
                )
                
                // 4. Guardar notificação
                notificationDao.insert(notification)
                
                // 5. Incrementar contador se houver regra
                regraAplicavel?.let {
                    ruleDao.incrementTriggerCount(it.id, System.currentTimeMillis())
                }
                
                // 6. Executar webhook se for automático
                if (regraAplicavel != null && regraAplicavel.actionType == ActionType.WEBHOOK_AUTO) {
                    executarWebhook(regraAplicavel, notification)
                }
                
                // 7. Recarregar lista
                carregarNotificacoes()
                
            } catch (e: Exception) {
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
        return regras.firstOrNull { regra ->
            var matches = true
            
            // Verificar package
            if (regra.appPackage != null) {
                matches = matches && (regra.appPackage == packageName)
            }
            
            // Verificar título
            if (regra.titleContains != null && title != null) {
                matches = matches && title.contains(regra.titleContains, ignoreCase = true)
            }
            
            // Verificar texto
            if (regra.textContains != null && text != null) {
                matches = matches && text.contains(regra.textContains, ignoreCase = true)
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
                
                matches = matches && currentTime in fromTime..toTime
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
                matches = matches && regra.daysOfWeek!!.contains(currentDay)
            }
            
            matches
        }
    }
    
    fun executarWebhook(regra: Rule, notification: Notification) {
        coroutineScope.launch {
            try {
                // Criar dados para o worker
                val data = workDataOf(
                    "notification" to gson.toJson(notification),
                    "rule" to gson.toJson(regra),
                    "webhookUrl" to (regra.webhookUrl ?: ""),
                    "headers" to (regra.webhookHeaders ?: "")
                )
                
                // Configurar requisição com retry
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
                
                // Executar
                workManager.enqueue(request)
                
                Log.d("Webhook", "🌐 Webhook enfileirado para ${regra.name}")
                
            } catch (e: Exception) {
                Log.e("Webhook", "❌ Erro ao enfileirar webhook", e)
            }
        }
    }
    
    suspend fun approveNotification(notification: Notification) {
        try {
            // Buscar a regra associada
            val rule = ruleDao.getRuleById(notification.ruleId ?: return)
            
            // Atualizar status
            notificationDao.update(notification.copy(status = NotificationStatus.PROCESSED))
            
            // Executar webhook
            rule?.let {
                executarWebhook(it, notification)
            }
            
            carregarNotificacoes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getNotificationById(id: Long): Notification? {
        return notificationDao.getNotificationById(id)
    }
    
    suspend fun applyRuleToNotification(rule: Rule, notificationId: Long) {
        try {
            val notification = notificationDao.getNotificationById(notificationId) ?: return
            
            val novoStatus = when (rule.actionType) {
                ActionType.WEBHOOK_AUTO -> NotificationStatus.PROCESSED
                ActionType.WEBHOOK_AUTH -> NotificationStatus.PENDING_AUTH
            }
            
            notificationDao.update(notification.copy(
                status = novoStatus,
                ruleId = rule.id,
                webhookUrl = rule.webhookUrl
            ))
            
            ruleDao.incrementTriggerCount(rule.id, System.currentTimeMillis())
            
            if (rule.actionType == ActionType.WEBHOOK_AUTO) {
                executarWebhook(rule, notification)
            }
            
            carregarNotificacoes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun updateNotificationStatus(id: Long, status: NotificationStatus) {
        coroutineScope.launch {
            try {
                val notifications = notificationDao.getAllNotifications()
                val notification = notifications.find { it.id == id }
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