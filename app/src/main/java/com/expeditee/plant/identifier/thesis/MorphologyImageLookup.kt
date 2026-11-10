package com.expeditee.plant.identifier.thesis

import com.google.gson.JsonParser
import java.text.Normalizer
import java.util.Locale

enum class MorphologyPart {
    ROOT,
    STEM,
    LEAF,
    FLOWER,
    FRUIT,
}

data class MorphologyPartImages(
    val root: String? = null,
    val stem: String? = null,
    val leaf: String? = null,
    val flower: String? = null,
    val fruit: String? = null,
) {
    fun url(part: MorphologyPart): String? = when (part) {
        MorphologyPart.ROOT -> root
        MorphologyPart.STEM -> stem
        MorphologyPart.LEAF -> leaf
        MorphologyPart.FLOWER -> flower
        MorphologyPart.FRUIT -> fruit
    }

    fun with(part: MorphologyPart, url: String?): MorphologyPartImages = when (part) {
        MorphologyPart.ROOT -> copy(root = url)
        MorphologyPart.STEM -> copy(stem = url)
        MorphologyPart.LEAF -> copy(leaf = url)
        MorphologyPart.FLOWER -> copy(flower = url)
        MorphologyPart.FRUIT -> copy(fruit = url)
    }

    val isEmpty: Boolean
        get() = root.isNullOrBlank() &&
            stem.isNullOrBlank() &&
            leaf.isNullOrBlank() &&
            flower.isNullOrBlank() &&
            fruit.isNullOrBlank()
}

data class MorphologyCandidate(
    val title: String,
    val url: String,
    val mime: String? = null,
)

/**
 * Builds Commons search queries and ranks image candidates for a plant part.
 * Images are resolved by scientific name — not hardcoded per species.
 */
object MorphologyImageLookup {
    private val blockedTitle = listOf(
        "rust", "blight", "disease", "virus", "fungus", "infestation", "worm",
        "caterpillar", "butterfly", "moth", "insect", "bee", "wasp", "beetle", "fly",
        "dragonfly", "grasshopper", "cicada", "spider", "ant", "snail", "slug", "bird",
        "animal", "pollinator", "lesion", "necrosis", "herbarium sheet label only",
        "logo", "icon", "map of", "distribution map", "qr code", "barcode",
        "svg", "diagram of deposition", "epidermis print", "stomata",
        "micrograph", "sem ", "tem ", "x-ray",
        "helminthosporium", "pyricularia", "magnaporthe", "rhizoctonia",
        "cercospora", "alternaria", "fusarium", "puccinia",
    )

    private val partKeywords: Map<MorphologyPart, List<String>> = mapOf(
        MorphologyPart.ROOT to listOf("root", "roots", "rhizome", "tuber", "radicle"),
        MorphologyPart.STEM to listOf(
            "stem", "stems", "culm", "trunk", "bark", "cane", "stalk", "shoot", "straw",
        ),
        MorphologyPart.LEAF to listOf("leaf", "leaves", "foliage", "frond", "flag leaf"),
        MorphologyPart.FLOWER to listOf(
            "flower", "flowers", "blossom", "inflorescence", "spikelet", "floret",
        ),
        MorphologyPart.FRUIT to listOf(
            "fruit", "fruits", "berry", "grain", "seed", "seeds", "panicle", "ear", "cob",
            "kernel", "pod",
        ),
    )

    fun searchQueries(scientific: String, part: MorphologyPart): List<String> {
        val binomial = scientific.trim().replace(Regex("\\s+"), " ")
        if (binomial.isBlank()) return emptyList()
        val genus = binomial.substringBefore(" ").takeIf { it.length >= 3 }.orEmpty()
        val keywords = partKeywords[part].orEmpty()
        val primary = keywords.take(3).map { kw -> "$binomial $kw" }
        val titled = keywords.take(2).map { kw -> "intitle:\"$binomial\" $kw" }
        val genusQueries = if (genus.isNotBlank()) {
            keywords.take(2).map { "$genus $it" }
        } else {
            emptyList()
        }
        return (primary + titled + genusQueries).distinct()
    }

    fun parseCandidates(json: String?): List<MorphologyCandidate> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val pages = root.getAsJsonObject("query")?.getAsJsonObject("pages")
                ?: return emptyList()
            buildList {
                for ((_, pageEl) in pages.entrySet()) {
                    val page = pageEl.asJsonObject
                    val title = page.get("title")?.asString.orEmpty()
                    val infoArr = page.getAsJsonArray("imageinfo") ?: continue
                    if (infoArr.size() == 0) continue
                    val info = infoArr[0].asJsonObject
                    val mime = info.get("mime")?.asString.orEmpty()
                    if (mime.isNotBlank() && !mime.startsWith("image/")) continue
                    if (mime.contains("svg", ignoreCase = true)) continue
                    val url = info.get("thumburl")?.asString
                        ?.takeIf { it.isNotBlank() }
                        ?: info.get("url")?.asString.orEmpty().trim()
                    if (url.isBlank() || !url.startsWith("http")) continue
                    add(MorphologyCandidate(title = title, url = url, mime = mime))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun pickBest(
        scientific: String,
        part: MorphologyPart,
        candidates: List<MorphologyCandidate>,
    ): MorphologyCandidate? {
        if (candidates.isEmpty()) return null
        val binomial = fold(scientific)
        val genus = binomial.substringBefore(" ").takeIf { it.length >= 3 }
        val keywords = partKeywords[part].orEmpty().map(::fold)
        return candidates
            .map { it to score(it, binomial, genus, keywords) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    fun score(
        candidate: MorphologyCandidate,
        binomialFolded: String,
        genus: String?,
        partKeywordsFolded: List<String>,
    ): Int {
        val title = fold(candidate.title)
        if (blockedTitle.any { it in title }) return -100
        val hasPart = partKeywordsFolded.any { it in title }
        if (!hasPart) return 0
        var score = 0
        if (binomialFolded.isNotBlank() && binomialFolded in title) score += 12
        else if (genus != null && genus in title) score += 6
        else return 0
        score += partKeywordsFolded.count { it in title } * 5
        score += 3
        if ("close" in title || "detail" in title || "macro" in title) score += 1
        return score
    }

    fun fold(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(Locale.US), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
