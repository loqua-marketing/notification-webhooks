package com.seuapp.notificationautomator

import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.WebhookStatus
import com.seuapp.notificationautomator.data.repository.NotificationRepository
import com.seuapp.notificationautomator.ui.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationListActivity : AppCompatActivity() {
    
    private lateinit var viewModel: NotificationViewModel
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLoadMore: Button
    
    // Botões de filtro (Linha 1)
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterAutomatic: Button
    private lateinit var btnFilterHidden: Button
    
    // Botões de filtro (Linha 2)
    private lateinit var btnFilterWebhookSuccess: Button
    private lateinit var btnFilterWebhookError: Button
    private lateinit var btnFilterApproved: Button
    private lateinit var btnFilterRejected: Button
    
    // Botões de ação
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    
    private lateinit var pm: PackageManager
    private val TAG = "NotificationList"
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshNotifications()
        viewModel.refreshCounters()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_list)
        
        pm = packageManager
        initViews()
        resetBotoes() // 👈 IMPORTANTE: Reset dos botões para ícones puros
        
        setupViewModel()
        setupListeners()
        observeData()
        
        // Carregar primeira página (Todas)
        viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.ALL_VISIBLE)
        
        // Forçar atualização inicial dos contadores
        viewModel.refreshCounters()
    }

    private fun resetBotoes() {
        // Resetar todos os botões para apenas o ícone (sem números)
        btnFilterAll.text = "📥"
        btnFilterPending.text = "⏳"
        btnFilterAutomatic.text = "🤖"
        btnFilterHidden.text = "🙈"
        btnFilterWebhookSuccess.text = "🌐"
        btnFilterWebhookError.text = "⚠️"
        btnFilterApproved.text = "✅"
        btnFilterRejected.text = "❌"
    }
    
    private fun initViews() {
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        
        // Linha 1
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterAutomatic = findViewById(R.id.btnFilterAutomatic)
        btnFilterHidden = findViewById(R.id.btnFilterHidden)
        
        // Linha 2
        btnFilterWebhookSuccess = findViewById(R.id.btnFilterWebhookSuccess)
        btnFilterWebhookError = findViewById(R.id.btnFilterWebhookError)
        btnFilterApproved = findViewById(R.id.btnFilterApproved)
        btnFilterRejected = findViewById(R.id.btnFilterRejected)
        
        // Ações
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)

        Log.d(TAG, "btnFilterAll.tag = ${btnFilterAll.tag}")
        Log.d(TAG, "btnFilterPending.tag = ${btnFilterPending.tag}")
        Log.d(TAG, "btnFilterAutomatic.tag = ${btnFilterAutomatic.tag}")
        Log.d(TAG, "btnFilterHidden.tag = ${btnFilterHidden.tag}")
        Log.d(TAG, "btnFilterWebhookSuccess.tag = ${btnFilterWebhookSuccess.tag}")
        Log.d(TAG, "btnFilterWebhookError.tag = ${btnFilterWebhookError.tag}")
        Log.d(TAG, "btnFilterApproved.tag = ${btnFilterApproved.tag}")
        Log.d(TAG, "btnFilterRejected.tag = ${btnFilterRejected.tag}")
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
    }
    
    private fun setupListeners() {
        // Filtros Linha 1
        btnFilterAll.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.ALL_VISIBLE)
            atualizarCoresBotoes(NotificationRepository.FilterType.ALL_VISIBLE)
        }
        
        btnFilterPending.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.PENDING)
            atualizarCoresBotoes(NotificationRepository.FilterType.PENDING)
        }
        
        btnFilterAutomatic.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.AUTOMATIC)
            atualizarCoresBotoes(NotificationRepository.FilterType.AUTOMATIC)
        }
        
        btnFilterHidden.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.HIDDEN)
            atualizarCoresBotoes(NotificationRepository.FilterType.HIDDEN)
        }
        
        // Filtros Linha 2
        btnFilterWebhookSuccess.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.WEBHOOK_SUCCESS)
            atualizarCoresBotoes(NotificationRepository.FilterType.WEBHOOK_SUCCESS)
        }
        
        btnFilterWebhookError.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.WEBHOOK_ERROR)
            atualizarCoresBotoes(NotificationRepository.FilterType.WEBHOOK_ERROR)
        }
        
        btnFilterApproved.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.APPROVED)
            atualizarCoresBotoes(NotificationRepository.FilterType.APPROVED)
        }
        
        btnFilterRejected.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.REJECTED)
            atualizarCoresBotoes(NotificationRepository.FilterType.REJECTED)
        }
        
        // Ações
        btnRefresh.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.ALL_VISIBLE)
            atualizarCoresBotoes(NotificationRepository.FilterType.ALL_VISIBLE)
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show()
        }
        
        btnClear.setOnClickListener {
            val filtroAtual = viewModel.currentFilter.value ?: NotificationRepository.FilterType.ALL_VISIBLE
            val mensagem = when (filtroAtual) {
                NotificationRepository.FilterType.ALL_VISIBLE -> "Todas as notificações visíveis"
                NotificationRepository.FilterType.PENDING -> "notificações pendentes"
                NotificationRepository.FilterType.AUTOMATIC -> "notificações automáticas"
                NotificationRepository.FilterType.HIDDEN -> "notificações ocultas"
                NotificationRepository.FilterType.WEBHOOK_SUCCESS -> "notificações com webhook com sucesso"
                NotificationRepository.FilterType.WEBHOOK_ERROR -> "notificações com webhook com erro"
                NotificationRepository.FilterType.APPROVED -> "notificações aprovadas"
                NotificationRepository.FilterType.REJECTED -> "notificações rejeitadas"
            }
            
            AlertDialog.Builder(this)
                .setTitle("Limpar notificações")
                .setMessage("Tem a certeza que pretende apagar $mensagem?")
                .setPositiveButton("Sim") { _, _ ->
                    viewModel.limparNotificacoesDoFiltroAtual()
                    Toast.makeText(this, "Notificações apagadas", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Não", null)
                .show()
        }
        
        btnLoadMore.setOnClickListener {
            viewModel.carregarProximaPagina()
        }
    }
    
    private fun atualizarCoresBotoes(filtroAtivo: NotificationRepository.FilterType) {
        // Resetar estado selecionado de todos os botões
        val todosBotoes = listOf(
            btnFilterAll, btnFilterPending, btnFilterAutomatic, btnFilterHidden,
            btnFilterWebhookSuccess, btnFilterWebhookError, btnFilterApproved, btnFilterRejected
        )
        todosBotoes.forEach { it.isSelected = false }
        
        // Marcar o botão ativo como selecionado
        when (filtroAtivo) {
            NotificationRepository.FilterType.ALL_VISIBLE -> btnFilterAll.isSelected = true
            NotificationRepository.FilterType.PENDING -> btnFilterPending.isSelected = true
            NotificationRepository.FilterType.AUTOMATIC -> btnFilterAutomatic.isSelected = true
            NotificationRepository.FilterType.HIDDEN -> btnFilterHidden.isSelected = true
            NotificationRepository.FilterType.WEBHOOK_SUCCESS -> btnFilterWebhookSuccess.isSelected = true
            NotificationRepository.FilterType.WEBHOOK_ERROR -> btnFilterWebhookError.isSelected = true
            NotificationRepository.FilterType.APPROVED -> btnFilterApproved.isSelected = true
            NotificationRepository.FilterType.REJECTED -> btnFilterRejected.isSelected = true
        }
    }
    
    private fun observeData() {
        viewModel.paginatedNotifications.observe(this) { notifications ->
            atualizarLista(notifications)
        }
        
        viewModel.isLoadingMore.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnLoadMore.isEnabled = !loading
        }
        
        viewModel.hasMore.observe(this) { hasMore ->
            btnLoadMore.visibility = if (hasMore) View.VISIBLE else View.GONE
        }
        
        viewModel.currentFilter.observe(this) { filter ->
            atualizarCoresBotoes(filter)
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        observarContadores()
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val name = pm.getApplicationLabel(appInfo).toString()
            
            when (packageName) {
                "com.seuapp.notificationautomator" -> "Notification Auto"
                else -> name
            }
        } catch (e: Exception) {
            when (packageName) {
                "com.seuapp.notificationautomator" -> "Notification Auto"
                "com.google.android.gm" -> "Gmail"
                "com.whatsapp" -> "WhatsApp"
                "com.facebook.katana" -> "Facebook"
                "com.instagram.android" -> "Instagram"
                "com.linkedin.android" -> "LinkedIn"
                "com.google.android.apps.maps" -> "Maps"
                "android" -> "Sistema Android"
                else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        }
    }
    
    private fun formatarTextoMelhorado(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        return if (texto.contains("@")) {
            "📧 $texto"
        } else {
            texto
        }
    }
    
    private fun atualizarLista(notifications: List<Notification>) {
        container.removeAllViews()
        
        if (notifications.isEmpty()) {
            tvEmpty.visibility = TextView.VISIBLE
            return
        }
        
        tvEmpty.visibility = TextView.GONE
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        
        Log.d(TAG, "Mostrando ${notifications.size} notificações (paginadas)")
        
        notifications.forEach { notification ->
            val itemView = layoutInflater.inflate(R.layout.item_notification, container, false)
            
            val tvPackage = itemView.findViewById<TextView>(R.id.tvPackage)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            val tvText = itemView.findViewById<TextView>(R.id.tvText)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val tvWebhookStatus = itemView.findViewById<TextView>(R.id.tvWebhookStatus)
            val layoutActions = itemView.findViewById<LinearLayout>(R.id.layoutActions)
            val btnApprove = itemView.findViewById<Button>(R.id.btnApprove)
            val btnReject = itemView.findViewById<Button>(R.id.btnReject)
            
            val appName = getAppName(notification.packageName)
            tvPackage.text = appName
            tvTitle.text = notification.title ?: "(sem título)"
            tvText.text = formatarTextoMelhorado(notification.text)
            tvTime.text = dateFormat.format(Date(notification.timestamp))
            
            val statusIcon = viewModel.getStatusIcon(notification.status, notification.webhookStatus)
            val statusText = viewModel.getStatusText(notification.status, notification.webhookStatus)
            tvStatus.text = "$statusIcon $statusText"
            tvStatus.setTextColor(viewModel.getStatusColor(notification.status))
            
            if (notification.webhookStatus == WebhookStatus.FAILED) {
                tvWebhookStatus.text = "❌ Erro: ${notification.webhookError ?: "desconhecido"}"
                tvWebhookStatus.visibility = TextView.VISIBLE
            } else {
                tvWebhookStatus.visibility = TextView.GONE
            }
            
            if (notification.status == NotificationStatus.PENDING_AUTH) {
                layoutActions.visibility = LinearLayout.VISIBLE
                btnApprove.setOnClickListener {
                    viewModel.approveNotification(notification)
                }
                btnReject.setOnClickListener {
                    viewModel.rejectNotification(notification)
                }
            } else {
                layoutActions.visibility = LinearLayout.GONE
            }
            
            itemView.setOnClickListener {
                mostrarOpcoesNotificacao(notification)
            }
            
            container.addView(itemView)
        }
    }
    
    private fun mostrarOpcoesNotificacao(notification: Notification) {
        val appName = getAppName(notification.packageName)
        val opcoes = mutableListOf<String>()
        
        opcoes.add("⚡ Criar Regra")
        
        if (notification.isHidden) {
            opcoes.add("👁️ Mostrar novamente")
        } else {
            opcoes.add("🙈 Ocultar notificações iguais")
        }
        
        if (notification.status == NotificationStatus.RECEIVED && !notification.isHidden) {
            opcoes.add("⚡ Processar com Regras")
        }
        
        if (notification.ruleId != null && !notification.isHidden) {
            opcoes.add("▶️ Executar Regra Agora")
        }
        
        opcoes.add("📋 Ver Detalhes")
        opcoes.add("❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("$appName")
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "⚡ Criar Regra" -> criarRegra(notification)
                    "🙈 Ocultar notificações iguais" -> ocultarNotificacoesIguais(notification)
                    "👁️ Mostrar novamente" -> mostrarNotificacoesIguais(notification)
                    "⚡ Processar com Regras" -> processarComRegras(notification)
                    "▶️ Executar Regra Agora" -> executarRegraAgora(notification)
                    "📋 Ver Detalhes" -> mostrarDetalhes(notification)
                }
            }
            .show()
    }

    private fun mostrarNotificacoesIguais(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Mostrar notificações")
            .setMessage("Deseja mostrar novamente todas as notificações iguais a esta?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.mostrarNotificacoesIguais(notification)
                Toast.makeText(this, "Notificações visíveis novamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun criarRegra(notification: Notification) {
        Log.d(TAG, "Criar regra para: ${notification.packageName} - ${notification.title}")
        val intent = Intent(this, RuleCreateActivity::class.java).apply {
            putExtra("notification_package", notification.packageName)
            putExtra("notification_title", notification.title)
            putExtra("notification_text", notification.text)
            putExtra("notification_timestamp", notification.timestamp)
            putExtra("notification_id", notification.id)
            putExtra("apply_rule_now", true)
        }
        startActivity(intent)
    }
    
    private fun ocultarNotificacoesIguais(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Ocultar notificações")
            .setMessage("Deseja ocultar todas as notificações iguais a esta?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.hideSimilarNotifications(notification)
                Toast.makeText(this, "Notificações ocultadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun atualizarBadge(button: Button, icon: String, count: Int) {
        Log.d("NotificationList", "🔄 atualizarBadge: ${button.tag} = $count")
        if (count > 0) {
            button.text = "$icon $count"
        } else {
            button.text = icon
        }
        Log.d("NotificationList", "   button text depois = ${button.text}")
        button.invalidate()
        button.invalidate()
    }

    private fun observarContadores() {
        viewModel.countAll.observe(this) { count ->
            Log.d("NotificationList", "📥 countAll = $count")
            atualizarBadge(btnFilterAll, "📥", count)
        }
        viewModel.countPending.observe(this) { count ->
            Log.d("NotificationList", "⏳ countPending = $count")
            atualizarBadge(btnFilterPending, "⏳", count)
        }
        viewModel.countAutomatic.observe(this) { count ->
            Log.d("NotificationList", "🤖 countAutomatic = $count")
            atualizarBadge(btnFilterAutomatic, "🤖", count)
        }
        viewModel.countHidden.observe(this) { count ->
            Log.d("NotificationList", "🙈 countHidden = $count")
            atualizarBadge(btnFilterHidden, "🙈", count)
        }
        viewModel.countWebhookSuccess.observe(this) { count ->
            Log.d("NotificationList", "🌐 countWebhookSuccess = $count")
            atualizarBadge(btnFilterWebhookSuccess, "🌐", count)
        }
        viewModel.countWebhookError.observe(this) { count ->
            Log.d("NotificationList", "⚠️ countWebhookError = $count")
            atualizarBadge(btnFilterWebhookError, "⚠️", count)
        }
        viewModel.countApproved.observe(this) { count ->
            Log.d("NotificationList", "✅ countApproved = $count")
            atualizarBadge(btnFilterApproved, "✅", count)
        }
        viewModel.countRejected.observe(this) { count ->
            Log.d("NotificationList", "❌ countRejected = $count")
            atualizarBadge(btnFilterRejected, "❌", count)
        }
    }

    private fun processarComRegras(notification: Notification) {
        Log.d(TAG, "Processar com regras: ${notification.id}")
        AlertDialog.Builder(this)
            .setTitle("Processar Notificação")
            .setMessage("Deseja submeter esta notificação para ser processada pelas regras existentes?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.processNotification(notification)
                Toast.makeText(this, "Notificação submetida para processamento", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun executarRegraAgora(notification: Notification) {
        Log.d(TAG, "Executar regra agora: ${notification.id}")
        AlertDialog.Builder(this)
            .setTitle("Executar Regra")
            .setMessage("Deseja executar a regra para esta notificação agora?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.processNotification(notification)
                Toast.makeText(this, "Regra executada!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun mostrarDetalhes(notification: Notification) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val appName = getAppName(notification.packageName)
        
        val statusString = viewModel.getStatusText(notification.status, notification.webhookStatus)
        val webhookStatus = viewModel.getWebhookStatusText(notification.webhookStatus)
        
        val detalhes = """
            📦 App: $appName
            📦 Package: ${notification.packageName}
            
            📝 Título: ${notification.title ?: "(sem título)"}
            
            💬 Texto: ${notification.text ?: "(sem texto)"}
            
            ⏰ Data: ${dateFormat.format(Date(notification.timestamp))}
            
            🏷️ Status: $statusString
            
            🌐 Webhook: $webhookStatus
            ${if (notification.webhookError != null) "❌ Erro: ${notification.webhookError}" else ""}
            
            ${if (notification.ruleId != null) "⚡ Regra ID: ${notification.ruleId}" else ""}
            ${if (notification.isHidden) "🙈 Ocultada: Sim" else ""}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Detalhes da Notificação")
            .setMessage(detalhes)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}