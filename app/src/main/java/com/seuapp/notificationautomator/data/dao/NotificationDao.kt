package com.seuapp.notificationautomator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import com.seuapp.notificationautomator.data.model.WebhookStatus

@Dao
interface NotificationDao {
    
    @Insert
    suspend fun insert(notification: Notification): Long
    
    @Update
    suspend fun update(notification: Notification)
    
    @Delete
    suspend fun delete(notification: Notification)
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotifications(): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getNotificationsByStatus(status: String): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): Notification?
    
    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE packageName = :packageName AND title = :title AND text = :text AND timestamp > :cutoffTime")
    suspend fun getRecentNotificationCount(packageName: String, title: String?, text: String?, cutoffTime: Long): Int

    // 🆕 NOVO MÉTODO PARA ML
    @Query("""
        UPDATE notifications 
        SET category = :category, 
            categoryConfidence = :confidence 
        WHERE id = :id
    """)
    suspend fun updateCategory(id: Long, category: String?, confidence: Float?)

    @Query("""
    SELECT * FROM notifications 
    WHERE (category IS NULL OR category = '') 
    AND timestamp > :since
    ORDER BY timestamp DESC
    """)
    suspend fun getUnclassifiedRecentNotifications(since: Long): List<Notification>

    // ===== MÉTODOS DE PAGINAÇÃO (USANDO STRINGS PARA STATUS) =====
    
    @Query("SELECT * FROM notifications WHERE isHidden = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getVisibleNotificationsPaginated(limit: Int, offset: Int): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE status = :status AND isHidden = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByStatusPaginated(status: String, limit: Int, offset: Int): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE webhookStatus = :webhookStatus AND isHidden = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByWebhookStatusPaginated(webhookStatus: String, limit: Int, offset: Int): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE isHidden = 1 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getHiddenNotificationsPaginated(limit: Int, offset: Int): List<Notification>
    
    @Query("SELECT * FROM notifications WHERE status IN (:statuses) AND isHidden = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByMultipleStatusesPaginated(statuses: List<String>, limit: Int, offset: Int): List<Notification>
    
    // ===== MÉTODOS PARA CONTAGENS =====
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isHidden = 0")
    suspend fun getVisibleCount(): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE status = :status AND isHidden = 0")
    suspend fun getCountByStatus(status: String): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE webhookStatus = :webhookStatus AND isHidden = 0")
    suspend fun getCountByWebhookStatus(webhookStatus: String): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isHidden = 1")
    suspend fun getHiddenCount(): Int
    
    @Query("SELECT COUNT(*) FROM notifications WHERE status IN (:statuses) AND isHidden = 0")
    suspend fun getCountByMultipleStatuses(statuses: List<String>): Int
    
    // ===== MÉTODO PARA OCULTAR NOTIFICAÇÕES SIMILARES =====
    
    @Query("UPDATE notifications SET isHidden = 1 WHERE packageName = :packageName AND title = :title AND isHidden = 0")
    suspend fun hideSimilarNotifications(packageName: String, title: String?)
    
    // ===== MÉTODOS PARA LIMPAR POR FILTRO =====
    
    @Query("DELETE FROM notifications WHERE isHidden = 0")
    suspend fun deleteAllVisible()
    
    @Query("DELETE FROM notifications WHERE status = :status AND isHidden = 0")
    suspend fun deleteByStatus(status: String)
    
    @Query("UPDATE notifications SET isHidden = 0 WHERE packageName = :packageName AND title = :title AND isHidden = 1")
    suspend fun mostrarNotificacoesIguais(packageName: String, title: String?)


    @Query("DELETE FROM notifications WHERE webhookStatus = :webhookStatus AND isHidden = 0")
    suspend fun deleteByWebhookStatus(webhookStatus: String)
    
    @Query("DELETE FROM notifications WHERE isHidden = 1")
    suspend fun deleteAllHidden()
}