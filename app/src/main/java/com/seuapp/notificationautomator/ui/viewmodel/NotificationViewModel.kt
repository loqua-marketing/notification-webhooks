package com.seuapp.notificationautomator.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.WebhookStatus
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import com.seuapp.notificationautomator.data.repository.RuleRepository
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: NotificationRepository = NotificationRepository(application)
    private val ruleRepository: RuleRepository = RuleRepository(application)
    
    val allNotifications: LiveData<List<Notification>> = repository.allNotifications
    
    private val _filteredNotifications = MutableLiveData<List<Notification>>()
    val filteredNotifications: LiveData<List<Notification>> = _filteredNotifications
    
    private val _currentFilter = MutableLiveData<FilterType>(FilterType.ALL)
    val currentFilter: LiveData<FilterType> = _currentFilter
    
    // Contadores
    private val _countAll = MutableLiveData(0)
    val countAll: LiveData<Int> = _countAll
    
    private val _countProcessed = MutableLiveData(0)
    val countProcessed: LiveData<Int> = _countProcessed
    
    private val _countPending = MutableLiveData(0)
    val countPending: LiveData<Int> = _countPending
    
    private val _countReceived = MutableLiveData(0)
    val countReceived: LiveData<Int> = _countReceived
    
    private val _countRejected = MutableLiveData(0)
    val countRejected: LiveData<Int> = _countRejected
    
    private val _countError = MutableLiveData(0)
    val countError: LiveData<Int> = _countError
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    enum class FilterType {
        ALL, PROCESSED, PENDING, RECEIVED, REJECTED, ERROR
    }
    
    init {
        repository.allNotifications.observeForever { notifications ->
            atualizarContadores(notifications)
            aplicarFiltro(_currentFilter.value ?: FilterType.ALL, notifications)
        }
    }
    
    private fun atualizarContadores(notifications: List<Notification>) {
        _countAll.value = notifications.size
        _countProcessed.value = notifications.count { 
            it.status == NotificationStatus.PROCESSED || 
            it.status == NotificationStatus.APPROVED 
        }
        _countPending.value = notifications.count { it.status == NotificationStatus.PENDING_AUTH }
        _countReceived.value = notifications.count { it.status == NotificationStatus.RECEIVED }
        _countRejected.value = notifications.count { it.status == NotificationStatus.REJECTED }
        _countError.value = notifications.count { 
            it.status == NotificationStatus.ERROR || 
            it.webhookStatus == WebhookStatus.FAILED 
        }
    }
    
    fun aplicarFiltro(filter: FilterType) {
        _currentFilter.value = filter
        allNotifications.value?.let { notifications ->
            aplicarFiltro(filter, notifications)
        }
    }
    
    private fun aplicarFiltro(filter: FilterType, notifications: List<Notification>) {
        _filteredNotifications.value = when (filter) {
            FilterType.ALL -> notifications
            FilterType.PROCESSED -> notifications.filter { 
                it.status == NotificationStatus.PROCESSED || 
                it.status == NotificationStatus.APPROVED 
            }
            FilterType.PENDING -> notifications.filter { it.status == NotificationStatus.PENDING_AUTH }
            FilterType.RECEIVED -> notifications.filter { it.status == NotificationStatus.RECEIVED }
            FilterType.REJECTED -> notifications.filter { it.status == NotificationStatus.REJECTED }
            FilterType.ERROR -> notifications.filter { 
                it.status == NotificationStatus.ERROR || 
                it.webhookStatus == WebhookStatus.FAILED 
            }
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
    
    fun approveNotification(notification: Notification) {
        viewModelScope.launch {
            repository.updateNotificationStatus(notification.id, NotificationStatus.APPROVED)
            repository.executarWebhookComNotificacao(notification)
        }
    }
    
    fun rejectNotification(notification: Notification) {
        viewModelScope.launch {
            repository.updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
        }
    }
    
    fun processNotification(notification: Notification) {
        viewModelScope.launch {
            repository.processarNotificacao(notification)
        }
    }
    
    fun ignoreNotification(notification: Notification) {
        viewModelScope.launch {
            repository.updateNotificationStatus(notification.id, NotificationStatus.REJECTED)
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
            NotificationStatus.PROCESSED -> "Automática"
            NotificationStatus.PENDING_AUTH -> "A Aguardar"
            NotificationStatus.APPROVED -> "Aprovada"
            NotificationStatus.REJECTED -> "Rejeitada"
            NotificationStatus.ERROR -> "Erro"
        }
    }
    
    fun getStatusIcon(status: NotificationStatus): String {
        return when (status) {
            NotificationStatus.RECEIVED -> "📥"
            NotificationStatus.PROCESSED -> "✅"
            NotificationStatus.PENDING_AUTH -> "⏳"
            NotificationStatus.APPROVED -> "✓"
            NotificationStatus.REJECTED -> "✗"
            NotificationStatus.ERROR -> "❌"
        }
    }
    
    fun getStatusColor(status: NotificationStatus): Int {
        return when (status) {
            NotificationStatus.RECEIVED -> android.graphics.Color.parseColor("#2196F3") // Azul
            NotificationStatus.PROCESSED -> android.graphics.Color.parseColor("#4CAF50") // Verde
            NotificationStatus.PENDING_AUTH -> android.graphics.Color.parseColor("#FF9800") // Laranja
            NotificationStatus.APPROVED -> android.graphics.Color.parseColor("#8BC34A") // Verde claro
            NotificationStatus.REJECTED -> android.graphics.Color.parseColor("#F44336") // Vermelho
            NotificationStatus.ERROR -> android.graphics.Color.parseColor("#9C27B0") // Roxo
        }
    }
    
    fun getWebhookStatusText(webhookStatus: WebhookStatus?): String {
        return when (webhookStatus) {
            WebhookStatus.PENDING -> "A enviar"
            WebhookStatus.SUCCESS -> "Enviado"
            WebhookStatus.FAILED -> "Falhou"
            null -> "Não enviado"
        }
    }
}
