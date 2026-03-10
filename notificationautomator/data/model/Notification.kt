package com.seuapp.notificationautomator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: NotificationStatus = NotificationStatus.RECEIVED
)

enum class NotificationStatus {
    RECEIVED,      // Acabou de chegar
    PROCESSED,     // Regra aplicada
    PENDING_AUTH,  // Aguarda autorização
    IGNORED        // Ignorada (sem regra ou regra de ignorar)
}
