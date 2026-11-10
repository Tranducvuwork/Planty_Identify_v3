package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.data.repository.PlantUserOverride
import com.expeditee.plant.identifier.network.model.FolkRemedyData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class DiaryEntry(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("title") val title: String,
    @SerializedName("plantName") val plantName: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("specialties") val specialties: String = "",
    @SerializedName("coverNote") val coverNote: String = "",
    @SerializedName("timeline") val timeline: List<DiaryTimelineEvent> = emptyList(),
)

data class DiaryTimelineEvent(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String = "",
    @SerializedName("photoUri") val photoUri: String = "",
)

data class ScanHistoryItem(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("commonName") val commonName: String,
    @SerializedName("scientificName") val scientificName: String = "",
    @SerializedName("imagePath") val imagePath: String = "",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("favorite") val favorite: Boolean = false,
    @SerializedName("plantGroup") val plantGroup: String = "",
)

class ThesisLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("thesis_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun listDiaries(): List<DiaryEntry> {
        val raw = prefs.getString(KEY_DIARIES, "[]") ?: "[]"
        val type = object : TypeToken<List<DiaryEntry>>() {}.type
        return gson.fromJson(raw, type) ?: emptyList()
    }

    fun saveDiaries(list: List<DiaryEntry>) {
        prefs.edit().putString(KEY_DIARIES, gson.toJson(list)).apply()
    }

    fun getDiary(id: String): DiaryEntry? = listDiaries().firstOrNull { it.id == id }

    fun upsertDiary(entry: DiaryEntry) {
        val next = listDiaries().filterNot { it.id == entry.id } + entry
        saveDiaries(next.sortedByDescending { it.startDate })
    }

    fun addEvent(diaryId: String, event: DiaryTimelineEvent): DiaryEntry? {
        val diary = getDiary(diaryId) ?: return null
        val updated = diary.copy(
            timeline = (diary.timeline + event).sortedByDescending { it.eventDate },
        )
        upsertDiary(updated)
        return updated
    }

    fun listHistory(): List<ScanHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<List<ScanHistoryItem>>() {}.type
        val list: List<ScanHistoryItem> = gson.fromJson(raw, type) ?: emptyList()
        // Chỉ giữ 1 bản ghi / tên cây (ưu tiên mới nhất)
        val unique = linkedMapOf<String, ScanHistoryItem>()
        list.sortedByDescending { it.timestamp }.forEach { item ->
            val key = item.commonName.trim().lowercase().ifBlank {
                item.scientificName.trim().lowercase()
            }
            if (key.isNotBlank() && !unique.containsKey(key)) {
                unique[key] = item
            }
        }
        val cleaned = unique.values.toList()
        if (cleaned.size != list.size) saveHistory(cleaned)
        return cleaned
    }

    private fun saveHistory(list: List<ScanHistoryItem>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list.take(100))).commit()
    }

    /** Thêm mới hoặc cập nhật lần scan mới nhất lên đầu danh sách. */
    fun addHistory(item: ScanHistoryItem): Boolean {
        val existing = listHistory()
        val duplicate = existing.firstOrNull { samePlantName(it, item) }
        val normalized = duplicate?.let {
            item.copy(
                id = it.id,
                favorite = it.favorite,
                timestamp = System.currentTimeMillis(),
            )
        } ?: item.copy(timestamp = System.currentTimeMillis())
        saveHistory(listOf(normalized) + existing.filterNot { samePlantName(it, item) })
        return true
    }

    fun findHistoryByName(commonName: String, scientificName: String): ScanHistoryItem? {
        return listHistory().firstOrNull {
            samePlantName(
                it,
                ScanHistoryItem(commonName = commonName, scientificName = scientificName),
            )
        }
    }

    private fun samePlantName(a: ScanHistoryItem, b: ScanHistoryItem): Boolean {
        val aCommon = a.commonName.trim().lowercase()
        val bCommon = b.commonName.trim().lowercase()
        val aSci = a.scientificName.trim().lowercase()
        val bSci = b.scientificName.trim().lowercase()
        if (aCommon.isNotBlank() && aCommon == bCommon) return true
        if (aSci.isNotBlank() && aSci == bSci) return true
        return false
    }

    fun toggleFavorite(id: String): ScanHistoryItem? {
        val list = listHistory()
        val updated = list.map {
            if (it.id == id) it.copy(favorite = !it.favorite) else it
        }
        saveHistory(updated)
        return updated.firstOrNull { it.id == id }
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    fun listFavorites(): List<ScanHistoryItem> = listHistory().filter { it.favorite }

    fun listOverrides(): List<PlantUserOverride> {
        val raw = prefs.getString(KEY_OVERRIDES, "[]") ?: "[]"
        val type = object : TypeToken<List<PlantUserOverride>>() {}.type
        return gson.fromJson(raw, type) ?: emptyList()
    }

    fun findOverride(scientificName: String, commonName: String = ""): PlantUserOverride? {
        val sci = scientificName.trim().lowercase()
        val common = commonName.trim().lowercase()
        return listOverrides().firstOrNull {
            val itSci = it.scientificName.trim().lowercase()
            val itCommon = it.commonNames.firstOrNull().orEmpty().trim().lowercase()
            (sci.isNotBlank() && itSci == sci) ||
                (common.isNotBlank() && itCommon == common)
        }
    }

    fun saveOverride(override: PlantUserOverride) {
        val sci = override.scientificName.trim().lowercase()
        val next = listOverrides().filterNot {
            it.scientificName.trim().lowercase() == sci
        } + override
        prefs.edit().putString(KEY_OVERRIDES, gson.toJson(next)).apply()
    }

    fun applyOverride(result: PlantScanResult): PlantScanResult {
        val override = findOverride(
            result.scientificName,
            result.commonNames.firstOrNull().orEmpty(),
        ) ?: return result
        return override.applyTo(result)
    }

    fun loadFolkFromAssets(context: Context): List<FolkRemedyData> {
        return try {
            context.assets.open("folk_remedies.json").bufferedReader().use { reader ->
                val type = object : TypeToken<List<FolkRemedyData>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_DIARIES = "diaries"
        private const val KEY_HISTORY = "scan_history"
        private const val KEY_OVERRIDES = "plant_overrides"
    }
}
