package com.expeditee.plant.identifier.helper


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.expeditee.plant.identifier.model.SearchHistoryModel
import org.json.JSONArray
import org.json.JSONObject

class SearchHistoryManager(private val context: Context) {

    companion object {
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val PREFS_KEY = "plant_app_base"
        private const val MAX_HISTORY_SIZE = 20
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    /**
     * Add a search query to history
     */
    fun addSearchToHistory(query: String) {
        if (query.isEmpty()) return

        val history = getSearchHistory().toMutableList()

        // Remove if already exists (to avoid duplicates)
        history.removeAll { it.query.equals(query, ignoreCase = true) }

        // Add new item to beginning
        val newItem = SearchHistoryModel(
            id = System.currentTimeMillis(),
            query = query.trim(),
            timestamp = System.currentTimeMillis()
        )
        history.add(0, newItem)

        // Keep only MAX_HISTORY_SIZE items
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.lastIndex)
        }

        saveHistory(history)
    }

    /**
     * Get all search history
     */
    fun getSearchHistory(): List<SearchHistoryModel> {
        return try {
            val json = preferences.getString(KEY_SEARCH_HISTORY, "[]") ?: "[]"
            val jsonArray = JSONArray(json)
            val history = mutableListOf<SearchHistoryModel>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                history.add(
                    SearchHistoryModel(
                        id = obj.getLong("id"),
                        query = obj.getString("query"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            history
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Delete specific history item
     */
    fun deleteHistory(historyModel: SearchHistoryModel) {
        val history = getSearchHistory().toMutableList()
        history.removeAll { it.id == historyModel.id }
        saveHistory(history)
    }

    /**
     * Clear all history
     */
    fun clearAllHistory() {
        preferences.edit {
            putString(KEY_SEARCH_HISTORY, "[]")
        }
    }

    private fun saveHistory(history: List<SearchHistoryModel>) {
        try {
            val jsonArray = JSONArray()
            history.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("query", item.query)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            preferences.edit {
                putString(KEY_SEARCH_HISTORY, jsonArray.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}