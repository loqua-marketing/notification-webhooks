package com.seuapp.notificationautomator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.seuapp.notificationautomator.data.database.Converters

// NOVAS CLASSES DE DADOS PARA CONFIGURAÇÕES AVANÇADAS
data class SavedWebhook(
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
    // 🆕 NOVOS CAMPOS
    val securityConfig: SecurityConfig? = null,
    val headers: Map<String, String>? = null,
    val timeoutSeconds: Int = 10,
    val maxRetries: Int = 3,
    val useCustomPayload: Boolean = false,
    val customPayloadTemplate: String? = null,
    val waitForResponse: Boolean = false
)

data class AuthConfig(
    val type: AuthType,          // BASIC, BEARER, API_KEY
    val username: String? = null, // Para Basic Auth
    val password: String? = null, // Para Basic Auth
    val token: String? = null,    // Para Bearer Token
    val apiKeyName: String? = null, // Nome da API Key (ex: "X-API-Key")
    val apiKeyValue: String? = null // Valor da API Key
)

enum class AuthType {
    NONE, BASIC, BEARER, API_KEY
}

data class SecurityConfig(
    val signWithHmac: Boolean = false,
    val hmacSecret: String? = null,
    val pgpEnabled: Boolean = false,
    val pgpPublicKey: String? = null,
    val pgpFormat: String? = null, // "ASCII_ARMOR" ou "BINARY"
    val auth: AuthConfig = AuthConfig(type = AuthType.NONE)
)

data class AdvancedWebhookConfig(
    val timeoutSeconds: Int = 10,
    val maxRetries: Int = 3,
    val useCustomPayload: Boolean = false,
    val customPayloadTemplate: String? = null, // JSON com {{variaveis}}
    val waitForResponse: Boolean = false
)

@Entity(tableName = "rules")
@TypeConverters(Converters::class)
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    
    // Condições (todas em AND)
    val appPackage: String? = null,
    val titleContains: String? = null,
    val textContains: String? = null,
    val hourFrom: String? = null,
    val hourTo: String? = null,
    val daysOfWeek: String? = null,
    val isSilent: Boolean? = null,
    val hasImage: Boolean? = null,
    
    // Ação - CAMPOS EXISTENTES (mantemos para compatibilidade)
    val actionType: ActionType,
    val webhookUrl: String? = null,
    val webhookHeaders: String? = null,
    
    // 🆕 NOVOS CAMPOS para configuração avançada
    val savedWebhooks: String? = null,           // JSON da lista de SavedWebhook (global partilhada)
    val selectedWebhookName: String? = null,      // Nome do webhook selecionado atualmente
    val securityConfig: String? = null,            // JSON do SecurityConfig
    val advancedConfig: String? = null,            // JSON do AdvancedWebhookConfig
    
    // Metadados
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null,
    val triggerCount: Int = 0
)

enum class ActionType {
    WEBHOOK_AUTO,
    WEBHOOK_AUTH
}