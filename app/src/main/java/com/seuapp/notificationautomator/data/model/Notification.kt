package com.seuapp.notificationautomator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seuapp.notificationautomator.ml.classification.NotificationClassifier  // 👈 Novo import

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
    val webhookError: String? = null,
    // 🆕 NOVO CAMPO (nullable para compatibilidade)
    val category: String? = null,  // "urgent", "transaction", etc.
    val categoryConfidence: Float? = null  // 0.0 a 1.0
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
