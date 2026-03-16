package com.nunomonteiro.notificationwebhooks.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nunomonteiro.notificationwebhooks.data.model.SavedWebhook
import com.nunomonteiro.notificationwebhooks.data.model.SecurityConfig
import com.nunomonteiro.notificationwebhooks.data.model.AuthConfig
import com.nunomonteiro.notificationwebhooks.data.model.AuthType

class WebhookRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("webhook_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        @Volatile
        private var INSTANCE: WebhookRepository? = null
        
        fun getInstance(context: Context): WebhookRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WebhookRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val KEY_SAVED_WEBHOOKS = "saved_webhooks"
    
    fun getAllSavedWebhooks(): List<SavedWebhook> {
        Log.d("WebhookRepo", "getAllSavedWebhooks() chamado")
        val jsonString = prefs.getString(KEY_SAVED_WEBHOOKS, null)
        if (jsonString.isNullOrBlank()) return emptyList()
        
        return try {
            val type = object : TypeToken<List<SavedWebhook>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("WebhookRepo", "Erro ao ler webhooks", e)
            emptyList()
        }
    }
    
    fun saveWebhook(
        name: String, 
        url: String, 
        isDefault: Boolean = false,
        securityConfig: SecurityConfig? = null,
        headers: Map<String, String>? = null,
        timeoutSeconds: Int = 10,
        maxRetries: Int = 3,
        useCustomPayload: Boolean = false,
        customPayloadTemplate: String? = null,
        waitForResponse: Boolean = false
    ): SavedWebhook {
        Log.d("WebhookRepo", "saveWebhook() chamado: $name - $url")
        val currentList = getAllSavedWebhooks().toMutableList()
        
        val shouldBeDefault = isDefault || currentList.isEmpty()
        
        if (shouldBeDefault) {
            currentList.forEachIndexed { index, webhook ->
                if (webhook.isDefault) {
                    currentList[index] = webhook.copy(isDefault = false)
                }
            }
        }
        
        val newWebhook = SavedWebhook(
            name = name,
            url = url,
            isDefault = shouldBeDefault,
            securityConfig = securityConfig,
            headers = headers,
            timeoutSeconds = timeoutSeconds,
            maxRetries = maxRetries,
            useCustomPayload = useCustomPayload,
            customPayloadTemplate = customPayloadTemplate,
            waitForResponse = waitForResponse
        )
        
        currentList.add(newWebhook)
        saveList(currentList)
        
        return newWebhook
    }
    
    fun updateWebhook(
        oldName: String, 
        newName: String, 
        newUrl: String, 
        isDefault: Boolean = false,
        securityConfig: SecurityConfig? = null,
        headers: Map<String, String>? = null,
        timeoutSeconds: Int = 10,
        maxRetries: Int = 3,
        useCustomPayload: Boolean = false,
        customPayloadTemplate: String? = null,
        waitForResponse: Boolean = false
    ): Boolean {
        val currentList = getAllSavedWebhooks().toMutableList()
        val index = currentList.indexOfFirst { it.name == oldName }
        
        if (index == -1) return false
        
        if (isDefault) {
            currentList.forEachIndexed { i, webhook ->
                if (i != index && webhook.isDefault) {
                    currentList[i] = webhook.copy(isDefault = false)
                }
            }
        }
        
        val updated = SavedWebhook(
            name = newName,
            url = newUrl,
            isDefault = isDefault,
            securityConfig = securityConfig,
            headers = headers,
            timeoutSeconds = timeoutSeconds,
            maxRetries = maxRetries,
            useCustomPayload = useCustomPayload,
            customPayloadTemplate = customPayloadTemplate,
            waitForResponse = waitForResponse
        )
        
        currentList[index] = updated
        saveList(currentList)
        
        return true
    }
    
    fun deleteWebhook(name: String): Boolean {
        val currentList = getAllSavedWebhooks().toMutableList()
        val removed = currentList.removeAll { it.name == name }
        
        if (removed) {
            saveList(currentList)
        }
        
        return removed
    }
    
    fun getDefaultWebhook(): SavedWebhook? {
        val list = getAllSavedWebhooks()
        return list.find { it.isDefault } ?: list.firstOrNull()
    }
    
    fun initDefaultWebhookIfNeeded() {
        val list = getAllSavedWebhooks()
        if (list.isEmpty()) {
            saveWebhook("Localhost", "http://localhost/", true)
        }
    }
    
    private fun saveList(list: List<SavedWebhook>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_SAVED_WEBHOOKS, json).apply()
    }
    
    fun clearAll() {
        prefs.edit().remove(KEY_SAVED_WEBHOOKS).apply()
    }
}