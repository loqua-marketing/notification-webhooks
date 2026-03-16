package com.nunomonteiro.notificationwebhooks.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nunomonteiro.notificationwebhooks.data.model.SavedWebhook
import com.nunomonteiro.notificationwebhooks.data.model.SecurityConfig
import com.nunomonteiro.notificationwebhooks.data.model.AdvancedWebhookConfig

class Converters {
    
    private val gson = Gson()
    
    // ===== CONVERTERS EXISTENTES =====
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringMap(string: String?): Map<String, String>? {
        if (string.isNullOrBlank()) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(string, type)
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(string: String?): List<String>? {
        if (string.isNullOrBlank()) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(string, type)
    }
    
    // ===== 🆕 NOVOS CONVERTERS PARA CONFIGURAÇÕES AVANÇADAS =====
    
    // Para lista de SavedWebhook (endereços guardados)
    @TypeConverter
    fun fromSavedWebhookList(value: String?): List<SavedWebhook>? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<List<SavedWebhook>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromListToSavedWebhookJson(list: List<SavedWebhook>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }
    
    // Para SecurityConfig
    @TypeConverter
    fun fromSecurityConfig(value: String?): SecurityConfig? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<SecurityConfig>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromSecurityConfigToJson(config: SecurityConfig?): String? {
        if (config == null) return null
        return gson.toJson(config)
    }
    
    // Para AdvancedWebhookConfig
    @TypeConverter
    fun fromAdvancedConfig(value: String?): AdvancedWebhookConfig? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<AdvancedWebhookConfig>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromAdvancedConfigToJson(config: AdvancedWebhookConfig?): String? {
        if (config == null) return null
        return gson.toJson(config)
    }
}