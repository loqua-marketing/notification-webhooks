package com.seuapp.notificationautomator.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import com.seuapp.notificationautomator.data.repository.RuleRepository
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: NotificationRepository = NotificationRepository(application)
    private val ruleRepository: RuleRepository = RuleRepository(application)
    
    // LiveData para todas as notificações
    val allNotifications: LiveData<List<Notification>> = repository.allNotifications
    
    // LiveData para notificações filtradas
    private val _filteredNotifications = MutableLiveData<List<Notification>>()
    val filteredNotifications: LiveData<List<Notification>> = _filteredNotifications
    
    // Filtro atual
    private val _currentFilter = MutableLiveData<FilterType>(FilterType.ALL)
    val currentFilter: LiveData<FilterType> = _currentFilter
    
    // Contadores
    private val _countAll = MutableLiveData(0)
    val countAll: LiveData<Int> = _countAll
    
    private val _countWebhookAuto = MutableLiveData(0)
    val countWebhookAuto: LiveData<Int> = _countWebhookAuto
    
    private val _countWebhookAuth = MutableLiveData(0)
    val countWebhookAuth: LiveData<Int> = _countWebhookAuth
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    enum class FilterType {
        ALL, WEBHOOK_AUTO, WEBHOOK_AUTH
    }
    
    init {
        // Observar mudanças nas notificações
        repository.allNotifications.observeForever { notifications ->
            atualizarContadores(notifications)
            aplicarFiltro(_currentFilter.value ?: FilterType.ALL, notifications)
        }
    }
    
    private fun atualizarContadores(notifications: List<Notification>) {
        _countAll.value = notifications.size
        
        // Estes valores serão atualizados quando tivermos as regras a funcionar
        // Por agora, deixamos a 0
        _countWebhookAuto.value = 0
        _countWebhookAuth.value = 0
    }
    
    fun aplicarFiltro(filter: FilterType) {
        _currentFilter.value = filter
        allNotifications.value?.let { notifications ->
            aplicarFiltro(filter, notifications)
        }
    }
    
    private fun aplicarFiltro(filter: FilterType, notifications: List<Notification>) {
        // Por agora, como não temos regras, todos os filtros mostram a mesma lista
        // Quando tivermos o motor de regras, aqui vamos filtrar com base nas regras
        _filteredNotifications.value = when (filter) {
            FilterType.ALL -> notifications
            FilterType.WEBHOOK_AUTO -> notifications // Temporário - depois filtra
            FilterType.WEBHOOK_AUTH -> notifications // Temporário - depois filtra
        }
    }
    
    fun refreshNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.carregarNotificacoes()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearAllNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearAllNotifications()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao limpar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getStatusText(status: NotificationStatus): String {
        return when (status) {
            NotificationStatus.RECEIVED -> "Recebida"
            NotificationStatus.PROCESSED -> "Processada"
            NotificationStatus.PENDING_AUTH -> "A Aguardar"
            NotificationStatus.IGNORED -> "Ignorada"
        }
    }
    
    fun getStatusColor(status: NotificationStatus): Int {
        return when (status) {
            NotificationStatus.RECEIVED -> android.graphics.Color.parseColor("#2196F3")
            NotificationStatus.PROCESSED -> android.graphics.Color.parseColor("#4CAF50")
            NotificationStatus.PENDING_AUTH -> android.graphics.Color.parseColor("#FF9800")
            NotificationStatus.IGNORED -> android.graphics.Color.parseColor("#9E9E9E")
        }
    }
}