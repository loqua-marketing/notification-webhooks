package com.seuapp.notificationautomator.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.data.repository.RuleRepository
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    
    private val repository = RuleRepository(context)
    private val gson = Gson()
    private val TAG = "BackupManager"
    
    fun exportRules(callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Buscar todas as regras
                val rules = repository.getAllRulesSync()
                
                if (rules.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "No rules to export")
                    }
                    return@launch
                }
                
                // Criar objeto de backup
                val backup = BackupData(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    rules = rules,
                    appVersion = "1.0"
                )
                
                // Converter para JSON
                val json = gson.toJson(backup)
                
                // Criar nome do ficheiro com data
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "notification_rules_${dateFormat.format(Date())}.json"
                
                // Guardar no Download (para Android 10+)
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    
                    val file = File(downloadsDir, fileName)
                    file.writeText(json)
                    
                    withContext(Dispatchers.Main) {
                        callback(true, file.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, "Cannot access external storage")
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false, "Error: ${e.message}")
                }
            }
        }
    }
    
    fun importRules(uri: Uri, contentResolver: ContentResolver, callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ler conteúdo do URI
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                
                if (json.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Empty file")
                    }
                    return@launch
                }
                
                // Parse JSON
                val type = object : TypeToken<BackupData>() {}.type
                val backup: BackupData = gson.fromJson(json, type)
                
                // Validar backup
                if (backup.version != 1) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Unsupported backup version")
                    }
                    return@launch
                }
                
                // Importar regras
                var imported = 0
                for (rule in backup.rules) {
                    // Criar nova regra (sem o ID antigo)
                    val newRule = rule.copy(id = 0)
                    repository.insertRule(newRule)
                    imported++
                }
                
                withContext(Dispatchers.Main) {
                    callback(true, "Imported $imported rules")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false, "Error: ${e.message}")
                }
            }
        }
    }
    
    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val rules: List<Rule>,
        val appVersion: String
    )
}
