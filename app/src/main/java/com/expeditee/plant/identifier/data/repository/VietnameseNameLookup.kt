package com.expeditee.plant.identifier.data.repository

object VietnameseNameLookup {
    const val UNKNOWN_PLACEHOLDER = "Cây chưa rõ tên Việt"

    private const val VIETNAMESE_MARKS =
        "ăâêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ" +
            "ĂÂÊÔƠƯÁÀẢÃẠẮẰẲẴẶẤẦẨẪẬÉÈẺẼẸẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌỐỒỔỖỘỚỜỞỠỢÚÙỦŨỤỨỪỬỮỰÝỲỶỸỴđĐ"

    fun needsRemoteLookup(commonNames: List<String>, scientificName: String): Boolean {
        if (scientificName.isBlank()) return false
        return commonNames.none { isUsableVietnameseName(it) }
    }

    fun parseModelAnswer(raw: String?): String? {
        val text = raw.orEmpty().trim()
        if (text.isEmpty()) return null
        return text.lineSequence()
            .map { cleanLine(it) }
            .firstNotNullOfOrNull { candidate ->
                if (candidate.isBlank()) null
                else if (candidate.equals("UNKNOWN", ignoreCase = true)) null
                else if (candidate.equals(UNKNOWN_PLACEHOLDER, ignoreCase = true)) null
                else if (looksScientific(candidate)) null
                else candidate.takeIf { isUsableVietnameseName(it) && isShortName(it) }
            }
    }

    private fun cleanLine(line: String): String {
        var value = line.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        value = value
            .replace(
                Regex("^(tên tiếng việt|tên việt|tên thường gọi)\\s*[:\\-]\\s*", RegexOption.IGNORE_CASE),
                "",
            )
            .trim()
        return value.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
    }

    fun prompt(scientificName: String, commonNames: List<String>): String {
        val english = commonNames
            .map { it.trim() }
            .filter { it.isNotBlank() && it != UNKNOWN_PLACEHOLDER && !looksScientific(it) }
            .distinct()
            .joinToString(", ")
        return buildString {
            appendLine("Bạn là chuyên gia thực vật Việt Nam.")
            appendLine("Tên khoa học: $scientificName")
            if (english.isNotBlank()) {
                appendLine("Tên tiếng Anh (nếu có): $english")
            }
            appendLine("Hãy trả về ĐÚNG MỘT tên tiếng Việt thông dụng nhất.")
            appendLine("Không giải thích, không markdown, không tên khoa học.")
            appendLine("Nếu không chắc, trả về UNKNOWN.")
        }.trim()
    }

    fun isUsableVietnameseName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.equals(UNKNOWN_PLACEHOLDER, ignoreCase = true)) return false
        if (looksScientific(trimmed)) return false
        return trimmed.any { it in VIETNAMESE_MARKS }
    }

    private fun isShortName(name: String): Boolean {
        return name.length <= 40 && name.split(Regex("\\s+")).size <= 6
    }

    private fun looksScientific(text: String): Boolean {
        val words = text.trim().split(Regex("\\s+"))
        if (words.size !in 2..4) return false
        val first = words.first()
        val second = words.getOrNull(1).orEmpty()
        return first.firstOrNull()?.isUpperCase() == true &&
            first.drop(1).all { it.isLowerCase() } &&
            second.all { it.isLowerCase() || it == '-' }
    }
}
