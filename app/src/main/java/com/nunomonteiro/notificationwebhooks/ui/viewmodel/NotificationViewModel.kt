package com.nunomonteiro.notificationwebhooks.ui.viewmodel

import kotlinx.coroutines.delay
import android.util.Log
import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nunomonteiro.notificationwebhooks.data.model.Notification
import com.nunomonteiro.notificationwebhooks.data.model.NotificationStatus
import com.nunomonteiro.notificationwebhooks.data.model.WebhookStatus
import com.nunomonteiro.notificationwebhooks.data.repository.NotificationRepository
import com.nunomonteiro.notificationwebhooks.data.repository.RuleRepository
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: NotificationRepository = NotificationRepository(application)
    private val ruleRepository: RuleRepository = RuleRepository(application)
    
    // LiveData para paginação
    val paginatedNotifications: LiveData<List<Notification>> = repository.paginatedNotifications
    
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore
    
    // Filtro atual
    private val _currentFilter = MutableLiveData(NotificationRepository.FilterType.ALL_VISIBLE)
    val currentFilter: LiveData<NotificationRepository.FilterType> = _currentFilter
    
    // Contadores
    private val _countAll = MutableLiveData(0)
    val countAll: LiveData<Int> = _countAll
    
    private val _countPending = MutableLiveData(0)
    val countPending: LiveData<Int> = _countPending
    
    private val _countAutomatic = MutableLiveData(0)
    val countAutomatic: LiveData<Int> = _countAutomatic
    
    private val _countHidden = MutableLiveData(0)
    val countHidden: LiveData<Int> = _countHidden
    
    private val _countWebhookSuccess = MutableLiveData(0)
    val countWebhookSuccess: LiveData<Int> = _countWebhookSuccess
    
    private val _countWebhookError = MutableLiveData(0)
    val countWebhookError: LiveData<Int> = _countWebhookError
    
    private val _countApproved = MutableLiveData(0)
    val countApproved: LiveData<Int> = _countApproved
    
    private val _countRejected = MutableLiveData(0)
    val countRejected: LiveData<Int> = _countRejected
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        // Atualizar contadores com um pequeno delay para garantir que o Repository está inicializado
        viewModelScope.launch {
            delay(100)
            refreshCounters()
        }
    }
    
    private fun atualizarContadores() {
        viewModelScope.launch {
            try {
                val all = repository.getVisibleCount()
                val pending = repository.getCountByStatus(NotificationStatus.PENDING_AUTH.name)
                val auto = repository.getCountByMultipleStatuses(
                    listOf(NotificationStatus.AUTO_SUCCESS.name, NotificationStatus.AUTO_ERROR.name)
                )
                val hidden = repository.getHiddenCount()
                val webhookSuccess = repository.getCountByWebhookStatus(WebhookStatus.SUCCESS.name)
                val webhookError = repository.getCountByWebhookStatus(WebhookStatus.FAILED.name)
                val approved = repository.getCountByMultipleStatuses(
                    listOf(NotificationStatus.APPROVED_SUCCESS.name, NotificationStatus.APPROVED_ERROR.name)
                )
                val rejected = repository.getCountByStatus(NotificationStatus.REJECTED.name)
                
                Log.d("NotificationViewModel", "📊 Contadores atualizados:")
                Log.d("NotificationViewModel", "   all=$all, pending=$pending, auto=$auto")
                Log.d("NotificationViewModel", "   hidden=$hidden, webhookSuccess=$webhookSuccess")
                Log.d("NotificationViewModel", "   webhookError=$webhookError, approved=$approved, rejected=$rejected")
                
                _countAll.postValue(all)
                _countPending.postValue(pending)
                _countAutomatic.postValue(auto)
                _countHidden.postValue(hidden)
                _countWebhookSuccess.postValue(webhookSuccess)
                _countWebhookError.postValue(webhookError)
                _countApproved.postValue(approved)
                _countRejected.postValue(rejected)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Erro ao atualizar contadores", e)
            }
        }
    }
    
    fun carregarPrimeiraPagina(filter: NotificationRepository.FilterType) {
        _currentFilter.value = filter
        repository.carregarPrimeiraPagina(filter)
        refreshCounters()
    }
    
    fun carregarProximaPagina() {
        repository.carregarProximaPagina()
        refreshCounters()
    }
    
    fun limparNotificacoesDoFiltroAtual() {
        viewModelScope.launch {
            repository.limparNotificacoesDoFiltroAtual()
            refreshCounters()
        }
    }
    
    fun refreshNotifications() {
        _currentFilter.value?.let { filter ->
            repository.carregarPrimeiraPagina(filter)
            refreshCounters()
        }
    }
    
    
    fun rejectNotification(notification: Notification) {
        viewModelScope.launch {
            repository.rejectNotification(notification)
            refreshCounters()
        }
    }
    
    fun hideSimilarNotifications(notification: Notification) {
        viewModelScope.launch {
            repository.hideSimilarNotifications(notification.packageName, notification.title)
            refreshCounters()
        }
    }
    
    fun processNotification(notification: Notification) {
        viewModelScope.launch {
            repository.processarNotificacao(notification)
            delay(500) // Pequeno delay para o processamento
            refreshNotifications()
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

    fun refreshCounters() {
        atualizarContadores()
    }
    
    fun mostrarNotificacoesIguais(notification: Notification) {
        viewModelScope.launch {
            repository.mostrarNotificacoesIguais(notification.packageName, notification.title)
            refreshCounters()
        }
    }

    fun approveNotification(notification: Notification) {
        viewModelScope.launch {
            repository.approveNotification(notification)
            // Refresh adicional para garantir
            delay(500) // Pequeno delay para o webhook começar
            refreshNotifications()
        }
    }

    // ===== MÉTODOS PARA OBTER TEXTO E COR DOS STATUS =====
    
    fun getStatusText(status: NotificationStatus): String {
        return when (status) {
            NotificationStatus.RECEIVED -> "Recebida"
            NotificationStatus.HIDDEN -> "Ocultada"
            NotificationStatus.PENDING_AUTH -> "A aguardar"
            NotificationStatus.APPROVED_SUCCESS -> "Aprovada sem erros"
            NotificationStatus.APPROVED_ERROR -> "Aprovada com erros"
            NotificationStatus.REJECTED -> "Rejeitada"
            NotificationStatus.AUTO_SUCCESS -> "Automática sem erros"
            NotificationStatus.AUTO_ERROR -> "Automática com erros"
            NotificationStatus.PROCESSED -> "Processada"
            else -> "Desconhecido"
        }
    }
    
    fun getStatusIcon(status: NotificationStatus): String {
        return when (status) {
            NotificationStatus.RECEIVED -> "📥"
            NotificationStatus.HIDDEN -> "🙈"
            NotificationStatus.PENDING_AUTH -> "⏳"
            NotificationStatus.APPROVED_SUCCESS -> "✅"
            NotificationStatus.APPROVED_ERROR -> "⚠️"
            NotificationStatus.REJECTED -> "❌"
            NotificationStatus.AUTO_SUCCESS -> "🤖"
            NotificationStatus.AUTO_ERROR -> "🔥"
            NotificationStatus.PROCESSED -> "⚡"
            else -> "❓"
        }
    }
    
    fun getStatusColor(status: NotificationStatus): Int {
        return when (status) {
            NotificationStatus.RECEIVED -> Color.parseColor("#2196F3") // Azul
            NotificationStatus.HIDDEN -> Color.parseColor("#9E9E9E") // Cinza
            NotificationStatus.PENDING_AUTH -> Color.parseColor("#FF9800") // Laranja
            NotificationStatus.APPROVED_SUCCESS -> Color.parseColor("#4CAF50") // Verde
            NotificationStatus.APPROVED_ERROR -> Color.parseColor("#FFC107") // Amarelo
            NotificationStatus.REJECTED -> Color.parseColor("#F44336") // Vermelho
            NotificationStatus.AUTO_SUCCESS -> Color.parseColor("#8BC34A") // Verde claro
            NotificationStatus.AUTO_ERROR -> Color.parseColor("#FF5722") // Laranja escuro
            NotificationStatus.PROCESSED -> Color.parseColor("#9C27B0") // Roxo
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