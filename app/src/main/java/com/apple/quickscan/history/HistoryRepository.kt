package com.apple.quickscan.history

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("quickscan_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyHistory = "key_history_items"

    fun getAllItems(): List<ScanHistoryItem> {
        val json = prefs.getString(keyHistory, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScanHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addItem(item: ScanHistoryItem) {
        val current = getAllItems().toMutableList()
        // Deduplicate recent identical scan if within 3 seconds
        if (current.isNotEmpty() && current.first().content == item.content && (System.currentTimeMillis() - current.first().timestamp < 3000)) {
            return
        }
        current.add(0, item)
        // Keep max 200 items
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        saveList(current)
    }

    fun removeItem(id: String) {
        val current = getAllItems().toMutableList()
        current.removeAll { it.id == id }
        saveList(current)
    }

    fun clearAll() {
        prefs.edit().remove(keyHistory).apply()
    }

    private fun saveList(items: List<ScanHistoryItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(keyHistory, json).apply()
    }
}
