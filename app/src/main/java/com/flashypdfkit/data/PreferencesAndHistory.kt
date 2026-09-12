package com.flashypdfkit.data

import android.content.Context
import android.content.SharedPreferences
import com.flashypdfkit.model.AppTheme
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.model.ProcessedFile
import com.flashypdfkit.model.UserSettings
import org.json.JSONArray
import org.json.JSONObject

class PreferencesAndHistory(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flashypdf_prefs", Context.MODE_PRIVATE)

    fun getUserSettings(): UserSettings {
        val isPremium = prefs.getBoolean("is_premium", false)
        val themeStr = prefs.getString("theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
        val theme = try { AppTheme.valueOf(themeStr) } catch (e: Exception) { AppTheme.DARK }
        val completedOb = prefs.getBoolean("completed_onboarding", false)

        return UserSettings(
            isPremium = isPremium,
            theme = theme,
            hasCompletedOnboarding = completedOb
        )
    }

    fun setPremium(isPremium: Boolean) {
        prefs.edit().putBoolean("is_premium", isPremium).apply()
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
    }

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean("completed_onboarding", completed).apply()
    }

    fun addRecentFile(file: ProcessedFile) {
        val current = getRecentFiles().toMutableList()
        current.removeAll { it.path == file.path }
        current.add(0, file)
        saveRecentFiles(current.take(20))
    }

    fun removeRecentFile(fileId: String) {
        val current = getRecentFiles().toMutableList()
        current.removeAll { it.id == fileId }
        saveRecentFiles(current)
    }

    fun clearRecentFiles() {
        prefs.edit().remove("recent_files_json").apply()
    }

    fun getRecentFiles(): List<ProcessedFile> {
        val jsonStr = prefs.getString("recent_files_json", null) ?: return emptyList()
        val result = mutableListOf<ProcessedFile>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val toolName = obj.optString("toolType", PdfToolType.READ.name)
                val toolType = try { PdfToolType.valueOf(toolName) } catch (e: Exception) { PdfToolType.READ }

                result.add(
                    ProcessedFile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        path = obj.getString("path"),
                        uriString = obj.optString("uriString", ""),
                        sizeBytes = obj.getLong("sizeBytes"),
                        pageCount = obj.optInt("pageCount", 0),
                        timestamp = obj.getLong("timestamp"),
                        toolType = toolType
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun saveRecentFiles(list: List<ProcessedFile>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("path", item.path)
                put("uriString", item.uriString)
                put("sizeBytes", item.sizeBytes)
                put("pageCount", item.pageCount)
                put("timestamp", item.timestamp)
                put("toolType", item.toolType.name)
            }
            array.put(obj)
        }
        prefs.edit().putString("recent_files_json", array.toString()).apply()
    }

    fun isFirstTimeForTool(toolKey: String): Boolean {
        val isFirst = prefs.getBoolean("first_time_tool_$toolKey", true)
        if (isFirst) {
            prefs.edit().putBoolean("first_time_tool_$toolKey", false).apply()
        }
        return isFirst
    }
}
