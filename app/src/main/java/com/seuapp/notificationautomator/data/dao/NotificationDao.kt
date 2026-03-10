package com.seuapp.notificationautomator.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.seuapp.notificationautomator.data.model.Notification

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
}