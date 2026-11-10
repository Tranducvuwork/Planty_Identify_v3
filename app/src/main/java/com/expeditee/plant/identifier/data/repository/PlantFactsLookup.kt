package com.expeditee.plant.identifier.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PlantFacts(
    @SerializedName("description") val description: String? = null,
    @SerializedName("uses") val uses: List<String> = emptyList(),
    @SerializedName("origin") val origin: String? = null,
    @SerializedName("planting") val planting: String? = null,
    @SerializedName("care") val care: String? = null,
)

object PlantFactsLookup {
    private val gson = Gson()

    private val genericMarkers = listOf(
        "học sinh",
        "hs làm vườn",
        "phù hợp hs",
        "phù hợp học sinh",
        "nhật ký",
        "quan sát sinh trưởng",
        "trồng cảnh, quan sát",
        "phù hợp bài học",
        "sân trường",
        "làm nhật ký học sinh",
        "được nhận diện",
        "tham khảo cách chăm sóc",
        "dược liệu thường gặp",
        "hướng dẫn trồng, chăm sóc",
    )

    fun looksGeneric(text: String?): Boolean {
        val value = text?.lowercase().orEmpty()
        if (value.isBlank()) return false
        return genericMarkers.any { it in value }
    }

    fun needsFacts(uses: List<String>, description: String?): Boolean {
        if (uses.isEmpty() && description.isNullOrBlank()) return true
        if (uses.any { looksGeneric(it) } || looksGeneric(description)) return true
        val thinUses = uses.size <= 2 && uses.all { it.trim().length <= 18 }
        val thinDesc = description.isNullOrBlank() || description.trim().length < 48
        return thinUses && thinDesc
    }

    fun parseFacts(raw: String?): PlantFacts? {
        val json = extractJson(raw) ?: return null
        val facts = runCatching { gson.fromJson(json, PlantFacts::class.java) }.getOrNull() ?: return null
        val uses = facts.uses.map { it.trim() }.filter { it.isNotBlank() && !looksGeneric(it) }
        val description = facts.description?.trim()?.takeIf { it.isNotBlank() && !looksGeneric(it) }
        if (description.isNullOrBlank() && uses.isEmpty()) return null
        return facts.copy(description = description, uses = uses)
    }

    fun prompt(scientificName: String, commonName: String): String {
        return """
            Bạn là chuyên gia thực vật. Trả về JSON thuần, không markdown.
            Tên khoa học: $scientificName
            Tên Việt: $commonName
            Viết tiếng Việt chính xác như Wikipedia/Google.
            Không viết "học sinh", "HS làm vườn", "nhật ký".
            Công dụng thật: thực phẩm, thuốc, cảnh, gỗ, gia vị, bóng mát...
            Schema:
            {"description":"...","uses":["...","..."],"origin":"...","planting":"...","care":"..."}
        """.trimIndent()
    }

    private fun extractJson(raw: String?): String? {
        val text = raw.orEmpty().trim()
        if (text.isEmpty()) return null
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }
}
