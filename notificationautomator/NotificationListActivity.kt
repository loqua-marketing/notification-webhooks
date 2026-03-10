package com.seuapp.notificationautomator

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
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
    
    // Botões de filtro
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterProcessed: Button
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterIgnored: Button
    
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
        
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterProcessed = findViewById(R.id.btnFilterProcessed)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnFilterIgnored = findViewById(R.id.btnFilterIgnored)
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
        
        btnFilterIgnored.setOnClickListener {
            viewModel.aplicarFiltro(NotificationViewModel.FilterType.IGNORED)
            atualizarCoresBotoes(NotificationViewModel.FilterType.IGNORED)
        }
    }
    
    private fun atualizarCoresBotoes(filtroAtivo: NotificationViewModel.FilterType) {
        btnFilterAll.alpha = 0.5f
        btnFilterProcessed.alpha = 0.5f
        btnFilterPending.alpha = 0.5f
        btnFilterIgnored.alpha = 0.5f
        
        when (filtroAtivo) {
            NotificationViewModel.FilterType.ALL -> btnFilterAll.alpha = 1.0f
            NotificationViewModel.FilterType.PROCESSED -> btnFilterProcessed.alpha = 1.0f
            NotificationViewModel.FilterType.PENDING -> btnFilterPending.alpha = 1.0f
            NotificationViewModel.FilterType.IGNORED -> btnFilterIgnored.alpha = 1.0f
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
            btnFilterProcessed.text = "✅ Processadas ($count)"
        }
        
        viewModel.countPending.observe(this) { count ->
            btnFilterPending.text = "⏳ Aguardam ($count)"
        }
        
        viewModel.countIgnored.observe(this) { count ->
            btnFilterIgnored.text = "⭕ Ignoradas ($count)"
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
            val btnApprove = itemView.findViewById<Button>(R.id.btnApprove)
            
            val appName = getAppName(notification.packageName)
            tvPackage.text = appName
            tvTitle.text = notification.title ?: "(sem título)"
            tvText.text = notification.text ?: "(sem texto)"
            tvTime.text = dateFormat.format(Date(notification.timestamp))
            
            val (statusText, statusColor) = when (notification.status) {
                NotificationStatus.RECEIVED -> Pair("📥 Recebida", Color.parseColor("#2196F3"))
                NotificationStatus.PROCESSED -> Pair("✅ Processada", Color.parseColor("#4CAF50"))
                NotificationStatus.PENDING_AUTH -> Pair("⏳ Aguarda", Color.parseColor("#FF9800"))
                NotificationStatus.IGNORED -> Pair("⭕ Ignorada", Color.parseColor("#9E9E9E"))
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(statusColor)
            
            // Botão de aprovar (só aparece para pendentes)
            if (notification.status == NotificationStatus.PENDING_AUTH) {
                btnApprove.visibility = Button.VISIBLE
                btnApprove.setOnClickListener {
                    viewModel.approveNotification(notification)
                }
            } else {
                btnApprove.visibility = Button.GONE
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
        
        // Adicionar opção de executar regra se já existir
        if (notification.ruleId != null) {
            opcoes.add(1, "▶️ Executar Regra Agora")
        }
        
        opcoes.add("❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("$appName")
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "⚡ Criar Regra" -> criarRegra(notification)
                    "▶️ Executar Regra Agora" -> executarRegraAgora(notification)
                    "📋 Ver Detalhes" -> mostrarDetalhes(notification)
                }
            }
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
            putExtra("apply_rule_now", true)  // Nova flag para aplicar regra agora
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
            NotificationStatus.IGNORED -> "Ignorada"
        }
        
        val detalhes = """
            📦 App: $appName
            
            📝 Título: ${notification.title ?: "(sem título)"}
            
            💬 Texto: ${notification.text ?: "(sem texto)"}
            
            ⏰ Data: ${dateFormat.format(Date(notification.timestamp))}
            
            🏷️ Status: $statusString
            
            ${if (notification.ruleId != null) "⚡ Regra ID: ${notification.ruleId}" else ""}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Detalhes da Notificação")
            .setMessage(detalhes)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}