package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.network.model.FolkRemedyData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.text.Normalizer

object SymptomRemedyCatalog {
    private val gson = Gson()
    private var cache: SymptomRemedyRoot? = null

    fun root(context: Context): SymptomRemedyRoot {
        cache?.let { return it }
        val loaded = try {
            context.assets.open("symptom_remedies.json").bufferedReader().use { reader ->
                gson.fromJson(reader, SymptomRemedyRoot::class.java)
            }
        } catch (_: Exception) {
            null
        } ?: SymptomRemedyRoot()
        cache = loaded
        return loaded
    }

    fun disclaimer(context: Context): String =
        root(context).disclaimer.takeIf { it.isNotBlank() }
            ?: "Đây là dự đoán theo triệu chứng, tốt nhất người bệnh nên đến BS y học cổ truyền hoặc chuyên khoa để thăm khám tốt hơn."

    fun matchIntent(context: Context, query: String): SymptomIntent? {
        return rankedIntents(context, query)
            .firstOrNull { (_, score) -> score >= 100 }
            ?.first
    }

    fun suggestions(context: Context, query: String, limit: Int = 6): List<SymptomIntent> {
        val ranked = rankedIntents(context, query)
        return ranked
            .filter { (_, score) -> score > 0 }
            .map { (intent, _) -> intent }
            .take(limit)
            .ifEmpty { root(context).intents.take(limit) }
    }

    fun intentById(context: Context, id: String): SymptomIntent? =
        root(context).intents.firstOrNull { it.id == id }

    fun remediesFor(context: Context, intent: SymptomIntent): List<FolkRemedyData> {
        val allFolk = loadFolk(context)
        val byId = allFolk.associateBy { it.id.orEmpty() }
        return intent.remedyIds.mapNotNull { byId[it] }
    }

    fun allIntents(context: Context): List<SymptomIntent> = root(context).intents

    private fun loadFolk(context: Context): List<FolkRemedyData> {
        return try {
            context.assets.open("folk_remedies.json").bufferedReader().use { reader ->
                val type = object : TypeToken<List<FolkRemedyData>>() {}.type
                gson.fromJson<List<FolkRemedyData>>(reader, type).orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun rankedIntents(context: Context, query: String): List<Pair<SymptomIntent, Int>> {
        val folded = fold(query)
        if (folded.length < 2) return emptyList()
        val queryTokens = symptomTokens(folded)
        return root(context).intents
            .map { intent -> intent to scoreIntent(intent, folded, queryTokens) }
            .sortedWith(compareByDescending<Pair<SymptomIntent, Int>> { it.second }.thenBy { it.first.title })
    }

    private fun scoreIntent(intent: SymptomIntent, foldedQuery: String, queryTokens: Set<String>): Int {
        val candidates = buildList {
            add(intent.title)
            addAll(intent.triggers)
            addAll(intent.questions)
            addAll(intent.questionHints)
        }.map(::fold).filter { it.isNotBlank() }

        var best = 0
        candidates.forEach { candidate ->
            val candidateTokens = symptomTokens(candidate)
            val overlap = queryTokens.intersect(candidateTokens).size
            val tokenScore = if (queryTokens.isNotEmpty()) {
                (overlap * 100) / queryTokens.size
            } else {
                0
            }
            best = maxOf(
                best,
                when {
                    foldedQuery == candidate -> 120
                    candidate.length >= 2 && foldedQuery.contains(candidate) -> 100
                    foldedQuery.length >= 3 && candidate.contains(foldedQuery) -> 82
                    overlap > 0 -> 42 + tokenScore
                    else -> 0
                },
            )
        }
        return best
    }

    private fun symptomTokens(value: String): Set<String> {
        val stopWords = setOf(
            "toi", "minh", "em", "bi", "muon", "tim", "kiem", "hoi", "ve", "cach",
            "chua", "benh", "trieu", "chung", "mot", "so", "co", "khong", "la", "gi",
            "hay", "cho", "biet", "xem", "goi", "y", "dan", "gian",
        )
        return value.split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
    }

    private fun fold(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(raw.lowercase().trim(), Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace(Regex("[^a-z0-9\\s-]"), " ")
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

data class SymptomRemedyRoot(
    @SerializedName("disclaimer") val disclaimer: String = "",
    @SerializedName("intents") val intents: List<SymptomIntent> = emptyList(),
)

data class SymptomIntent(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("triggers") val triggers: List<String> = emptyList(),
    @SerializedName("questions") val questions: List<String> = emptyList(),
    @SerializedName("questionHints") val questionHints: List<String> = emptyList(),
    @SerializedName("remedyIds") val remedyIds: List<String> = emptyList(),
)
