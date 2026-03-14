package com.seuapp.notificationautomator

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.WebhookStatus
import com.seuapp.notificationautomator.ui.viewmodel.NotificationViewModel
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
        tvCount = findViewById(R.id.tvCount)
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
            AlertDialog.Builder(this)
                .setTitle("Limpar notificações")
                .setMessage("Tem a certeza que pretende apagar todas as notificações?")
                .setPositiveButton("Sim") { _, _ ->
                    viewModel.clearAllNotifications()
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
            viewModel.setFilter(NotificationViewModel.FilterType.ALL_VISIBLE)
        }
        
        btnFilterPending.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.PENDING)
        }
        
        btnFilterAutomatic.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.AUTOMATIC)
        }
        
        btnFilterHidden.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.HIDDEN)
        }
        
        btnFilterWebhookSuccess.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.WEBHOOK_SUCCESS)
        }
        
        btnFilterWebhookError.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.WEBHOOK_ERROR)
        }
        
        btnFilterApproved.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.APPROVED)
        }
        
        btnFilterRejected.setOnClickListener {
            viewModel.setFilter(NotificationViewModel.FilterType.REJECTED)
        }
    }
    
    private fun observeData() {
        viewModel.filteredNotifications.observe(this) { notifications ->
            atualizarLista(notifications)
        }
        
        viewModel.countAll.observe(this) { count ->
            tvCount.text = "Total: $count"
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
    
    private fun atualizarCoresBotoes(filtroAtivo: NotificationViewModel.FilterType) {
        // Lista de todos os botões
        val todosBotoes = listOf(
            btnFilterAll, btnFilterPending, btnFilterAutomatic, btnFilterHidden,
            btnFilterWebhookSuccess, btnFilterWebhookError, btnFilterApproved, btnFilterRejected
        )
        
        // Resetar todos para inativo
        todosBotoes.forEach { button ->
            button.isSelected = false
            button.alpha = 0.6f
        }
        
        // Ativar o botão correspondente
        val botaoAtivo = when (filtroAtivo) {
            NotificationViewModel.FilterType.ALL_VISIBLE -> btnFilterAll
            NotificationViewModel.FilterType.PENDING -> btnFilterPending
            NotificationViewModel.FilterType.AUTOMATIC -> btnFilterAutomatic
            NotificationViewModel.FilterType.HIDDEN -> btnFilterHidden
            NotificationViewModel.FilterType.WEBHOOK_SUCCESS -> btnFilterWebhookSuccess
            NotificationViewModel.FilterType.WEBHOOK_ERROR -> btnFilterWebhookError
            NotificationViewModel.FilterType.APPROVED -> btnFilterApproved
            NotificationViewModel.FilterType.REJECTED -> btnFilterRejected
        }
        
        botaoAtivo.isSelected = true
        botaoAtivo.alpha = 1.0f
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
            
            // Status e cor
            val (statusText, statusColor) = when (notification.status) {
                NotificationStatus.RECEIVED -> Pair("📥 Recebida", Color.parseColor("#2196F3"))
                NotificationStatus.PROCESSED -> Pair("✅ Processada", Color.parseColor("#4CAF50"))
                NotificationStatus.PENDING_AUTH -> Pair("⏳ Aguarda", Color.parseColor("#FF9800"))
                NotificationStatus.APPROVED_SUCCESS -> Pair("✅ Aprovada", Color.parseColor("#4CAF50"))
                NotificationStatus.APPROVED_ERROR -> Pair("⚠️ Aprovada (erro)", Color.parseColor("#FF9800"))
                NotificationStatus.REJECTED -> Pair("❌ Rejeitada", Color.parseColor("#F44336"))
                NotificationStatus.AUTO_SUCCESS -> Pair("🤖 Automática", Color.parseColor("#2196F3"))
                NotificationStatus.AUTO_ERROR -> Pair("🔥 Automática (erro)", Color.parseColor("#F44336"))
                NotificationStatus.HIDDEN -> Pair("🙈 Ocultada", Color.parseColor("#9E9E9E"))
                else -> Pair("📥 Recebida", Color.parseColor("#2196F3"))
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(statusColor)
            
            // Mostrar mensagem de erro se houver
            if (!notification.webhookError.isNullOrBlank()) {
                tvError.text = "❌ Erro: ${notification.webhookError}"
                tvError.visibility = TextView.VISIBLE
            } else {
                tvError.visibility = TextView.GONE
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
        val opcoes = mutableListOf(
            "⚡ Criar Regra",
            "📋 Ver Detalhes"
        )
        
        // Opção para ocultar/mostrar
        if (notification.isHidden) {
            opcoes.add(0, "👁️ Mostrar novamente")
        } else {
            opcoes.add(0, "🙈 Ocultar notificações iguais")
        }
        
        // Adicionar opção de executar regra se já existir
        if (notification.ruleId != null) {
            opcoes.add(1, "▶️ Executar Regra Agora")
        }
        
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
        
        val statusString = when (notification.status) {
            NotificationStatus.RECEIVED -> "Recebida"
            NotificationStatus.PROCESSED -> "Processada"
            NotificationStatus.PENDING_AUTH -> "A aguardar autorização"
            NotificationStatus.APPROVED_SUCCESS -> "Aprovada (webhook OK)"
            NotificationStatus.APPROVED_ERROR -> "Aprovada (webhook falhou)"
            NotificationStatus.REJECTED -> "Rejeitada"
            NotificationStatus.AUTO_SUCCESS -> "Automática (webhook OK)"
            NotificationStatus.AUTO_ERROR -> "Automática (webhook falhou)"
            NotificationStatus.HIDDEN -> "Ocultada"
            else -> "Desconhecido"
        }
        
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