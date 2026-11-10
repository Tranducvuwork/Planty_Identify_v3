package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.model.Solution
import java.text.Normalizer

object ThesisDiseaseCopy {

    fun vietnameseName(raw: String?): String {
        val key = fold(raw)
        if (key.isBlank()) return "Bệnh cây"
        return when {
            "anthracnose" in key || "than thu" in key -> "Bệnh thán thư"
            "powdery" in key || "phan trang" in key -> "Bệnh phấn trắng"
            "downy" in key || "suong mai" in key -> "Bệnh sương mai"
            "leaf scorch" in key || "dry edge" in key || "chay mep" in key || "kho mep" in key -> "Đốm lá / cháy mép lá"
            "blight" in key || "chay la" in key -> "Bệnh cháy lá"
            "leaf spot" in key || "brown spot" in key || "necrotic" in key || "dom la" in key || "dom nau" in key || key.endsWith(" spot") || key == "spot" -> "Bệnh đốm lá"
            "leaf miner" in key || "leafminer" in key || "duc la" in key || "mine" in key -> "Sâu đục lá"
            isYellowLeafKey(key) -> "Vàng lá"
            "root rot" in key || "thoi re" in key -> "Bệnh thối rễ"
            "rust" in key || "gi sat" in key -> "Bệnh gỉ sắt"
            "mosaic" in key || "kham la" in key -> "Bệnh khảm lá"
            "aphid" in key || "rep" in key -> "Rệp hại"
            "mealy" in key || "rep sap" in key -> "Rệp sáp"
            "scale" in key || "rap" in key -> "Rệp vảy"
            "spider mite" in key || "mite" in key || "nhen do" in key -> "Nhện đỏ"
            "caterpillar" in key || "sau an la" in key || "worm" in key -> "Sâu ăn lá"
            "wilt" in key || "heo" in key -> "Bệnh héo"
            "canker" in key -> "Bệnh ung thư thân"
            "sooty" in key -> "Nấm bồ hóng"
            "healthy" in key || "khoe" in key -> "Cây khỏe"
            looksLatinOnly(raw) -> "Bệnh cây"
            else -> raw!!.trim()
        }
    }

    fun vietnameseDescription(name: String, notes: String?): String {
        val vnName = vietnameseName(name)
        val hint = treatmentHint(name)
        val note = notes?.trim().orEmpty()
        val noteVn = when {
            note.isBlank() -> ""
            looksLatinOnly(note) ->
                "Ảnh cho thấy mô bệnh / đốm / lá vàng — cần cách ly và xử lý sớm."
            else -> note
        }
        return buildString {
            append(vnName)
            append(". ")
            if (hint.possibleCause.isNotBlank()) {
                append(hint.possibleCause)
                append(" ")
            }
            if (noteVn.isNotBlank()) {
                append(noteVn)
                append(" ")
            }
            append(hint.simpleCure)
        }
    }

    fun buildSolution(rawName: String?): Solution {
        val hint = treatmentHint(rawName)
        return Solution(
            title = "Gợi ý cách chữa",
            summary = hint.possibleCause,
            steps = hint.steps,
            precautions = listOf(
                "Cách ly cây bệnh khỏi cây khỏe.",
                "Không phun thuốc hóa học liều cao nếu chưa hỏi khuyến nông.",
                "Theo dõi 5–7 ngày sau khi xử lý.",
            ),
        )
    }

