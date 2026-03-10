package com.seuapp.notificationautomator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: NotificationStatus = NotificationStatus.RECEIVED,
    val ruleId: Long? = null,
    val webhookUrl: String? = null,
    val webhookStatus: WebhookStatus? = null,
    val webhookResponse: String? = null,
    val webhookError: String? = null
)

enum class NotificationStatus {
    RECEIVED,      // Sem regra, aguarda ação
    PROCESSED,     // Processada automaticamente por regra
    PENDING_AUTH,  // Aguarda autorização
    APPROVED,      // Aprovada manualmente
    REJECTED,      // Rejeitada manualmente
    ERROR          // Erro no webhook
}

enum class WebhookStatus {
    PENDING,       // A enviar
    SUCCESS,       // Enviado com sucesso
    FAILED         // Falhou
}
