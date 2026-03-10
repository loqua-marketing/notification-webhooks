package com.seuapp.notificationautomator

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var btnGmail: Button
    private lateinit var btnLinkedIn: Button
    private lateinit var btnFacebook: Button
    private lateinit var btnCheckPermission: Button
    private lateinit var btnViewList: Button
    private lateinit var btnRules: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupListeners()
        updatePermissionStatus()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        btnGmail = findViewById(R.id.btnGmail)
        btnLinkedIn = findViewById(R.id.btnLinkedIn)
        btnFacebook = findViewById(R.id.btnFacebook)
        btnCheckPermission = findViewById(R.id.btnCheckPermission)
        btnViewList = findViewById(R.id.btnViewList)
        btnRules = findViewById(R.id.btnRules)
    }
    
    private fun setupListeners() {
        btnGmail.setOnClickListener { sendTestNotification("Gmail", "Fatura vencida", "A fatura de março está disponível") }
        btnLinkedIn.setOnClickListener { sendTestNotification("LinkedIn", "João quer conectar-se", "Aceitar convite?") }
        btnFacebook.setOnClickListener { sendTestNotification("Facebook", "Nova mensagem", "Maria: Olá, tudo bem?") }
        btnCheckPermission.setOnClickListener { checkNotificationPermission() }
        btnViewList.setOnClickListener { 
            startActivity(Intent(this, NotificationListActivity::class.java))
        }
        btnRules.setOnClickListener {
            try {
                val intent = Intent(this, RuleListActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "A abrir lista de regras...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
    
    private fun updatePermissionStatus() {
        val isGranted = isNotificationAccessGranted()
        statusText.text = if (isGranted) 
            "✅ Acesso a notificações: CONCEDIDO" 
        else 
            "❌ Acesso a notificações: NEGADO"
    }
    
    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) ?: false
    }
    
    private fun checkNotificationPermission() {
        if (!isNotificationAccessGranted()) {
            Toast.makeText(this, "A abrir definições...", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            Toast.makeText(this, "Permissão já concedida!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendTestNotification(appName: String, title: String, text: String) {
        val notificationManager = NotificationManagerCompat.from(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "test_channel",
                "Test Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificações de teste"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(this, "test_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(appName.hashCode(), notification)
        Toast.makeText(this, "Notificação enviada: $appName", Toast.LENGTH_SHORT).show()
    }
}
