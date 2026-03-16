package com.nunomonteiro.notificationwebhooks.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nunomonteiro.notificationwebhooks.data.dao.RuleDao
import com.nunomonteiro.notificationwebhooks.data.database.AppDatabase
import com.nunomonteiro.notificationwebhooks.data.model.ActionType
import com.nunomonteiro.notificationwebhooks.data.model.NotificationStatus
import com.nunomonteiro.notificationwebhooks.data.model.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RuleRepository(context: Context) {
    
    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val ruleDao: RuleDao = database.ruleDao()
    
    // 🆕 Adicionar referência ao NotificationRepository
    private val notificationRepository: NotificationRepository = NotificationRepository(context)
    
    private val TAG = "RuleRepository"
    
    // Flow para observar mudanças
    val allRules: Flow<List<Rule>> = ruleDao.getAllRules()
    val activeRules: Flow<List<Rule>> = ruleDao.getActiveRules()
    val inactiveRules: Flow<List<Rule>> = ruleDao.getInactiveRules()
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    suspend fun insertRule(rule: Rule): Long {
        return ruleDao.insert(rule)
    }
    
    suspend fun updateRule(rule: Rule) {
        ruleDao.update(rule)
    }

    suspend fun getAllRulesSync(): List<Rule> {
        return ruleDao.getAllRulesSync()
    }
    
    suspend fun deleteRule(rule: Rule) {
        ruleDao.delete(rule)
    }
    
    suspend fun getRuleById(id: Long): Rule? {
        return ruleDao.getRuleById(id)
    }
    
    fun toggleRuleActive(rule: Rule) {
        CoroutineScope(Dispatchers.IO).launch {
            ruleDao.update(rule.copy(isActive = !rule.isActive))
        }
    }
    
    suspend fun incrementTriggerCount(id: Long) {
        ruleDao.incrementTriggerCount(id, System.currentTimeMillis())
    }
    
    fun getActiveCount(): Int {
        return runBlocking { ruleDao.getActiveCount() }
    }
    
    fun getTotalCount(): Int {
        return runBlocking { ruleDao.getTotalCount() }
    }
    
    /**
     * Aplica uma regra recém-criada a uma notificação específica.
     * Usa a lógica existente no NotificationRepository para processar a notificação.
     */
    suspend fun applyRuleToNotification(rule: Rule, notificationId: Long) {
        Log.d(TAG, "🚀 applyRuleToNotification: rule=${rule.id}, notification=$notificationId")
        
        try {
            // 1. Buscar a notificação pelo ID
            val notification = notificationRepository.getNotificationById(notificationId)
            
            if (notification == null) {
                Log.e(TAG, "❌ Notificação $notificationId não encontrada")
                return
            }
            
            Log.d(TAG, "📦 Notificação encontrada: ${notification.id} - ${notification.title}")
            
            // 2. Verificar se a notificação já foi processada
            if (notification.status != NotificationStatus.RECEIVED) {
                Log.d(TAG, "⚠️ Notificação já processada (status=${notification.status}). Ignorando.")
                return
            }
            
            // 3. Atualizar a notificação com os dados da regra
            val notificationAtualizada = notification.copy(
                ruleId = rule.id,
                webhookUrl = rule.webhookUrl,
                webhookStatus = null, // Será definido pelo processarNotificacao
                status = NotificationStatus.RECEIVED // Manter RECEIVED para processamento
            )
            
            // 4. Usar o processarNotificacao existente que já tem toda a lógica
            // (incluindo webhook, atualização de status, incremento de contador)
            notificationRepository.processarNotificacao(notificationAtualizada)

            // 👇 NOVO: Forçar atualização da lista de notificações
            // O método antigo carregarNotificacoes foi substituído por carregarPrimeiraPagina
            notificationRepository.carregarPrimeiraPagina(NotificationRepository.FilterType.ALL_VISIBLE)

            Log.d(TAG, "✅ Regra ${rule.id} aplicada à notificação $notificationId")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao aplicar regra à notificação", e)
        }
    }
    
    // Para debugging: criar regras de exemplo
    suspend fun createSampleRules() {
        val sampleRules = listOf(
            Rule(
                name = "Gmail - Faturas",
                appPackage = "com.google.android.gm",
                titleContains = "fatura",
                actionType = ActionType.WEBHOOK_AUTO,
                webhookUrl = "https://webhook.site/example1"
            ),
            Rule(
                name = "LinkedIn - Convites",
                appPackage = "com.linkedin.android",
                textContains = "conectar",
                actionType = ActionType.WEBHOOK_AUTH,
                webhookUrl = "https://webhook.site/example2"
            ),
            Rule(
                name = "WhatsApp - Horário comercial",
                appPackage = "com.whatsapp",
                hourFrom = "09:00",
                hourTo = "18:00",
                daysOfWeek = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                actionType = ActionType.WEBHOOK_AUTO,
                webhookUrl = "https://webhook.site/example3"
            )
        )
        
        sampleRules.forEach { ruleDao.insert(it) }
    }
}