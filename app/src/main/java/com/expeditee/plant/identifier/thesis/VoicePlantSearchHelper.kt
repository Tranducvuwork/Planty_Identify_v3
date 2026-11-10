package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.network.model.PlantScanData
import java.text.Normalizer

object VoicePlantSearchHelper {
    private val commandWords = listOf(
        "cho toi biet",
        "tim kiem",
        "thong tin",
        "ve cay",
        "cay",
        "cong dung",
        "tac dung",
        "cach cham soc",
        "cham soc",
        "cach trong",
        "trong duoc",
        "sau benh",
        "phan bo",
        "o dau",
        "co nhung loai nao",
        "co the dung",
        "lam thuoc",
        "khong",
        "nhu the nao",
        "la gi",
        "gi",
        "nay",
        "no",
        "toi muon",
        "hay",
    )

    fun extractPlantQuery(spokenText: String, fallbackPlantName: String? = null): String {
        val raw = spokenText.trim()
        if (raw.isBlank()) return fallbackPlantName.orEmpty()
        val normalized = normalize(raw)
        val fallback = fallbackPlantName.orEmpty().trim()
        if ((normalized.contains("cay nay") || normalized == "no" || normalized.contains(" no ")) &&
            fallback.isNotBlank()
        ) {
            return fallback
        }
        var cleaned = normalized
        commandWords.sortedByDescending { it.length }.forEach { word ->
            cleaned = cleaned.replace("\\b${Regex.escape(word)}\\b".toRegex(), " ")
        }
        cleaned = cleaned.replace("[?.,!;:]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        return cleaned.ifBlank { raw }
    }

    fun findLocalPlant(context: Context, spokenText: String): PlantScanData? {
        return findLocalPlant(context, listOf(spokenText))
    }

    fun findLocalPlant(context: Context, spokenTexts: List<String>): PlantScanData? {
        val candidates = spokenTexts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (candidates.isEmpty()) return null

        return ThesisPlantCatalog.all(context)
            .map { plant -> plant to scorePlant(plant, candidates) }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.takeIf { (_, score) -> score >= 82 }
            ?.first
    }

    private fun scorePlant(plant: PlantScanData, spokenTexts: List<String>): Int {
        var best = 0
        spokenTexts.forEachIndexed { index, spokenText ->
            val query = normalize(extractPlantQuery(spokenText))
            val spoken = normalize(spokenText)
            if (query.isNotBlank() || spoken.isNotBlank()) {
                best = maxOf(best, scorePlantCandidate(plant, query, spoken, index))
            }
        }
        return best
    }

    private fun scorePlantCandidate(plant: PlantScanData, query: String, spoken: String, candidateIndex: Int): Int {
        val names = buildList {
            plant.scientificName?.let(::add)
            plant.commonNames.orEmpty().forEach(::add)
            plant.plantId?.replace("-", " ")?.let(::add)
            plant.qrCode?.replace("-", " ")?.let(::add)
            aliasesFor(plant).forEach(::add)
        }.map(::normalize).filter { it.isNotBlank() }.distinct()

        var best = 0
        val queryTokens = query.split(" ").filter { it.length >= 2 }.toSet()
        names.forEach { name ->
            best = maxOf(
                best,
                when {
                    query == name -> 130
                    spoken == name -> 124
                    query.isNotBlank() && name.contains(query) && query.length >= 3 -> 104
                    query.isNotBlank() && query.contains(name) && name.length >= 3 -> 100
                    spoken.contains(name) && name.length >= 3 -> 94
                    queryTokens.isNotEmpty() &&
                        name.split(" ").filter { it.length >= 2 }.toSet().containsAll(queryTokens) -> 88
                    else -> 0
                },
            )
        }
        val confidencePenalty = candidateIndex * 4
        return (best - confidencePenalty).coerceAtLeast(0)
    }

    private fun aliasesFor(plant: PlantScanData): List<String> {
        val key = normalize(
            buildString {
                append(plant.plantId.orEmpty()).append(' ')
                append(plant.qrCode.orEmpty()).append(' ')
                append(plant.scientificName.orEmpty()).append(' ')
                plant.commonNames.orEmpty().forEach { append(it).append(' ') }
            },
        )
        return when {
            key.contains("long nhan") || key.contains("dimocarpus") || key.contains("longan") ->
                listOf("Nhãn", "Cây nhãn", "Quả nhãn", "Long nhãn")
            key == "toi" || key.contains("allium sativum") || key.contains("garlic") ->
                listOf("Cây tỏi", "Củ tỏi")
            key.contains("trung ca") ||
                key.contains("muntingia") ||
                key.contains("jamaican cherry") ||
                key.contains("calabura") ->
                listOf("Trứng cá", "Cây trứng cá", "Jamaican cherry", "Calabura")
            key.contains("cay bang") || key.contains("terminalia") ->
                listOf("Cây bàng", "Bàng", "Terminalia catappa")
            key.contains("phuong vi") || key.contains("delonix") ->
                listOf("Phượng vĩ", "Phượng", "Delonix regia")
            key.contains("hoa giay") || key.contains("bougainvillea") ->
                listOf("Hoa giấy", "Bông giấy", "Bougainvillea glabra")
            key.contains("la giang") || key.contains("gnetum") || key.contains("cay giang") ->
                listOf("Lá giang", "Gnetum gnemon", "Melinjo")
            key.contains("ixora") || key.contains("bong trang") ->
                listOf("Bông trang", "Ixora coccinea", "Trang")
            key.contains("dam but") || key.contains("hibiscus rosa") ->
                listOf("Dâm bụt", "Hibiscus rosa-sinensis", "Bông bụt")
            key.contains("bau bi") || key.contains("lagenaria") ->
                listOf("Bầu", "Bầu bí", "Lagenaria siceraria")
            key.contains("mat gau") || key.contains("andrographis") ->
                listOf("Cây mật gấu", "Mật gấu", "Xuyên tâm liên", "Andrographis paniculata")
            key.contains("bo cong anh") || key.contains("taraxacum") ->
                listOf("Bồ công anh", "Cúc dại", "Taraxacum officinale", "Dandelion")
            key.contains("kim ngan hoa") || key.contains("lonicera japonica") ->
                listOf("Kim ngân hoa", "Lonicera japonica", "Honeysuckle")
            key.contains("hoan ngoc") || key.contains("hedyotis") ->
                listOf("Hoàn ngọc", "Hedyotis diffusa", "Oldenlandia diffusa")
            key.contains("xuong khop") || key.contains("orthosiphon") || key.contains("rau meo") ->
                listOf("Trà xương khớp", "Cây xương khớp", "Orthosiphon aristatus", "Rau mèo")
            key.contains("sam cau") || key.contains("curculigo") ->
                listOf("Sâm cau", "Curculigo orchioides")
            key.contains("cuc tan") || key.contains("artemisia argyi") || key.contains("ai diep") ->
                listOf("Cúc tần", "Ngải diệp", "Artemisia argyi")
            key.contains("allium tuberosum") || key.contains("garlic chives") || key.contains("chinese chives") || key.contains("ca he") ->
                listOf("Hẹ", "Allium tuberosum", "Garlic chives")
            key.contains("dan sam") || key.contains("salvia miltiorrhiza") || key.contains("danshen") ->
                listOf("Đan sâm", "Salvia miltiorrhiza", "Danshen")
            key.contains("trinh nu hoang cung") || key.contains("actaea cimicifuga") || key.contains("cimicifuga") || key.contains("thang ma") ->
                listOf("Trinh nữ hoàng cung", "Thăng ma", "Actaea cimicifuga")
            else -> emptyList()
        }
    }

    private fun normalize(value: String?): String {
        return Normalizer.normalize(value.orEmpty().lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .replace("[^a-z0-9\\s-]".toRegex(), " ")
            .replace("-", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
