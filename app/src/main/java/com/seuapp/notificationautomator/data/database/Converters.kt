// app/src/main/java/com/seuapp/notificationautomator/data/database/Converters.kt

package com.seuapp.notificationautomator.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringMap(string: String?): Map<String, String>? {
        if (string.isNullOrBlank()) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(string, type)
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(string: String?): List<String>? {
        if (string.isNullOrBlank()) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(string, type)
    }
}