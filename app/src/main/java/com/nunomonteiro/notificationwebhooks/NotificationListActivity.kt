package com.nunomonteiro.notificationwebhooks

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nunomonteiro.notificationwebhooks.data.model.Notification
import com.nunomonteiro.notificationwebhooks.data.model.NotificationStatus
import com.nunomonteiro.notificationwebhooks.data.model.WebhookStatus
import com.nunomonteiro.notificationwebhooks.data.repository.NotificationRepository
import com.nunomonteiro.notificationwebhooks.ui.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationListActivity : AppCompatActivity() {
    
    private lateinit var viewModel: NotificationViewModel
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var btnSettings: Button
    private lateinit var pm: PackageManager
    
    // Botões de filtro (8 botões)
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterAutomatic: Button
    private lateinit var btnFilterHidden: Button
    private lateinit var btnFilterWebhookSuccess: Button
    private lateinit var btnFilterWebhookError: Button
    private lateinit var btnFilterApproved: Button
    private lateinit var btnFilterRejected: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_list)
        
        pm = packageManager
        initViews()
        setupViewModel()
        setupListeners()
        observeData()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshNotifications()
    }
    
    private fun initViews() {
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        btnSettings = findViewById(R.id.btnSettings)
        
        // Inicializar todos os botões de filtro
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterAutomatic = findViewById(R.id.btnFilterAutomatic)
        btnFilterHidden = findViewById(R.id.btnFilterHidden)
        btnFilterWebhookSuccess = findViewById(R.id.btnFilterWebhookSuccess)
        btnFilterWebhookError = findViewById(R.id.btnFilterWebhookError)
        btnFilterApproved = findViewById(R.id.btnFilterApproved)
        btnFilterRejected = findViewById(R.id.btnFilterRejected)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
    }
    
    private fun setupListeners() {
        btnRefresh.setOnClickListener {
            viewModel.refreshNotifications()
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show()
        }
        
    btnClear.setOnClickListener {
        val filtroAtual = viewModel.currentFilter.value
        val mensagem = when (filtroAtual) {
            NotificationRepository.FilterType.ALL_VISIBLE -> "apagar TODAS as notificações visíveis"
            NotificationRepository.FilterType.PENDING -> "apagar TODAS as notificações PENDENTES"
            NotificationRepository.FilterType.AUTOMATIC -> "apagar TODAS as notificações AUTOMÁTICAS"
            NotificationRepository.FilterType.HIDDEN -> "apagar TODAS as notificações OCULTAS"
            NotificationRepository.FilterType.WEBHOOK_SUCCESS -> "apagar TODAS as notificações com SUCESSO"
            NotificationRepository.FilterType.WEBHOOK_ERROR -> "apagar TODAS as notificações com ERRO"
            NotificationRepository.FilterType.APPROVED -> "apagar TODAS as notificações APROVADAS"
            NotificationRepository.FilterType.REJECTED -> "apagar TODAS as notificações REJEITADAS"
            else -> "apagar estas notificações"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Limpar notificações")
            .setMessage("Tem a certeza que pretende $mensagem?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.limparNotificacoesDoFiltroAtual()
                Toast.makeText(this, "Notificações apagadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
        
        btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        
        // Listeners dos filtros
        btnFilterAll.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.ALL_VISIBLE)
        }
        
        btnFilterPending.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.PENDING)
        }
        
        btnFilterAutomatic.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.AUTOMATIC)
        }
        
        btnFilterHidden.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.HIDDEN)
        }
        
        btnFilterWebhookSuccess.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.WEBHOOK_SUCCESS)
        }
        
        btnFilterWebhookError.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.WEBHOOK_ERROR)
        }
        
        btnFilterApproved.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.APPROVED)
        }
        
        btnFilterRejected.setOnClickListener {
            viewModel.carregarPrimeiraPagina(NotificationRepository.FilterType.REJECTED)
        }
    }
    
    private fun observeData() {
        viewModel.paginatedNotifications.observe(this) { notifications ->
            atualizarLista(notifications)
        }
        
        viewModel.countAll.observe(this) { count ->
            btnFilterAll.text = if (count > 0) "📥 $count" else "📥"
        }
        
        viewModel.countPending.observe(this) { count ->
            btnFilterPending.text = if (count > 0) "⏳ $count" else "⏳"
        }
        
        viewModel.countAutomatic.observe(this) { count ->
            btnFilterAutomatic.text = if (count > 0) "🤖 $count" else "🤖"
        }
        
        viewModel.countHidden.observe(this) { count ->
            btnFilterHidden.text = if (count > 0) "🙈 $count" else "🙈"
        }
        
        viewModel.countWebhookSuccess.observe(this) { count ->
            btnFilterWebhookSuccess.text = if (count > 0) "🌐 $count" else "🌐"
        }
        
        viewModel.countWebhookError.observe(this) { count ->
            btnFilterWebhookError.text = if (count > 0) "⚠️ $count" else "⚠️"
        }
        
        viewModel.countApproved.observe(this) { count ->
            btnFilterApproved.text = if (count > 0) "✅ $count" else "✅"
        }
        
        viewModel.countRejected.observe(this) { count ->
            btnFilterRejected.text = if (count > 0) "❌ $count" else "❌"
        }
        
        viewModel.currentFilter.observe(this) { filter ->
            atualizarCoresBotoes(filter)
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun atualizarCoresBotoes(filtroAtivo: NotificationRepository.FilterType) {
        // Lista de todos os botões com seus respectivos ícones
        val botoes = listOf(
            btnFilterAll to "📥",
            btnFilterPending to "⏳",
            btnFilterAutomatic to "🤖",
            btnFilterHidden to "🙈",
            btnFilterWebhookSuccess to "🌐",
            btnFilterWebhookError to "⚠️",
            btnFilterApproved to "✅",
            btnFilterRejected to "❌"
        )
        
        // Resetar todos para inativo
        botoes.forEach { (button, icon) ->
            button.isSelected = false
            button.alpha = 0.5f
            button.elevation = 0f
            button.text = icon
        }
        
        // Ativar o botão correspondente
        val botaoAtivo = when (filtroAtivo) {
            NotificationRepository.FilterType.ALL_VISIBLE -> btnFilterAll
            NotificationRepository.FilterType.PENDING -> btnFilterPending
            NotificationRepository.FilterType.AUTOMATIC -> btnFilterAutomatic
            NotificationRepository.FilterType.HIDDEN -> btnFilterHidden
            NotificationRepository.FilterType.WEBHOOK_SUCCESS -> btnFilterWebhookSuccess
            NotificationRepository.FilterType.WEBHOOK_ERROR -> btnFilterWebhookError
            NotificationRepository.FilterType.APPROVED -> btnFilterApproved
            NotificationRepository.FilterType.REJECTED -> btnFilterRejected
        }
        
        botaoAtivo.isSelected = true
        botaoAtivo.alpha = 1.0f
        botaoAtivo.elevation = 4f
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val name = pm.getApplicationLabel(appInfo).toString()
            
            when (packageName) {
                "com.nunomonteiro.notificationwebhooks" -> "Notification Auto"
                else -> name
            }
        } catch (e: Exception) {
            when (packageName) {
                "com.nunomonteiro.notificationwebhooks" -> "Notification Auto"
                "com.google.android.gm" -> "Gmail"
                "com.whatsapp" -> "WhatsApp"
                "com.facebook.katana" -> "Facebook"
                "com.instagram.android" -> "Instagram"
                "com.linkedin.android" -> "LinkedIn"
                else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
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
        
        notifications.forEach { notification ->
            val itemView = layoutInflater.inflate(R.layout.item_notification, container, false)
            
            val tvPackage = itemView.findViewById<TextView>(R.id.tvPackage)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            val tvText = itemView.findViewById<TextView>(R.id.tvText)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val tvError = itemView.findViewById<TextView>(R.id.tvError)
            val layoutActions = itemView.findViewById<LinearLayout>(R.id.layoutActions)
            val btnApprove = itemView.findViewById<Button>(R.id.btnApprove)
            val btnReject = itemView.findViewById<Button>(R.id.btnReject)
            
            val appName = getAppName(notification.packageName)
            tvPackage.text = appName
            tvTitle.text = notification.title ?: "(sem título)"
            tvText.text = notification.text ?: "(sem texto)"
            tvTime.text = dateFormat.format(Date(notification.timestamp))
            
            // Status e cor - usando os novos métodos do ViewModel
            tvStatus.text = "${viewModel.getStatusIcon(notification.status)} ${viewModel.getStatusText(notification.status)}"
            tvStatus.setTextColor(viewModel.getStatusColor(notification.status))
            
            // Mostrar mensagem de erro se houver
            if (!notification.webhookError.isNullOrBlank()) {
                tvError.text = "❌ Erro: ${notification.webhookError}"
                tvError.visibility = TextView.VISIBLE
            } else {
                tvError.visibility = TextView.GONE
            }
            
            // Mostrar resposta do servidor se houver
            if (!notification.webhookResponse.isNullOrBlank()) {
                val tvResposta = TextView(itemView.context).apply {
                    text = "📥 Resposta: ${notification.webhookResponse.take(100)}${if (notification.webhookResponse.length > 100) "..." else ""}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#4CAF50"))
                    setPadding(0, 4, 0, 0)
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                
                // Adicionar ao layout principal
                val layoutPrincipal = itemView.findViewById<LinearLayout>(R.id.linearLayout)
                if (layoutPrincipal != null) {
                    layoutPrincipal.addView(tvResposta)
                } else {
                    (itemView as? ViewGroup)?.addView(tvResposta)
                }
            }
            
            // Botões de ação (só aparece para pendentes)
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
        
        // Opção para ocultar/mostrar
        if (notification.isHidden) {
            opcoes.add("👁️ Mostrar novamente")
        } else {
            opcoes.add("🙈 Ocultar notificações iguais")
        }
        
        // Opção "Criar Regra" (sempre disponível)
        opcoes.add("⚡ Criar Regra")
        
        // 🆕 Opção "Executar Regra Agora" - disponível para TODAS as notificações
        opcoes.add("▶️ Executar Regra Agora")
        
        // Opção "Ver Detalhes"
        opcoes.add("📋 Ver Detalhes")
        
        // Opção "Cancelar"
        opcoes.add("❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("$appName")
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "🙈 Ocultar notificações iguais" -> ocultarNotificacoes(notification)
                    "👁️ Mostrar novamente" -> mostrarNotificacoes(notification)
                    "⚡ Criar Regra" -> criarRegra(notification)
                    "▶️ Executar Regra Agora" -> executarRegraAgora(notification)
                    "📋 Ver Detalhes" -> mostrarDetalhes(notification)
                }
            }
            .show()
    }
    
    private fun ocultarNotificacoes(notification: Notification) {
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
    
    private fun mostrarNotificacoes(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Mostrar notificações")
            .setMessage("Deseja mostrar novamente as notificações ocultas?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.mostrarNotificacoesIguais(notification)
                Toast.makeText(this, "Notificações visíveis", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun executarRegraAgora(notification: Notification) {
        AlertDialog.Builder(this)
            .setTitle("Executar Regra")
            .setMessage("Deseja executar a regra para esta notificação agora?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.processNotification(notification)
                // Adicionar um pequeno delay para a regra ser processada
                // e depois atualizar a lista
                viewModel.refreshNotifications()
                Toast.makeText(this, "Regra executada!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
    
    private fun criarRegra(notification: Notification) {
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
    
    private fun mostrarDetalhes(notification: Notification) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val appName = getAppName(notification.packageName)
        
        val statusString = viewModel.getStatusText(notification.status)
        
        var detalhes = """
            📦 App: $appName
            
            📝 Título: ${notification.title ?: "(sem título)"}
            
            💬 Texto: ${notification.text ?: "(sem texto)"}
            
            ⏰ Data: ${dateFormat.format(Date(notification.timestamp))}
            
            🏷️ Status: $statusString
            
            ${if (notification.ruleId != null) "⚡ Regra ID: ${notification.ruleId}" else ""}
        """.trimIndent()
        
        if (!notification.webhookError.isNullOrBlank()) {
            detalhes += "\n\n❌ Erro: ${notification.webhookError}"
        }
        
        if (!notification.webhookResponse.isNullOrBlank()) {
            detalhes += "\n\n📥 Resposta: ${notification.webhookResponse}"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Detalhes da Notificação")
            .setMessage(detalhes)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}