package com.seuapp.notificationautomator.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.seuapp.notificationautomator.data.model.ActionType
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.data.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RuleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: RuleRepository = RuleRepository(application)
    
    // Flows da base de dados
    val allRules: Flow<List<Rule>> = repository.allRules
    val activeRules: Flow<List<Rule>> = repository.activeRules
    val inactiveRules: Flow<List<Rule>> = repository.inactiveRules
    
    // LiveData para a UI
    private val _rules = MutableLiveData<List<Rule>>()
    val rules: LiveData<List<Rule>> = _rules
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Tab atual
    private val _currentTab = MutableLiveData(0) // 0 = Ativas, 1 = Inativas
    val currentTab: LiveData<Int> = _currentTab
    
    // Texto de pesquisa
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    
    // Todas as regras (para filtragem)
    private var allRulesList: List<Rule> = emptyList()
    
    init {
        // Observar todas as regras
        viewModelScope.launch {
            repository.allRules.collect { rules ->
                allRulesList = rules
                aplicarFiltro()
            }
        }
    }
    
    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
        aplicarFiltro()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        aplicarFiltro()
    }
    
    private fun aplicarFiltro() {
        val tab = _currentTab.value ?: 0
        val query = _searchQuery.value?.lowercase() ?: ""
        
        val filtradas = allRulesList.filter { rule ->
            // Filtrar por tab
            val tabMatch = when (tab) {
                0 -> rule.isActive
                1 -> !rule.isActive
                else -> true
            }
            
            // Filtrar por pesquisa
            val searchMatch = query.isEmpty() || 
                rule.name.lowercase().contains(query) ||
                (rule.appPackage?.lowercase()?.contains(query) == true) ||
                (rule.titleContains?.lowercase()?.contains(query) == true)
            
            tabMatch && searchMatch
        }
        
        _rules.value = filtradas
    }
    
    fun toggleRuleActive(rule: Rule) {
        viewModelScope.launch {
            repository.toggleRuleActive(rule)
        }
    }
    
    fun deleteRule(rule: Rule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }
    
    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }
    
    fun getActionDescription(rule: Rule): String {
        return when (rule.actionType) {
            ActionType.WEBHOOK_AUTO -> "🌐 Webhook automático"
            ActionType.WEBHOOK_AUTH -> "🔐 Webhook com autorização"
        }
    }
    
    fun getConditionsDescription(rule: Rule): String {
        val conditions = mutableListOf<String>()
        
        rule.appPackage?.let {
            val appName = it.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            conditions.add("App: $appName")
        }
        
        rule.titleContains?.let {
            conditions.add("Título contém \"$it\"")
        }
        
        rule.textContains?.let {
            conditions.add("Texto contém \"$it\"")
        }
        
        if (rule.hourFrom != null && rule.hourTo != null) {
            conditions.add("Horário: ${rule.hourFrom}-${rule.hourTo}")
        }
        
        rule.daysOfWeek?.let {
            val days = it.split(",").map { day ->
                when (day) {
                    "MONDAY" -> "Seg"
                    "TUESDAY" -> "Ter"
                    "WEDNESDAY" -> "Qua"
                    "THURSDAY" -> "Qui"
                    "FRIDAY" -> "Sex"
                    "SATURDAY" -> "Sáb"
                    "SUNDAY" -> "Dom"
                    else -> day
                }
            }.joinToString(" ")
            conditions.add("Dias: $days")
        }
        
        rule.isSilent?.let {
            conditions.add(if (it) "🔇 Silenciosa" else "🔔 Alertante")
        }
        
        rule.hasImage?.let {
            conditions.add(if (it) "🖼️ Com imagem" else "🚫 Sem imagem")
        }
        
        return if (conditions.isEmpty()) "Sem condições" else conditions.joinToString(" · ")
    }
    
    fun getStatsDescription(rule: Rule): String {
        val count = rule.triggerCount
        val last = rule.lastTriggered?.let {
            java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "nunca"
        
        return "⚡ $count vezes · Última: $last"
    }
    
    // Para debugging: criar regras de exemplo
    fun createSampleRules() {
        viewModelScope.launch {
            repository.createSampleRules()
        }
    }
}