// app/src/main/java/com/seuapp/notificationautomator/data/repository/RuleRepository.kt

package com.seuapp.notificationautomator.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seuapp.notificationautomator.data.dao.RuleDao
import com.seuapp.notificationautomator.data.database.AppDatabase
import com.seuapp.notificationautomator.data.model.ActionType
import com.seuapp.notificationautomator.data.model.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RuleRepository(context: Context) {
    
    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val ruleDao: RuleDao = database.ruleDao()
    
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