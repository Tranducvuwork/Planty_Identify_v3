package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.CitationData
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer

data class CareVideoHit(
    val videoId: String,
    val title: String,
)

object CareVideoLookup {
    const val LOG_TAG = "CareVideo"

    private val idRegex = Regex("^[A-Za-z0-9_-]{11}$")
    private val fromUrl = Regex("(?:v=|/embed/|/shorts/|youtu\\.be/)([A-Za-z0-9_-]{11})")
    private val hitRegex = Regex(
        "\"(?:videoRenderer|compactVideoRenderer)\"\\s*:\\s*\\{\\s*\"videoId\"\\s*:\\s*\"([A-Za-z0-9_-]{11})\".{0,3500}?\"(?:text|simpleText)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val unicodeEscape = Regex("\\\\u([0-9a-fA-F]{4})")

    private val blockedIds = setOf(
        "dQw4w9WgXcQ",
        "oHg5SJYRHA0",
        "iik25wqIuFo",
        "xvFZjo5PgG0",
        "QB7ACr7pUuE",
        "j5a0jTc9S10",
        "lpiB2wGeRGQ",
        "bj5OdPuWNbM",
    )

    private val stopwords = setOf(
        "cach", "trong", "cham", "soc", "cay", "video", "va", "cua", "the",
        "and", "how", "to", "plant", "care", "huong", "dan", "tai", "nha",
    )

    fun parseVideoId(raw: String?): String? {
        val text = raw.orEmpty().trim().removeSurrounding("\"").removeSurrounding("'")
        if (text.isBlank()) return null
        fromUrl.find(text)?.groupValues?.getOrNull(1)?.let { return trusted(it) }
        val token = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
        return token.takeIf { idRegex.matches(it) }?.let(::trusted)
    }

    fun parseSearchHits(html: String?): List<CareVideoHit> {
        if (html.isNullOrBlank()) return emptyList()
        val normalized = unescape(html)
        return hitRegex.findAll(normalized).mapNotNull { match ->
            val id = trusted(match.groupValues[1]) ?: return@mapNotNull null
            val title = unescape(match.groupValues[2]).trim()
            if (title.isBlank() || isRickrollTitle(title)) return@mapNotNull null
            CareVideoHit(id, title)
        }.distinctBy { it.videoId }.toList()
    }

    fun pickForPlant(name: String, scientific: String, hits: List<CareVideoHit>): CareVideoHit? {
        val resolvedScientific = resolveScientificName(scientific, name)
        val tokens = plantTokens(name, resolvedScientific)
        val genus = resolvedScientific.trim().substringBefore(" ").let(::fold).takeIf { it.length >= 4 }
        return hits.maxByOrNull { hit ->
            val title = fold(hit.title)
            var score = 0
            if (genus != null && genus in title) score += 10
            score += tokens.count { token -> token in title } * 4
            if ("cham soc" in title || "cach trong" in title) score += 3
            if ("mai vang" in title && tokens.none { it == "vang" }) score -= 8
            if ("nhat chi mai" in title && tokens.none { it.contains("nhat") }) score -= 6
            misleadingPlantTitles.forEach { wrong ->
                if (wrong in title && tokens.none { token -> token in wrong || wrong.contains(token) }) {
                    score -= 12
                }
            }
            score
        }?.takeIf { hit ->
            val title = fold(hit.title)
            val genusHit = genus != null && genus in title
            val tokenHit = tokens.any { it in title }
            val phraseHit = vietnamesePhrases(name).any { it in title }
            genusHit || tokenHit || phraseHit
        }
    }

    fun resolveScientificName(scientific: String, plantName: String): String {
        val blob = fold("$scientific $plantName")
        return when {
            blob.contains("eclipta") || blob.contains("herba ecliptae") ||
                blob.contains("nho noi") || blob.contains("co muc") -> "Eclipta prostrata"
            blob.contains("portulaca") || blob.contains("rau sam") -> "Portulaca oleracea"
            blob.contains("oryza") || blob.contains("lua") -> "Oryza sativa"
            blob.contains("zea mays") || blob.contains("ngo") || blob.contains("bap") -> "Zea mays"
            blob.contains("malus") || blob.contains("tao") -> "Malus domestica"
            else -> scientific.trim().ifBlank { plantName.trim() }
        }
    }

    private val misleadingPlantTitles = listOf(
        "co kieng", "coc kieng", "cay kieng", "cay canh", "cay canh van phong",
        "monstera", "phoenix", "cau", "dua canh",
    )

    private fun vietnamesePhrases(plantName: String): List<String> {
        val f = fold(plantName)
        return buildList {
            if ("nho noi" in f) add("nho noi")
            if ("co muc" in f) add("co muc")
            if ("rau sam" in f) add("rau sam")
            if ("lua" in f) add("lua")
            if ("ngo" in f || "bap" in f) add("ngo")
        }
    }

    fun isRickrollTitle(title: String): Boolean {
        val t = fold(title)
        return "rickroll" in t ||
            "rick roll" in t ||
            "rick astley" in t ||
            "never gonna" in t ||
            "give you up" in t
    }

    fun watchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun searchQuery(name: String, scientific: String): String {
        val resolvedScientific = resolveScientificName(scientific, name)
        val videoName = listOf(name, resolvedScientific)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        return "cách trồng chăm sóc $videoName"
    }

    fun searchUrl(name: String, scientific: String): String {
        val encoded = URLEncoder.encode(searchQuery(name, scientific), StandardCharsets.UTF_8.name())
        return "https://www.youtube.com/results?search_query=$encoded"
    }

    fun isYoutube(cite: CitationData): Boolean {
        val url = cite.url.orEmpty()
        return cite.type.equals("youtube", ignoreCase = true) ||
            url.contains("youtube", ignoreCase = true) ||
            url.contains("youtu.be", ignoreCase = true) ||
            !cite.videoId.isNullOrBlank()
    }

    fun bestExisting(citations: List<CitationData>): CitationData? {
        val youtube = citations.filter { isYoutube(it) }
        val watch = youtube.firstOrNull {
            parseVideoId(it.videoId) != null || parseVideoId(it.url) != null
        } ?: return youtube.firstOrNull()
        val id = parseVideoId(watch.videoId) ?: parseVideoId(watch.url) ?: return watch
        return watch.copy(videoId = id, url = watchUrl(id))
    }

    fun trusted(id: String): String? =
        id.takeIf { idRegex.matches(it) && !blockedIds.contains(it) }

    fun plantTokens(name: String, scientific: String): List<String> {
        val genus = scientific.trim().substringBefore(" ").let(::fold).takeIf { it.length >= 4 }
        val fromName = fold(name)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in stopwords }
        val fromScientific = fold(scientific)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 && it !in stopwords }
        return (fromName + listOfNotNull(genus) + fromScientific).distinct()
    }

    private fun unescape(text: String): String {
        val unicode = unicodeEscape.replace(text) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            code.toChar().toString()
        }
        return unicode
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
    }

    private fun fold(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "").replace('đ', 'd').trim()
    }
}
