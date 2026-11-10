package com.expeditee.plant.identifier.data.converter


import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list.isNullOrEmpty()) null else gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}