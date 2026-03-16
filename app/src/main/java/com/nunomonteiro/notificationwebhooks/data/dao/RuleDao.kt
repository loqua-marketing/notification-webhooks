package com.nunomonteiro.notificationwebhooks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nunomonteiro.notificationwebhooks.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    
    @Query("SELECT * FROM rules ORDER BY isActive DESC, name ASC")
    fun getAllRules(): Flow<List<Rule>>
    
    @Query("SELECT * FROM rules WHERE isActive = 1")
    fun getActiveRules(): Flow<List<Rule>>
    
    @Query("SELECT * FROM rules WHERE isActive = 0")
    fun getInactiveRules(): Flow<List<Rule>>
    
    @Query("SELECT * FROM rules WHERE isActive = 1")
    suspend fun getActiveRulesSync(): List<Rule>
    
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): Rule?
    
    @Insert
    suspend fun insert(rule: Rule): Long
    
    @Update
    suspend fun update(rule: Rule)
    
    @Delete
    suspend fun delete(rule: Rule)
    
    @Query("UPDATE rules SET triggerCount = triggerCount + 1, lastTriggered = :timestamp WHERE id = :id")
    suspend fun incrementTriggerCount(id: Long, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM rules WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM rules")
    suspend fun getAllRulesSync(): List<Rule>
}