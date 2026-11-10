package com.expeditee.plant.identifier.data.repository

import android.util.Log
import com.expeditee.plant.identifier.network.PlantBackendApiService

class VietnameseTranslationRepository(
    private val apiService: PlantBackendApiService,
) {
    private val cache = mutableMapOf<String, String>()

    suspend fun translateDisplayText(text: String?): String? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        fallback(trimmed)?.let { return it }
        if (looksScientificName(trimmed)) return trimmed
        if (!shouldTranslate(trimmed)) return trimmed

        cache[trimmed]?.let { return it }
        val translated = runCatching {
            apiService.sendChat(
                message = buildPrompt(trimmed),
                language = "vi",
            ).data?.answer?.trim()
        }.onFailure {
            Log.w(TAG, "translateDisplayText failed for '$trimmed': ${it.message}")
        }.getOrNull()
            ?.cleanAnswer()
            ?.takeIf { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }

        val result = translated ?: fallback(trimmed) ?: trimmed
        cache[trimmed] = result
        return result
    }

    suspend fun translateDisplayList(items: List<String>?): List<String> {
        return items.orEmpty()
            .mapNotNull { translateDisplayText(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    private fun buildPrompt(text: String): String {
        return """
            Dịch nội dung sau sang tiếng Việt tự nhiên để hiển thị trong app nhận diện cây.
            Chỉ trả về bản dịch, không giải thích, không thêm dấu ngoặc kép.
            Nếu là tên khoa học Latin dạng chi loài thì giữ nguyên.
            Nội dung: $text
        """.trimIndent()
    }

    private fun String.cleanAnswer(): String {
        return trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .removePrefix("'")
            .removeSuffix("'")
            .trim()
    }

    private fun shouldTranslate(text: String): Boolean {
        if (looksScientificName(text)) return false
        if (text.any { it in VIETNAMESE_MARKS } || text.contains('đ', true)) return false
        return Regex("[A-Za-z]{3,}").containsMatchIn(text)
    }

    private fun looksScientificName(text: String): Boolean {
        val words = text.trim().split(Regex("\\s+"))
        if (words.size !in 2..4) return false
        val first = words.first()
        val second = words.getOrNull(1).orEmpty()
        return first.firstOrNull()?.isUpperCase() == true &&
            first.drop(1).all { it.isLowerCase() } &&
            second.all { it.isLowerCase() || it == '-' } &&
            words.none { COMMON_ENGLISH_NAME_WORDS.contains(it.lowercase()) }
    }

    private fun fallback(text: String): String? {
        return NameFallbackMatcher.match(text, NAME_FALLBACKS)
    }

    companion object {
        private const val TAG = "VnTranslationRepo"
        private const val VIETNAMESE_MARKS =
            "ăâêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ"

        private val COMMON_ENGLISH_NAME_WORDS = setOf(
            "plant", "tree", "leaf", "flower", "vine", "grass", "palm", "cactus", "fern",
        )

        private val NAME_FALLBACKS = mapOf(
            "snake plant" to "Cây lưỡi hổ",
            "mother-in-law's tongue" to "Cây lưỡi hổ",
            "mother in law tongue" to "Cây lưỡi hổ",
            "golden pothos" to "Cây trầu bà vàng",
            "money plant" to "Cây trầu bà",
            "paper flower" to "Hoa giấy",
            "bougainvillea" to "Hoa giấy",
            "great bougainvillea" to "Hoa giấy",
            "ixora coccinea" to "Bông trang",
            "jungle geranium" to "Bông trang",
            "hibiscus" to "Dâm bụt",
            "chinese hibiscus" to "Dâm bụt",
            "hibiscus rosa-sinensis" to "Dâm bụt",
            "ixora coccinea" to "Bông trang",
            "ixora chinensis" to "Bông trang",
            "jungle geranium" to "Bông trang",
            "dracaena fragrans" to "Thiết mộc lan",
            "corn plant" to "Thiết mộc lan",
            "mass cane" to "Thiết mộc lan",
            "fragrant dracaena" to "Thiết mộc lan",
            "aglaonema" to "Đại phú gia",
            "aglaonema sp." to "Đại phú gia",
            "dieffenbachia spp." to "Ngọc ngân",
            "dieffenbachia sp." to "Ngọc ngân",
            "dieffenbachia seguine" to "Ngọc ngân",
            "dumb cane" to "Ngọc ngân",
            "leopard lily" to "Ngọc ngân",
            "tuft jungle fires" to "Ngọc ngân",
            "sphagneticola trilobata" to "Sài đất cảnh",
            "wedelia trilobata" to "Sài đất cảnh",
            "singapore daisy" to "Sài đất cảnh",
            "creeping oxeye" to "Sài đất cảnh",
            "eclipta prostrata" to "Cỏ nhọ nồi",
            "eclipta alba" to "Cỏ nhọ nồi",
            "trailing eclipta" to "Cỏ nhọ nồi",
            "white eclipta" to "Cỏ nhọ nồi",
            "herba ecliptae" to "Cỏ nhọ nồi",
            "false daisy" to "Cỏ nhọ nồi",
            "yerba de tago" to "Cỏ nhọ nồi",
            "yerba de tajo" to "Cỏ nhọ nồi",
            "bhringraj" to "Cỏ nhọ nồi",
            "bhringaraj" to "Cỏ nhọ nồi",
            "karisalankanni" to "Cỏ nhọ nồi",
            "kehraj" to "Cỏ nhọ nồi",
            "scoparia dulcis" to "Cam thảo đất",
            "sweet broomweed" to "Cam thảo đất",
            "licorice weed" to "Cam thảo đất",
            "rose" to "Hoa hồng",
            "china rose" to "Hoa hồng",
            "chinese rose" to "Hoa hồng",
            "chrysanthemum" to "Hoa cúc",
            "florist's daisy" to "Hoa cúc",
            "florists daisy" to "Hoa cúc",
            "moth orchid" to "Lan hồ điệp",
            "moon orchid" to "Lan hồ điệp",
            "phalaenopsis" to "Lan hồ điệp",
            "sunflower" to "Hoa hướng dương",
            "common sunflower" to "Hoa hướng dương",
            "arabian jasmine" to "Hoa nhài",
            "sambac jasmine" to "Hoa nhài",
            "jasmine" to "Hoa nhài",
            "sacred lotus" to "Hoa sen",
            "lotus" to "Hoa sen",
            "aloe vera" to "Nha đam",
            "peace lily" to "Cây lan ý",
            "spider plant" to "Cây dây nhện",
            "rubber plant" to "Cây đa búp đỏ",
            "fiddle leaf fig" to "Cây bàng Singapore",
            "zz plant" to "Cây kim tiền",
            "jade plant" to "Cây ngọc bích",
            "monstera" to "Cây trầu bà lá xẻ",
            "swiss cheese plant" to "Cây trầu bà lá xẻ",
            "rice" to "Lúa",
            "oryza sativa" to "Lúa",
            "sweet potato" to "Khoai lang",
            "ipomoea batatas" to "Khoai lang",
            "maize" to "Ngô",
            "corn" to "Ngô",
            "zea mays" to "Ngô",
            "tomato" to "Cà chua",
            "solanum lycopersicum" to "Cà chua",
            "chili pepper" to "Ớt",
            "capsicum annuum" to "Ớt",
            "mung bean" to "Đậu xanh",
            "soybean" to "Đậu nành",
            "peanut" to "Lạc",
            "black gram" to "Đậu đen",
            "potato" to "Khoai tây",
            "cassava" to "Sắn",
            "taro" to "Khoai môn",
            "cucumber" to "Dưa chuột",
            "pumpkin" to "Bí đỏ",
            "wax gourd" to "Bí xanh",
            "bottle gourd" to "Bầu",
            "loofah" to "Mướp hương",
            "luffa" to "Mướp hương",
            "water spinach" to "Rau muống",
            "bok choy" to "Cải xanh",
            "cabbage" to "Cải bắp",
            "lettuce" to "Xà lách",
            "spring onion" to "Hành lá",
            "garlic" to "Tỏi",
            "watermelon" to "Dưa hấu",
            "papaya" to "Đu đủ",
            "banana" to "Chuối",
            "mango" to "Xoài",
            "orange" to "Cam",
            "lemon" to "Chanh",
            "pomelo" to "Bưởi",
            "rambutan" to "Chôm chôm",
            "durian" to "Sầu riêng",
            "muntingia calabura" to "Trứng cá",
            "muntingia" to "Trứng cá",
            "jamaican cherry" to "Trứng cá",
            "calabura" to "Trứng cá",
            "panama berry" to "Trứng cá",
            "strawberry tree" to "Trứng cá",
            "phalsa" to "Trứng cá",
            "grewia asiatica" to "Trứng cá",
            "terminalia catappa" to "Cây bàng",
            "delonix regia" to "Phượng vĩ",
            "bougainvillea glabra" to "Hoa giấy",
            "lagenaria siceraria" to "Bầu",
            "benincasa hispida" to "Bí xanh",
            "gnetum gnemon" to "Lá giang",
            "wax apple" to "Mận",
            "java apple" to "Mận",
            "jackfruit" to "Mít",
            "yardlong bean" to "Đậu que",
            "green bean" to "Đậu que",
            "katuk" to "Rau ngót",
            "amaranth" to "Rau dền",
            "green amaranth" to "Rau dền",
            "slender amaranth" to "Rau dền",
            "amaranthus viridis" to "Rau dền",
            "amaranthus hybridus" to "Rau dền",
            "amaranthus tricolor" to "Rau dền",
            "amaranthus spinosus" to "Rau dền gai",
            "fish mint" to "Diếp cá",
            "centella asiatica" to "Rau má",
            "gotu kola" to "Rau má",
            "plantain seed" to "Mã đề",
            "perilla" to "Tía tô",
            "betel leaf" to "Lá lốt",
            "piper lolot" to "Lá lốt",
            "lemongrass" to "Sả",
            "ginger" to "Gừng",
            "turmeric" to "Nghệ",
            "bitter melon" to "Mướp đắng",
            "bitter gourd" to "Mướp đắng",
            "mugwort" to "Ngải cứu",
            "artemisia" to "Ngải cứu",
            "stevia" to "Cỏ ngọt",
            "longan" to "Long nhãn",
            "star fruit" to "Khế",
            "starfruit" to "Khế",
            "carambola" to "Khế",
            "averrhoa carambola" to "Khế",
            "averrhoa bilimbi" to "Khế tàu",
            "skunkvine" to "Lá mơ",
            "paederia" to "Lá mơ",
            "paederia foetida" to "Lá mơ",
            "paederia scandens" to "Lá mơ",
            "stinkvine" to "Lá mơ",
            "andrographis paniculata" to "Cây mật gấu",
            "king of bitters" to "Cây mật gấu",
            "taraxacum officinale" to "Bồ công anh",
            "dandelion" to "Bồ công anh",
            "lonicera japonica" to "Kim ngân hoa",
            "japanese honeysuckle" to "Kim ngân hoa",
            "honeysuckle" to "Kim ngân hoa",
            "hedyotis diffusa" to "Hoàn ngọc",
            "oldenlandia diffusa" to "Hoàn ngọc",
            "orthosiphon aristatus" to "Trà xương khớp",
            "java tea" to "Trà xương khớp",
            "cat's whiskers" to "Trà xương khớp",
            "curculigo orchioides" to "Sâm cau",
            "artemisia argyi" to "Cúc tần",
            "argy wormwood" to "Cúc tần",
            "allium tuberosum" to "Hẹ",
            "garlic chives" to "Hẹ",
            "chinese chives" to "Hẹ",
            "salvia miltiorrhiza" to "Đan sâm",
            "danshen" to "Đan sâm",
            "red sage" to "Đan sâm",
            "actaea cimicifuga" to "Trinh nữ hoàng cung",
            "cimicifuga foetida" to "Trinh nữ hoàng cung",
            "black cohosh" to "Trinh nữ hoàng cung",
            "cinnamon" to "Quế",
            "licorice" to "Cam thảo",
            "snake plant" to "Lưỡi hổ",
            "mother-in-law's tongue" to "Lưỡi hổ",
            "mother in law tongue" to "Lưỡi hổ",
            "dracaena trifasciata" to "Lưỡi hổ",
            "sansevieria trifasciata" to "Lưỡi hổ",
            "sansevieria" to "Lưỡi hổ",
            "callisia fragrans" to "Lược vàng",
            "basket plant" to "Lược vàng",
            "peperomia pellucida" to "Tần dày lá",
            "shiny bush" to "Tần dày lá",
            "pseuderanthemum reticulatum" to "Sao nhái",
            "polianthes tuberosa" to "Hoa huệ",
            "tuberose" to "Hoa huệ",
            "healthy" to "Cây khỏe",
            "unknown disease" to "Bệnh cây chưa xác định",
            "yellow leaf" to "Vàng lá",
            "yellow leaves" to "Vàng lá",
            "yellowing" to "Vàng lá",
            "chlorosis" to "Vàng lá",
            "leaf spot" to "Bệnh đốm lá",
            "root rot" to "Bệnh thối rễ",
            "powdery mildew" to "Bệnh phấn trắng",
            "downy mildew" to "Bệnh sương mai",
            "anthracnose" to "Bệnh thán thư",
            "blight" to "Bệnh cháy lá",
            "rust" to "Bệnh gỉ sắt",
            "mosaic virus" to "Bệnh khảm lá",
        )
    }
}