    fun treatmentHint(raw: String?): TreatmentHint {
        val key = fold(raw)
        return when {
            "leaf miner" in key || "leafminer" in key || "duc la" in key || "mine" in key ->
                TreatmentHint(
                    possibleCause = "Có thể bị sâu bệnh đục lá.",
                    simpleCure = "Gợi ý cách chữa: ngắt lá bị đường đục, phun dầu neem loãng buổi chiều mát.",
                    steps = listOf(
                        "Ngắt và hủy lá có đường đục của sâu.",
                        "Phun dầu neem loãng (theo hướng dẫn trên chai) 1 lần/tuần trong 2–3 tuần.",
                        "Tăng thông gió, tránh để lá ẩm lâu.",
                    ),
                )
            isYellowLeafKey(key) ->
                TreatmentHint(
                    possibleCause = "Có thể do thiếu dinh dưỡng, úng nước hoặc sâu bệnh làm vàng lá.",
                    simpleCure = "Gợi ý cách chữa: kiểm tra rễ úng, bón phân hữu cơ nhẹ, cắt lá vàng già.",
                    steps = listOf(
                        "Cắt bỏ lá vàng già, kiểm tra đất có bị úng không.",
                        "Để đất khô mặt trước khi tưới lại.",
                        "Bón phân hữu cơ / NPK nhẹ; nếu nghi thiếu sắt thì bổ sung phân vi lượng.",
                    ),
                )
            "leaf scorch" in key || "dry edge" in key || "chay mep" in key || "kho mep" in key ->
                TreatmentHint(
                    possibleCause = "Có thể do cháy nắng, tưới chưa hợp lý, úng rễ nhẹ hoặc nấm đốm lá làm khô mép.",
                    simpleCure = "Gợi ý cách chữa: chuyển cây sang sáng tán xạ, kiểm tra thoát nước, cắt phần lá bệnh và theo dõi vết lan.",
                    steps = listOf(
                        "Cắt bỏ phần mép lá khô / đốm nâu nặng bằng kéo sạch.",
                        "Đặt cây nơi sáng tán xạ, tránh nắng gắt giữa trưa.",
                        "Chỉ tưới khi lớp đất mặt khô 2–3 cm và không để nước đọng trong chậu.",
                        "Nếu đốm tiếp tục lan, phun dầu neem hoặc chế phẩm sinh học trị nấm theo hướng dẫn.",
                    ),
                )
            "leaf spot" in key || "brown spot" in key || "necrotic" in key || "dom la" in key || "dom nau" in key || "spot" in key || "anthracnose" in key || "than thu" in key ->
                TreatmentHint(
                    possibleCause = "Có thể bị bệnh đốm lá / nấm.",
                    simpleCure = "Gợi ý cách chữa: cắt lá bệnh, tránh tưới ướt lá buổi tối, dùng dầu neem hoặc chế phẩm sinh học.",
                    steps = listOf(
                        "Cắt và hủy phần lá đốm / thối.",
                        "Tưới gốc, tránh tưới ướt lá vào chiều tối.",
                        "Phun dầu neem hoặc Bacillus subtilis theo hướng dẫn.",
                    ),
                )
            "powdery" in key || "phan trang" in key ->
                TreatmentHint(
                    possibleCause = "Có thể bị bệnh phấn trắng.",
                    simpleCure = "Gợi ý cách chữa: tăng thông gió, cắt lá bệnh, phun baking soda loãng hoặc dầu neem.",
                    steps = listOf(
                        "Cắt lá phủ phấn trắng nặng.",
                        "Tăng khoảng cách giữa các chậu để thông gió.",
                        "Phun hỗn hợp baking soda loãng hoặc dầu neem buổi sáng.",
                    ),
                )
            "aphid" in key || "mealy" in key || "mite" in key || "rep" in key || "nhen" in key ->
                TreatmentHint(
                    possibleCause = "Có thể bị rệp / nhện hại.",
                    simpleCure = "Gợi ý cách chữa: rửa lá bằng nước, phun dầu neem, bắt côn trùng bằng tay.",
                    steps = listOf(
                        "Rửa lá bằng nước sạch hoặc khăn ẩm.",
                        "Phun dầu neem loãng 1 lần/tuần.",
                        "Cách ly cây đến khi hết sâu hại.",
                    ),
                )
            "root rot" in key || "thoi re" in key || "wilt" in key ->
                TreatmentHint(
                    possibleCause = "Có thể bị thối rễ / héo do úng nước.",
                    simpleCure = "Gợi ý cách chữa: giảm tưới, thay đất thoát nước, cắt rễ thối.",
                    steps = listOf(
                        "Nhấc cây kiểm tra rễ, cắt phần thối đen.",
                        "Thay đất tơi xốp, chậu có lỗ thoát nước.",
                        "Tưới ít hơn, để mặt đất khô giữa các lần tưới.",
                    ),
                )
            else ->
                TreatmentHint(
                    possibleCause = "Có thể bị sâu bệnh đục lá hoặc bệnh nấm trên lá.",
                    simpleCure = "Gợi ý cách chữa: cắt phần bệnh, thông gió, dùng dầu neem loãng.",
                    steps = listOf(
                        "Cắt bỏ lá đốm / vàng / có đường đục.",
                        "Tăng thông gió, tránh tưới ướt lá buổi tối.",
                        "Phun dầu neem loãng và theo dõi thêm 1 tuần.",
                    ),
                )
        }
    }

    private fun isYellowLeafKey(key: String): Boolean =
        "yellow" in key ||
            "chlorosis" in key ||
            "yellowing" in key ||
            "vang la" in key ||
            "la vang" in key ||
            "la ua" in key ||
            "ua vang" in key ||
            "dom vang" in key ||
            "vang mep la" in key ||
            "mat diep luc" in key

    data class TreatmentHint(
        val possibleCause: String,
        val simpleCure: String,
        val steps: List<String>,
    )

    private fun fold(value: String?): String {
        val nfd = Normalizer.normalize(value.orEmpty().trim().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun looksLatinOnly(text: String?): Boolean {
        val value = text.orEmpty().trim()
        if (value.isEmpty()) return false
        val letters = value.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.all { it.code < 128 }
    }
}
