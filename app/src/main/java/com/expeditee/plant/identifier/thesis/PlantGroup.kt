package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.PlantScanData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import java.text.Normalizer

enum class PlantGroup(val key: String, val label: String) {
    EDUCATION("education", "Giáo dục"),
    MEDICINE("medicine", "Y học / cây thuốc"),
    AGRICULTURE("agriculture", "Nông nghiệp"),
    VEGETABLE("vegetable", "Rau"),
    FRUIT("fruit", "Cây ăn quả"),
    MUSHROOM("mushroom", "Nấm ngoài trời"),
    TOXIC("toxic", "Cây / nấm độc"),
    ORNAMENTAL("ornamental", "Cây cảnh");

    companion object {
        fun fromKey(raw: String?): PlantGroup? =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) }

        fun from(result: PlantScanResult): PlantGroup {
            fromKey(result.plantGroup)?.let { return it }
            return infer(
                scientific = result.scientificName,
                names = result.commonNames,
                medicinal = result.isMedicinal,
                toxic = result.toxicity?.isToxic == true ||
                    result.toxicity?.playAlertSound == true,
                fungus = result.toxicity?.isFungus == true,
            )
        }

        fun from(data: PlantScanData): PlantGroup {
            fromKey(data.plantGroup)?.let { return it }
            return infer(
                scientific = data.scientificName,
                names = data.commonNames.orEmpty(),
                medicinal = data.isMedicinal == true,
                toxic = data.toxicity?.isToxic == true ||
                    data.toxicity?.playAlertSound == true,
                fungus = data.toxicity?.isFungus == true,
            )
        }

        fun chips(group: PlantGroup, plantName: String): List<AiChip> {
            val name = plantName.ifBlank { "cây này" }
            return when (group) {
                MEDICINE -> listOf(
                    AiChip("learn", "Tìm hiểu về $name"),
                    AiChip("distribution", "Vùng phân bố"),
                    AiChip("uses", "Công dụng"),
                    AiChip("folk", "Bài thuốc từ cây"),
                    AiChip("care", "Cách trồng và chăm sóc"),
                    AiChip("diagnose", "Chẩn đoán tình trạng cây"),
                )
                AGRICULTURE, VEGETABLE, FRUIT -> listOf(
                    AiChip("learn", "Đặc điểm cây"),
                    AiChip("care", "Cách trồng và chăm sóc"),
                    AiChip("fertilizer", "Bón phân hợp lý"),
                    AiChip("pests", "Sâu bệnh thường gặp"),
                    AiChip("diagnose", "Chẩn đoán bệnh"),
                    AiChip("harvest", "Thu hoạch và bảo quản"),
                )
                EDUCATION -> listOf(
                    AiChip("learn", "Tìm hiểu về $name"),
                    AiChip("structure", "Cấu tạo rễ – thân – lá"),
                    AiChip("care", "Cách trồng và chăm sóc"),
                    AiChip("pests", "Sâu bệnh thường gặp"),
                )
                MUSHROOM -> listOf(
                    AiChip("learn", "Nhận biết $name"),
                    AiChip("uses", "Ăn được hay độc?"),
                    AiChip("warning", "An toàn khi gặp nấm"),
                    AiChip("notes", "Không ăn nếu chưa chắc"),
                )
                TOXIC -> listOf(
                    AiChip("warning", "Cảnh báo an toàn"),
                    AiChip("learn", "Nhận biết $name"),
                    AiChip("notes", "Không chạm / không ăn"),
                )
                ORNAMENTAL -> listOf(
                    AiChip("learn", "Tìm hiểu về $name"),
                    AiChip("care", "Cách trồng và chăm sóc"),
                    AiChip("pests", "Sâu bệnh thường gặp"),
                    AiChip("diagnose", "Chẩn đoán tình trạng cây"),
                )
            }
        }

        private fun infer(
            scientific: String?,
            names: List<String>,
            medicinal: Boolean,
            toxic: Boolean,
            fungus: Boolean,
        ): PlantGroup {
            if (toxic) return TOXIC
            if (fungus) return MUSHROOM
            val blob = " ${fold("$scientific ${names.joinToString(" ")}")} "
            if (educationTokens.any { " $it " in blob || blob.contains(it) && it.length >= 6 }) {
                if (educationExact.any { it in blob }) return EDUCATION
            }
            if (agricultureExact.any { it in blob }) return AGRICULTURE
            if (vegetableExact.any { it in blob }) return VEGETABLE
            if (fruitExact.any { it in blob }) return FRUIT
            if (medicinal || medicineExact.any { it in blob }) return MEDICINE
            return ORNAMENTAL
        }

        private fun fold(value: String): String {
            val nfd = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            return nfd.replace("\\p{Mn}+".toRegex(), "").replace('đ', 'd').trim()
        }

        private val educationExact = listOf(
            "terminalia", "catappa", "delonix", "phuong vi", "cay bang", " cay bang ",
            "bougainvillea", "hoa giay", "bong giay",
            "rosa chinensis", "hoa hong", "phalaenopsis", "lan ho diep",
            "chrysanthemum morifolium", "hoa cuc", "cay bong mat",
        )
        private val educationTokens = educationExact
        private val agricultureExact = listOf(
            "oryza", " lua ", "lua nuoc", "ipomoea batatas", "khoai lang",
            "zea mays", " ngo ", "ca chua", "solanum lycopersicum",
            "capsicum", " ot ", "citrullus", "dua hau",
        )
        private val vegetableExact = listOf(
            "rau ma", "centella", "rau sam", "portulaca", "muop dang", "momordica",
            "gnetum", "la giang", "cay giang", "melinjo",
            "lagenaria", "bau bi", " benincasa ", "bi xanh", "bi dao",
            "cucurbita maxima", "bi do", "cucurbita pepo",
        )
        private val fruitExact = listOf(
            "long nhan", "dimocarpus", "longan",
            "muntingia", "trung ca", "trungca",
            "spondias", "cay coc", "qua coc", "ambarella",
            "averrhoa", "averrhoa carambola", "star fruit", "starfruit", "carambola", " khe ",
        )
        private val medicineExact = listOf(
            "houttuynia", "diep ca", "polyscias", "dinh lang",
            "zingiber", "gung", "curcuma", "nghe", "cymbopogon", " sa ",
            "mentha", "bac ha", "perilla", "tia to", "piper sarmentosum", "la lot",
            "eclipta", "co nho noi", "co muc", "scoparia dulcis", "cam thao dat",
            "paederia", "paederia foetida", "la mo", "mo long",
            "andrographis", "mat gau", "xuyen tam lien",
            "taraxacum", "bo cong anh", "dandelion",
            "lonicera japonica", "kim ngan hoa", "nhan dong",
            "hedyotis", "hoan ngoc", "oldenlandia diffusa",
            "orthosiphon", "xuong khop", "rau meo", "java tea",
            "curculigo", "sam cau",
            "artemisia argyi", "cuc tan", "ai diep", "ngai diep",
            "allium tuberosum", "garlic chives", "chinese chives",
            "salvia miltiorrhiza", "dan sam", "danshen",
            "actaea cimicifuga", "cimicifuga", "trinh nu hoang cung", "thang ma",
            "ixora", "bong trang",
        )
    }
}

data class AiChip(val id: String, val label: String)
