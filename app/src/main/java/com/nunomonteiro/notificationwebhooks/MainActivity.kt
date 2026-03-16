package com.nunomonteiro.notificationwebhooks

import com.nunomonteiro.notificationwebhooks.data.repository.NotificationRepository
import android.util.Log
import com.nunomonteiro.notificationwebhooks.ml.config.FeatureFlags
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nunomonteiro.notificationwebhooks.utils.BackupManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var btnCheckPermission: Button
    private lateinit var btnViewList: Button
    private lateinit var btnRules: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    private lateinit var btnAbout: Button
    private lateinit var backupManager: BackupManager
    
    private val PERMISSION_REQUEST_CODE = 100
    private val IMPORT_REQUEST_CODE = 200
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        backupManager = BackupManager(this)
        initViews()
        setupListeners()
        updatePermissionStatus()
        
        // 🆕 INICIALIZAR ML O MAIS CEDO POSSÍVEL
        initializeMLAsync()
    }

    private fun initializeMLAsync() {
        if (!FeatureFlags.ML_ENABLED) return
        
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🚀 A inicializar ML...")
                val repository = NotificationRepository(applicationContext)
                repository.initializeML()
                Log.d("MainActivity", "✅ ML inicializado com sucesso")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Erro ao inicializar ML", e)
            }
        }
    }

    private fun requestBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${packageName}")
            )
            startActivity(intent)
        }
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        btnCheckPermission = findViewById(R.id.btnCheckPermission)
        btnViewList = findViewById(R.id.btnViewList)
        btnRules = findViewById(R.id.btnRules)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnAbout = findViewById(R.id.btnAbout)
    }
    
    private fun setupListeners() {
        btnCheckPermission.setOnClickListener { checkAllPermissions() }
        btnViewList.setOnClickListener { 
            startActivity(Intent(this, NotificationListActivity::class.java))
        }
        btnRules.setOnClickListener {
            startActivity(Intent(this, RuleListActivity::class.java))
        }
        btnExport.setOnClickListener {
            exportRules()
        }
        btnImport.setOnClickListener {
            importRules()
        }
        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
    
    private fun updatePermissionStatus() {
        val notificationGranted = isNotificationAccessGranted()
        val storageGranted = isStoragePermissionGranted()
        
        statusText.text = buildString {
            append(if (notificationGranted) "✅ Notificações" else "❌ Notificações")
            append(" | ")
            append(if (storageGranted) "✅ Armazenamento" else "❌ Armazenamento")
        }
    }
    
    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) ?: false
    }
    
    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(this, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(this, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun checkAllPermissions() {
        if (!isNotificationAccessGranted()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (!isStoragePermissionGranted()) {
                requestStoragePermission()
                return
            }
        }
        
        Toast.makeText(this, "Todas as permissões concedidas!", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                updatePermissionStatus()
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissões concedidas!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permissões negadas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun exportRules() {
        if (!checkStoragePermission()) {
            requestStoragePermission()
            return
        }
        
        backupManager.exportRules { success, message ->
            if (success) {
                Toast.makeText(this, "✅ Regras exportadas para: $message", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ Export falhou: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun importRules() {
        if (!checkStoragePermission()) {
            requestStoragePermission()
            return
        }
        
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, IMPORT_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                backupManager.importRules(uri, contentResolver) { success, message ->
                    if (success) {
                        Toast.makeText(this, "✅ $message", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ Import falhou: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
