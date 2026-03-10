package com.seuapp.notificationautomator

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
    private lateinit var tvCountProcessed: TextView
    private lateinit var tvCountPending: TextView
    private lateinit var tvCountReceived: TextView
    private lateinit var tvCountRejected: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var btnSettings: Button
    private lateinit var pm: PackageManager
    private val TAG = "NotificationList"
    
    // Botões de filtro
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterProcessed: Button
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterReceived: Button
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
        tvCountProcessed = findViewById(R.id.tvCountProcessed)
        tvCountPending = findViewById(R.id.tvCountPending)
        tvCountReceived = findViewById(R.id.tvCountReceived)
        tvCountRejected = findViewById(R.id.tvCountRejected)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        btnSettings = findViewById(R.id.btnSettings)
        
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterProcessed = findViewById(R.id.btnFilterProcessed)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterReceived = findViewById(R.id.btnFilterReceived)
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
        
        btnFilterAll.setOnClickListener {
            viewModel.aplicarFiltro(NotificationViewModel.FilterType.ALL)
            atualizarCoresBotoes(NotificationViewModel.FilterType.ALL)
        }
        
        btnFilterProcessed.setOnClickListener {
            viewModel.aplicarFiltro(NotificationViewModel.FilterType.PROCESSED)
            atualizarCoresBotoes(NotificationViewModel.FilterType.PROCESSED)
        }
        
        btnFilterPending.setOnClickListener {
            viewModel.aplicarFiltro(NotificationViewModel.FilterType.PENDING)
            atualizarCoresBotoes(NotificationViewModel.FilterType.PENDING)
        }
        
        btnFilterReceived.setOnClickListener {
            viewModel.aplicarFiltro(NotificationViewModel.FilterType.RECEIVED)
            atualizarCoresBotoes(NotificationViewModel.FilterType.RECEIVED)
        }
        
        btnFilterRejected.setOnClickListener { viewModel.aplicarFiltro(NotificationViewModel.FilterType.REJECTED) }
        

    }
    
        private fun atualizarCoresBotoes(filtroAtivo: NotificationViewModel.FilterType) {
            btnFilterAll.alpha = 0.5f
            btnFilterProcessed.alpha = 0.5f
            btnFilterPending.alpha = 0.5f
            btnFilterReceived.alpha = 0.5f
            btnFilterRejected.alpha = 0.5f
            
            when (filtroAtivo) {
                NotificationViewModel.FilterType.ALL -> btnFilterAll.alpha = 1.0f
                NotificationViewModel.FilterType.PROCESSED -> btnFilterProcessed.alpha = 1.0f
                NotificationViewModel.FilterType.PENDING -> btnFilterPending.alpha = 1.0f
                NotificationViewModel.FilterType.RECEIVED -> btnFilterReceived.alpha = 1.0f
                NotificationViewModel.FilterType.REJECTED -> btnFilterRejected.alpha = 1.0f
                else -> { /* não faz nada para outros filtros (ERROR) */ }
            }
        }
    
    private fun observeData() {
        viewModel.filteredNotifications.observe(this) { notifications ->
            atualizarLista(notifications)
        }
        
        viewModel.countAll.observe(this) { count ->
            tvCount.text = "Total: $count"
        }
        
        viewModel.countProcessed.observe(this) { count ->
            tvCountProcessed.text = "✓ $count"
        }
        
        viewModel.countPending.observe(this) { count ->
            tvCountPending.text = "⏳ $count"
        }
        
        viewModel.countReceived.observe(this) { count ->
            tvCountReceived.text = "📥 $count"
        }
        viewModel.countRejected.observe(this) { count ->
            tvCountRejected.text = "✗ $count"
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
        
        // NÃO agrupar notificações - mostrar todas individualmente
        Log.d(TAG, "Mostrando ${notifications.size} notificações")
        
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
            
            val statusIcon = viewModel.getStatusIcon(notification.status)
            val statusText = viewModel.getStatusText(notification.status)
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
        
        if (notification.status == NotificationStatus.RECEIVED) {
            opcoes.add("⚡ Processar com Regras")
        }
        
        if (notification.ruleId != null) {
            opcoes.add("▶️ Executar Regra Agora")
        }
        
        opcoes.add("📋 Ver Detalhes")
        opcoes.add("❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("$appName")
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "⚡ Criar Regra" -> criarRegra(notification)
                    "⚡ Processar com Regras" -> processarComRegras(notification)
                    "▶️ Executar Regra Agora" -> executarRegraAgora(notification)
                    "📋 Ver Detalhes" -> mostrarDetalhes(notification)
                }
            }
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
        
        val statusString = viewModel.getStatusText(notification.status)
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
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Detalhes da Notificação")
            .setMessage(detalhes)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}
