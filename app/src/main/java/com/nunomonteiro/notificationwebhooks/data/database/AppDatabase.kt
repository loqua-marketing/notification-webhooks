package com.nunomonteiro.notificationwebhooks.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.nunomonteiro.notificationwebhooks.data.dao.NotificationDao
import com.nunomonteiro.notificationwebhooks.data.dao.RuleDao
import com.nunomonteiro.notificationwebhooks.data.model.Notification
import com.nunomonteiro.notificationwebhooks.data.model.Rule

@Database(
    entities = [Notification::class, Rule::class],
    version = 5,  // <-- ALTERADO DE 4 PARA 5
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
                .fallbackToDestructiveMigration()  // Isto vai recriar a BD automaticamente
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}