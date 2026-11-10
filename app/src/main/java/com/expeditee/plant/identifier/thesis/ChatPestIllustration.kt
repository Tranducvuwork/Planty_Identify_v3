package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.network.model.CommonDiseaseData
import com.expeditee.plant.identifier.network.model.PlantScanData
import java.text.Normalizer
import java.util.Locale

data class ChatPestReply(
    val text: String,
    val imageUrls: List<String>,
)

/**
 * Answers pest-shape questions locally with catalog data + illustration assets,
 * so chat does not rely on the LLM for images.
 */
object ChatPestIllustration {
    private val pestQuestionTokens = listOf(
        "hinh dang", "hinh anh", "co hinh", "minh hoa", "hinh minh hoa",
        "nhan biet sau", "nhan biet benh", "dang sau", "dang benh",
    )

    private val pestTopicTokens = listOf(
        "sau benh", "sau hai", "benh hai", "benh thuong gap", "sau", "benh", "rep",
    )

    private val plantIdHints = mapOf(
        "lua" to listOf("lua", "oryza", "thoc", "cay lua"),
        "ngo" to listOf("ngo", "bap", "zea mays", "zea"),
        "xoai" to listOf("xoai", "mango", "mangifera"),
        "tao" to listOf("tao", "malus", "apple"),
    )

    fun build(context: Context, message: String, plantName: String = ""): ChatPestReply? {
        val blob = fold("$message $plantName")
        if (!matchesPestIllustrationQuestion(blob)) return null

        val plant = findPlant(context, blob, plantName)
        val diseases = plant?.commonDiseases.orEmpty()
            .filter { !it.name.isNullOrBlank() }
            .ifEmpty { defaultPestsFor(blob) }

        if (diseases.isEmpty()) return null

        val displayPlant = plant?.commonNames?.firstOrNull()
            ?: plantName.takeIf { it.isNotBlank() }
            ?: guessPlantLabel(blob)
            ?: "cây này"

        val lines = buildList {
            add("Dưới đây là **một số sâu/bệnh thường gặp** trên $displayPlant (ảnh minh họa trong app):")
            diseases.take(4).forEach { disease ->
                val name = disease.name.orEmpty()
                val desc = disease.description?.takeIf { it.isNotBlank() }
                if (desc != null) {
                    add("• **$name**: $desc")
                } else {
                    add("• **$name**")
                }
            }
            add("")
            add("Ảnh chỉ mang tính tham khảo. Nếu cần chẩn đoán chính xác, dùng chức năng **chẩn đoán bệnh** hoặc hỏi khuyến nông.")
        }

        val images = diseases
            .take(4)
            .mapNotNull { disease -> imageFor(disease) }
            .distinct()

        if (images.isEmpty()) return null
        return ChatPestReply(text = lines.joinToString("\n"), imageUrls = images)
    }

    private fun imageFor(disease: CommonDiseaseData): String? {
        val local = disease.imageUrl?.takeIf { it.isNotBlank() && !it.startsWith("http") }
        return local ?: DiseaseImages.urlFor(disease.name).takeIf { it.isNotBlank() }
    }

    fun matchesPestIllustrationQuestion(message: String, plantName: String = ""): Boolean {
        return isPestIllustrationQuestion(fold("$message $plantName"))
    }

    private fun isPestIllustrationQuestion(blob: String): Boolean {
        val asksVisual = pestQuestionTokens.any { it in blob } ||
            ("hinh" in blob && pestTopicTokens.any { it in blob })
        val asksPest = pestTopicTokens.any { it in blob }
        return asksVisual && asksPest
    }

    private fun findPlant(context: Context, blob: String, plantName: String): PlantScanData? {
        if (plantName.isNotBlank()) {
            ThesisPlantCatalog.findByName(context, null, listOf(plantName))?.let { return it }
        }
        plantIdHints.entries.firstOrNull { (_, tokens) ->
            tokens.any { it in blob }
        }?.let { (plantId, _) ->
            ThesisPlantCatalog.findByQrOrId(context, plantId)?.let { return it }
        }
        return ThesisPlantCatalog.all(context).firstOrNull { plant ->
            val names = listOfNotNull(plant.scientificName, plant.plantId) + plant.commonNames.orEmpty()
            names.any { name ->
                val folded = fold(name)
                folded.length >= 4 && folded in blob
            }
        }
    }

    private fun guessPlantLabel(blob: String): String? = when {
        "lua" in blob || "oryza" in blob -> "lúa"
        "ngo" in blob || "bap" in blob -> "ngô"
        "xoai" in blob -> "xoài"
        "tao" in blob -> "táo"
        else -> null
    }

    private fun defaultPestsFor(blob: String): List<CommonDiseaseData> {
        return when {
            "lua" in blob || "oryza" in blob -> listOf(
                pest("Đạo ôn", "Vết bệnh hình thoi trên lá lúa."),
                pest("Sâu cuốn lá", "Lá cuốn thành ống, sâu xanh bên trong."),
                pest("Vàng lá", "Lá vàng do thiếu dinh dưỡng hoặc úng nước."),
            )
            "ngo" in blob || "bap" in blob || "zea" in blob -> listOf(
                pest("Sâu đục thân", "Thân có lỗ đục, cây dễ gãy."),
                pest("Đốm lá", "Đốm nâu trên lá."),
                pest("Vàng lá", "Lá vàng do úng hoặc thiếu dinh dưỡng."),
            )
            else -> listOf(
                pest("Rệp", "Rệp bám mặt dưới lá, hút nhựa."),
                pest("Sâu ăn lá", "Lá bị khuyết, sâu cắn."),
                pest("Đốm lá do ẩm", "Đốm nâu khi trời ẩm."),
            )
        }
    }

    private fun pest(name: String, description: String) = CommonDiseaseData(
        name = name,
        description = description,
        ecoTreatment = null,
        imageUrl = DiseaseImages.urlFor(name),
    )

    private fun fold(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(Locale.US), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }
}
