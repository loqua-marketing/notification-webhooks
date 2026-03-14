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
import com.seuapp.notificationautomator.ml.config.FeatureFlags
import com.seuapp.notificationautomator.service.MLService
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
    
    // 🆕 ML Service
    private var mlService: MLService? = null
    
    // Propriedades para paginação
    private val PAGE_SIZE = 25
    private var _currentPage = 0
    private var _hasMorePages = true
    private var _totalNotifications = 0
    
    // LiveData para a UI
    private val _paginatedNotifications = MutableLiveData<List<Notification>>()
    val paginatedNotifications: LiveData<List<Notification>> = _paginatedNotifications
    
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore
    
    // Estado atual do filtro
    private var currentFilterType: FilterType = FilterType.ALL_VISIBLE
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // LiveData antiga (para compatibilidade)
    private val _allNotifications = MutableLiveData<List<Notification>>()
    val allNotifications: LiveData<List<Notification>> = _allNotifications
    
    // Enum para os novos filtros
    enum class FilterType {
        ALL_VISIBLE,        // 📥 Todas (exceto ocultas)
        PENDING,            // ⏳ Pendentes (PENDING_AUTH)
        AUTOMATIC,          // 🤖 Automáticas (AUTO_SUCCESS + AUTO_ERROR)
        HIDDEN,             // 🙈 Ocultas
        WEBHOOK_SUCCESS,    // 🌐 Sucesso
        WEBHOOK_ERROR,      // ⚠️ Erro
        APPROVED,           // ✅ Aprovadas (APPROVED_SUCCESS + APPROVED_ERROR)
        REJECTED            // ❌ Rejeitadas
    }
    
    init {
        // Inicializar com primeira página
        carregarPrimeiraPagina(FilterType.ALL_VISIBLE)

        // Debug: mostrar estatísticas da BD
        coroutineScope.launch {
            debugDatabaseStats()
        }
    }
    
    // 🆕 Inicializar ML
    suspend fun initializeML() {
        if (FeatureFlags.ML_ENABLED) {
            mlService = MLService(appContext)
            mlService?.initialize(notificationDao)
        }
    }
    
    // 🆕 Cleanup ML
    fun cleanupML() {
        mlService?.close()
        mlService = null
    }
    
    // ===== MÉTODOS DE PAGINAÇÃO =====
    
    fun carregarPrimeiraPagina(filterType: FilterType) {
        _currentPage = 0
        _hasMorePages = true
        currentFilterType = filterType
        carregarPagina(0, filterType)
    }
    
    fun carregarProximaPagina() {
        if (!_hasMorePages || _isLoadingMore.value == true) return
        carregarPagina(_currentPage + 1, currentFilterType)
    }
    
    private fun carregarPagina(page: Int, filterType: FilterType) {
        coroutineScope.launch {
            try {
                _isLoadingMore.postValue(true)

                val offset = page * PAGE_SIZE
                val notifications = when (filterType) {
                    FilterType.ALL_VISIBLE -> 
                        notificationDao.getVisibleNotificationsPaginated(PAGE_SIZE, offset)
                    
                    FilterType.PENDING -> 
                        notificationDao.getByStatusPaginated(NotificationStatus.PENDING_AUTH.name, PAGE_SIZE, offset)
                    
                    FilterType.AUTOMATIC -> 
                        notificationDao.getByMultipleStatusesPaginated(
                            listOf(NotificationStatus.AUTO_SUCCESS.name, NotificationStatus.AUTO_ERROR.name),
                            PAGE_SIZE, offset
                        )
                    
                    FilterType.HIDDEN -> 
                        notificationDao.getHiddenNotificationsPaginated(PAGE_SIZE, offset)
                    
                    FilterType.WEBHOOK_SUCCESS -> 
                        notificationDao.getByWebhookStatusPaginated(WebhookStatus.SUCCESS.name, PAGE_SIZE, offset)
                    
                    FilterType.WEBHOOK_ERROR -> 
                        notificationDao.getByWebhookStatusPaginated(WebhookStatus.FAILED.name, PAGE_SIZE, offset)
                    
                    FilterType.APPROVED -> 
                        notificationDao.getByMultipleStatusesPaginated(
                            listOf(NotificationStatus.APPROVED_SUCCESS.name, NotificationStatus.APPROVED_ERROR.name),
                            PAGE_SIZE, offset
                        )
                    
                    FilterType.REJECTED -> 
                        notificationDao.getByStatusPaginated(NotificationStatus.REJECTED.name, PAGE_SIZE, offset)
                }
                
                _totalNotifications = when (filterType) {
                    FilterType.ALL_VISIBLE -> notificationDao.getVisibleCount()
                    FilterType.PENDING -> notificationDao.getCountByStatus(NotificationStatus.PENDING_AUTH.name)
                    FilterType.AUTOMATIC -> notificationDao.getCountByMultipleStatuses(
                        listOf(NotificationStatus.AUTO_SUCCESS.name, NotificationStatus.AUTO_ERROR.name)
                    )
                    FilterType.HIDDEN -> notificationDao.getHiddenCount()
                    FilterType.WEBHOOK_SUCCESS -> notificationDao.getCountByWebhookStatus(WebhookStatus.SUCCESS.name)
                    FilterType.WEBHOOK_ERROR -> notificationDao.getCountByWebhookStatus(WebhookStatus.FAILED.name)
                    FilterType.APPROVED -> notificationDao.getCountByMultipleStatuses(
                        listOf(NotificationStatus.APPROVED_SUCCESS.name, NotificationStatus.APPROVED_ERROR.name)
                    )
                    FilterType.REJECTED -> notificationDao.getCountByStatus(NotificationStatus.REJECTED.name)
                }
                
                _hasMorePages = (offset + PAGE_SIZE) < _totalNotifications
                _hasMore.postValue(_hasMorePages)
                
                if (page == 0) {
                    _paginatedNotifications.postValue(notifications)
                } else {
                    val currentList = _paginatedNotifications.value?.toMutableList() ?: mutableListOf()
                    currentList.addAll(notifications)
                    _paginatedNotifications.postValue(currentList)
                }
                
                _currentPage = page
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar página $page", e)
            } finally {
                _isLoadingMore.postValue(false)
            }
        }
    }
    
    fun resetPagination() {
        _currentPage = 0
        _hasMorePages = true
        currentFilterType = FilterType.ALL_VISIBLE
        _paginatedNotifications.postValue(emptyList())
    }
    
    // ===== MÉTODOS PARA LIMPAR POR FILTRO =====
    
    fun limparNotificacoesDoFiltroAtual() {
        coroutineScope.launch {
            try {
                when (currentFilterType) {
                    FilterType.ALL_VISIBLE -> notificationDao.deleteAllVisible()
                    FilterType.PENDING -> notificationDao.deleteByStatus(NotificationStatus.PENDING_AUTH.name)
                    FilterType.AUTOMATIC -> {
                        notificationDao.deleteByStatus(NotificationStatus.AUTO_SUCCESS.name)
                        notificationDao.deleteByStatus(NotificationStatus.AUTO_ERROR.name)
                    }
                    FilterType.HIDDEN -> notificationDao.deleteAllHidden()
                    FilterType.WEBHOOK_SUCCESS -> notificationDao.deleteByWebhookStatus(WebhookStatus.SUCCESS.name)
                    FilterType.WEBHOOK_ERROR -> notificationDao.deleteByWebhookStatus(WebhookStatus.FAILED.name)
                    FilterType.APPROVED -> {
                        notificationDao.deleteByStatus(NotificationStatus.APPROVED_SUCCESS.name)
                        notificationDao.deleteByStatus(NotificationStatus.APPROVED_ERROR.name)
                    }
                    FilterType.REJECTED -> notificationDao.deleteByStatus(NotificationStatus.REJECTED.name)
                }
                
                // Recarregar primeira página após limpar
                carregarPrimeiraPagina(currentFilterType)
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar notificações", e)
            }
        }
    }
    
    // ===== MÉTODO PARA OCULTAR NOTIFICAÇÕES SIMILARES =====
    
    suspend fun hideSimilarNotifications(packageName: String, title: String?) {
        try {
            notificationDao.hideSimilarNotifications(packageName, title)
            // Recarregar a página atual
            carregarPrimeiraPagina(currentFilterType)
            Log.d(TAG, "🙈 Notificações similares ocultadas: $packageName - ${title ?: "sem título"}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ocultar notificações", e)
        }
    }
    
    // ===== MÉTODO PARA ATUALIZAR STATUS (com base no webhook) =====
    
    suspend fun atualizarStatusAposWebhook(id: Long, webhookStatus: WebhookStatus, error: String? = null) {
        try {
            val notification = notificationDao.getNotificationById(id) ?: return
            
            val novoStatus = when {
                // Se era pendente de aprovação e webhook成功
                notification.status == NotificationStatus.PENDING_AUTH && webhookStatus == WebhookStatus.SUCCESS ->
                    NotificationStatus.APPROVED_SUCCESS
                
                notification.status == NotificationStatus.PENDING_AUTH && webhookStatus == WebhookStatus.FAILED ->
                    NotificationStatus.APPROVED_ERROR
                
                // Se era automática
                notification.status == NotificationStatus.PROCESSED && webhookStatus == WebhookStatus.SUCCESS ->
                    NotificationStatus.AUTO_SUCCESS
                
                notification.status == NotificationStatus.PROCESSED && webhookStatus == WebhookStatus.FAILED ->
                    NotificationStatus.AUTO_ERROR
                
                // Se não se encaixa nos casos acima, manter o status
                else -> notification.status
            }
            
            notificationDao.update(notification.copy(
                status = novoStatus,
                webhookStatus = webhookStatus,
                webhookError = error
            ))
            
            // Recarregar a lista
            carregarPrimeiraPagina(currentFilterType)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar status após webhook", e)
        }
    }
    
    // ===== MÉTODO INSERT NOTIFICATION (ATUALIZADO) =====
    
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
                
                val regraAplicavel = encontrarRegraAplicavel(packageName, title, text, regras)
                
                val statusInicial = if (regraAplicavel != null) {
                    Log.d(TAG, "✅ REGRA ENCONTRADA: ${regraAplicavel.name}")
                    when (regraAplicavel.actionType) {
                        ActionType.WEBHOOK_AUTO -> {
                            Log.d(TAG, "⚡ Ação: Webhook Automático")
                            NotificationStatus.PROCESSED  // Vai ser atualizado depois do webhook
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
                    title = title ?: "",
                    text = text ?: "",
                    timestamp = System.currentTimeMillis(),
                    status = statusInicial,
                    ruleId = regraAplicavel?.id,
                    webhookUrl = regraAplicavel?.webhookUrl ?: "",
                    webhookStatus = if (regraAplicavel != null) WebhookStatus.PENDING else null,
                    category = null,
                    categoryConfidence = null,
                    isHidden = false
                )
                
                val id = notificationDao.insert(notification)
                
                if (FeatureFlags.CLASSIFICATION_ENABLED) {
                    processWithMLAsync(id, title, text)
                }
                
                Log.d(TAG, "💾 Notificação guardada com ID: $id, Status: $statusInicial")
                
                regraAplicavel?.let {
                    ruleDao.incrementTriggerCount(it.id, System.currentTimeMillis())
                }
                
                if (regraAplicavel != null && regraAplicavel.actionType == ActionType.WEBHOOK_AUTO) {
                    Log.d(TAG, "🚀 A executar webhook automático...")
                    executarWebhook(regraAplicavel, notification.copy(id = id))
                }
                
                // Recarregar a primeira página do filtro atual
                carregarPrimeiraPagina(currentFilterType)
                Log.d(TAG, "==========================================")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao processar notificação", e)
                e.printStackTrace()
            }
        }
    }
    
    // ===== MÉTODOS EXISTENTES (MANTER) =====
    
    private fun processWithMLAsync(id: Long, title: String?, text: String?) {
        coroutineScope.launch {
            try {
                mlService?.classifyNotificationAsync(id, title, text) { category, confidence ->
                    launch {
                        notificationDao.updateCategory(id, category, confidence)
                        if (FeatureFlags.ML_DEBUG) {
                            Log.d(TAG, "🤖 ML: Notificação $id classificada como $category ($confidence)")
                        }
                    }
                }
            } catch (e: Exception) {
                if (FeatureFlags.ML_DEBUG) {
                    Log.e(TAG, "❌ Erro no ML async", e)
                }
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
            
            if (regra.appPackage != null) {
                val packageMatch = regra.appPackage == packageName
                matches = matches && packageMatch
                matchDetails.add("Package: ${regra.appPackage} == $packageName? $packageMatch")
            } else {
                matchDetails.add("Package: QUALQUER APP")
            }
            
            if (regra.titleContains != null && title != null) {
                val titleMatch = title.contains(regra.titleContains, ignoreCase = true)
                matches = matches && titleMatch
                matchDetails.add("Título contém '${regra.titleContains}'? $titleMatch")
            } else if (regra.titleContains != null && title == null) {
                matches = false
                matchDetails.add("Título contém '${regra.titleContains}'? false (título nulo)")
            }
            
            if (regra.textContains != null && text != null) {
                val textMatch = text.contains(regra.textContains, ignoreCase = true)
                matches = matches && textMatch
                matchDetails.add("Texto contém '${regra.textContains}'? $textMatch")
            } else if (regra.textContains != null && text == null) {
                matches = false
                matchDetails.add("Texto contém '${regra.textContains}'? false (texto nulo)")
            }
            
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
            
            // Atualizar status para PENDING_AUTH (vai ser atualizado depois do webhook)
            notificationDao.update(notification.copy(status = NotificationStatus.PENDING_AUTH))
            
            rule?.let {
                executarWebhook(it, notification)
            }
            
            carregarPrimeiraPagina(currentFilterType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun rejectNotification(notification: Notification) {
        try {
            Log.d(TAG, "❌ Rejeitando notificação ${notification.id}")
            notificationDao.update(notification.copy(status = NotificationStatus.REJECTED))
            carregarPrimeiraPagina(currentFilterType)
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
                    ActionType.WEBHOOK_AUTO -> NotificationStatus.PROCESSED
                    ActionType.WEBHOOK_AUTH -> NotificationStatus.PENDING_AUTH
                }
                
                notificationDao.update(notification.copy(
                    status = novoStatus,
                    ruleId = regraAplicavel.id,
                    webhookUrl = regraAplicavel.webhookUrl ?: "",
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
            
            carregarPrimeiraPagina(currentFilterType)
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
        return notificationDao.getRecentNotificationCount(packageName, title ?: "", text ?: "", cutoffTime)
    }
    
    suspend fun mostrarNotificacoesIguais(packageName: String, title: String?) {
        try {
            // Atualizar isHidden = 0 para todas as notificações com este package/title
            notificationDao.mostrarNotificacoesIguais(packageName, title)
            // Recarregar a página atual
            carregarPrimeiraPagina(currentFilterType)
            Log.d(TAG, "👁️ Notificações similares mostradas novamente: $packageName - ${title ?: "sem título"}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mostrar notificações", e)
        }
    }

    suspend fun getVisibleCount(): Int {
        val count = notificationDao.getVisibleCount()
        Log.d("NotificationRepo", "📊 getVisibleCount = $count")
        return count
    }

    suspend fun getCountByStatus(status: String): Int {
        val count = notificationDao.getCountByStatus(status)
        Log.d("NotificationRepo", "📊 getCountByStatus($status) = $count")
        return count
    }

    suspend fun getCountByMultipleStatuses(statuses: List<String>): Int {
        val count = notificationDao.getCountByMultipleStatuses(statuses)
        Log.d("NotificationRepo", "📊 getCountByMultipleStatuses($statuses) = $count")
        return count
    }

    suspend fun getHiddenCount(): Int {
        val count = notificationDao.getHiddenCount()
        Log.d("NotificationRepo", "📊 getHiddenCount = $count")
        return count
    }

    suspend fun getCountByWebhookStatus(webhookStatus: String): Int {
        val count = notificationDao.getCountByWebhookStatus(webhookStatus)
        Log.d("NotificationRepo", "📊 getCountByWebhookStatus($webhookStatus) = $count")
        return count
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
                // Usar o novo método que atualiza o status baseado no webhook
                atualizarStatusAposWebhook(id, webhookStatus, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun debugDatabaseStats() {
        try {
            val total = notificationDao.getVisibleCount()
            val pending = notificationDao.getCountByStatus(NotificationStatus.PENDING_AUTH.name)
            val auto = notificationDao.getCountByMultipleStatuses(
                listOf(NotificationStatus.AUTO_SUCCESS.name, NotificationStatus.AUTO_ERROR.name)
            )
            val hidden = notificationDao.getHiddenCount()
            
            Log.d(TAG, "📊 DATABASE STATS:")
            Log.d(TAG, "   TOTAL VISÍVEIS: $total")
            Log.d(TAG, "   PENDING: $pending")
            Log.d(TAG, "   AUTO: $auto")
            Log.d(TAG, "   HIDDEN: $hidden")
            
            // Listar as primeiras 5 notificações para debug
            val sample = notificationDao.getVisibleNotificationsPaginated(5, 0)
            Log.d(TAG, "📋 AMOSTRA (primeiras 5):")
            sample.forEachIndexed { index, notif ->
                Log.d(TAG, "   $index: id=${notif.id}, title=${notif.title}, status=${notif.status}, hidden=${notif.isHidden}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer debug DB", e)
        }
    }

    fun updateNotificationStatus(id: Long, status: NotificationStatus) {
        coroutineScope.launch {
            try {
                val notification = notificationDao.getNotificationById(id)
                if (notification != null) {
                    notificationDao.update(notification.copy(status = status))
                    carregarPrimeiraPagina(currentFilterType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun getNotificationCount(): Int {
        return runBlocking {
            try {
                notificationDao.getVisibleCount()
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
                carregarPrimeiraPagina(currentFilterType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun clearAllNotifications() {
        coroutineScope.launch {
            try {
                notificationDao.deleteAllVisible()
                notificationDao.deleteAllHidden()
                carregarPrimeiraPagina(FilterType.ALL_VISIBLE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}