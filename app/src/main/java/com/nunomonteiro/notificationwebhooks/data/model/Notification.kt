package com.nunomonteiro.notificationwebhooks.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunomonteiro.notificationwebhooks.ml.classification.NotificationClassifier  // 👈 Novo import

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
    val categoryConfidence: Float? = null,  // 0.0 a 1.0
    val isHidden: Boolean = false  // NOVO campo
)

enum class NotificationStatus {
    RECEIVED,           // Recebida sem regras
    HIDDEN,             // Ocultada pelo utilizador
    PENDING_AUTH,       // A aguardar aprovação
    APPROVED_SUCCESS,    // Aprovada e webhook OK
    APPROVED_ERROR,      // Aprovada mas webhook falhou
    REJECTED,           // Rejeitada
    AUTO_SUCCESS,       // Automática e webhook OK
    AUTO_ERROR,         // Automática mas webhook falhou
    PROCESSED           // Legacy - manter para compatibilidade
}

enum class WebhookStatus {
    PENDING,       // A enviar
    SUCCESS,       // Enviado com sucesso
    FAILED         // Falhou
}