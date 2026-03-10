package com.seuapp.notificationautomator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.seuapp.notificationautomator.data.database.Converters

@Entity(tableName = "rules")
@TypeConverters(Converters::class)
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    
    // Condições (todas em AND)
    val appPackage: String? = null,              // package da app (ex: com.google.android.gm)
    val titleContains: String? = null,            // texto que o título deve conter
    val textContains: String? = null,              // texto que o conteúdo deve conter
    val hourFrom: String? = null,                  // "09:00"
    val hourTo: String? = null,                    // "18:00"
    val daysOfWeek: String? = null,                 // "MONDAY,TUESDAY,WEDNESDAY"
    val isSilent: Boolean? = null,                  // true = silenciosa, false = alertante, null = qualquer
    val hasImage: Boolean? = null,                   // true = com imagem, false = sem imagem, null = qualquer
    
    // Ação
    val actionType: ActionType,
    val webhookUrl: String? = null,
    val webhookHeaders: String? = null,              // JSON com headers
    
    // Metadados
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null,
    val triggerCount: Int = 0
)

enum class ActionType {
    WEBHOOK_AUTO,           // Envia sem confirmação
    WEBHOOK_AUTH            // Envia só após autorização
}