package com.seuapp.notificationautomator.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.seuapp.notificationautomator.data.dao.NotificationDao
import com.seuapp.notificationautomator.data.dao.RuleDao
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.Rule

@Database(
    entities = [Notification::class, Rule::class],
    version = 4,  // AUMENTAR PARA 4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun notificationDao(): NotificationDao
    abstract fun ruleDao(): RuleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_database"
                )
                .fallbackToDestructiveMigration()  // Forçar recriação em caso de migração
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
