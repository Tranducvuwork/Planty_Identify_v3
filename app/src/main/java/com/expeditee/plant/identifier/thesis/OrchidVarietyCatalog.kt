package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.CareData
import com.expeditee.plant.identifier.network.model.CommonDiseaseData
import com.expeditee.plant.identifier.network.model.PlantScanResult

data class OrchidVariety(
    val name: String,
    val scientificName: String,
    val aliases: List<String>,
    val description: String,
    val morphology: String,
    val origin: String,
    val uses: List<String>,
    val care: CareData,
    val diseases: List<CommonDiseaseData>,
    val imagePath: String? = null,
)

/** Nội dung khám phá các giống lan, dùng được cả khi catalog online chưa sẵn sàng. */
object OrchidVarietyCatalog {
    val varieties: List<OrchidVariety> = listOf(
        OrchidVariety(
            name = "Lan Mokara",
            scientificName = "Mokara hybrid (Arachnis × Ascocentrum × Vanda)",
            aliases = listOf("mokara", "lan mokara"),
            description = "Lan Mokara là lan lai giữa Arachnis, Ascocentrum và Vanda. Cây đơn thân, rễ trần, sinh trưởng nhanh, hoa đẹp và có giá trị kinh tế cao; thường trồng chậu hoặc trồng luống để cắt cành.",
            morphology = "Cây lan đơn thân, không có giả hành, thân vươn cao và có nhiều tầng rễ khí sinh. Đặc tính nổi bật là hoa đẹp, màu sắc đa dạng và tăng trưởng nhanh.",
            origin = "Lan lai trồng phổ biến ở vùng nhiệt đới; thích hợp với vườn có mái che, thoáng gió.",
            uses = listOf(
                "Trồng chậu làm cây cảnh.",
                "Trồng thành luống để thu hoa cắt cành.",
                "Có giá trị kinh tế cao trong sản xuất hoa.",
            ),
            care = CareData(
                planting = MOKARA_PLANTING,
                care = MOKARA_CARE,
                bestSeason = "Trồng khi thời tiết ấm, khô ráo; tránh giai đoạn mưa dầm hoặc nắng gắt.",
                water = "Rễ Mokara là rễ trần nên cần ẩm độ vườn cao nhưng không chịu úng. Tưới mỗi ngày 2 lần vào sáng sớm và chiều mát; luôn giữ vườn thông thoáng.",
                fertilizer = "Cây cần dinh dưỡng khá cao và thường xuyên. Kết hợp phân chuồng hoặc phân cá với NPK 30-10-10 hoặc 20-20-20 tùy tuổi cây và sinh trưởng. Vì rễ trần, dùng liều thấp và nồng độ loãng. Vàng, tuột lá chân thường liên quan đến thiếu nước hoặc thiếu dinh dưỡng, nhất là đạm.",
                light = "Nhiệt độ thích hợp 25-30°C. Che lưới để cây nhận khoảng 50-60% ánh sáng; lan cần ánh sáng trung bình và độ thông thoáng cao.",
                citations = listOf(
                    com.expeditee.plant.identifier.network.model.CitationData(
                        type = "document",
                        title = "Nguyễn Công Nghiệp (2006) — Kỹ thuật trồng lan",
                        url = "https://www.google.com/search?q=quy+tr%C3%ACnh+k%E1%BB%B9+thu%E1%BA%ADt+tr%E1%BB%93ng+lan+Mokara",
                    ),
                ),
                citationNote = "Nội dung tóm tắt theo tài liệu kỹ thuật người dùng cung cấp; điều chỉnh liều phân và thuốc theo nhãn sản phẩm, tuổi cây và điều kiện vườn.",
            ),
            diseases = listOf(
                CommonDiseaseData(
                    name = "Đốm lá (Cercospora sp.)",
                    description = "Các đốm bệnh xuất hiện trên lá, thường nặng hơn khi vườn ẩm bí.",
                    ecoTreatment = "Cắt bỏ lá bệnh, thu gom tiêu hủy, tăng thông thoáng. Tài liệu tham khảo nêu Zineb 3/2000 hoặc Benlat 1/2000; chỉ pha và sử dụng khi nhãn sản phẩm hiện hành cho phép.",
                ),
                CommonDiseaseData(
                    name = "Đốm vòng cánh hoa (Alternaria sp.)",
                    description = "Đốm vòng trên cánh hoa làm giảm chất lượng hoa cắt cành.",
                    ecoTreatment = "Giữ hoa và tán cây khô thoáng, loại bỏ hoa bệnh; có thể dùng thuốc nấm phổ rộng như Daconil theo nhãn.",
                ),
            ),
        ),
        OrchidVariety(
            name = "Lan hồ điệp",
            scientificName = "Phalaenopsis amabilis",
            aliases = listOf("lan ho diep", "phalaenopsis", "moth orchid"),
            description = "Lan hồ điệp có hoa bền, dáng hoa thanh lịch và phù hợp trồng chậu trong nhà có ánh sáng tán xạ.",
            morphology = "Lan đơn thân, lá bản rộng, rễ khí sinh dày và hoa mọc trên vòi cong.",
            origin = "Các vùng nhiệt đới châu Á và Đông Nam Á.",
            uses = listOf("Trồng chậu trang trí nhà ở, văn phòng và làm quà tặng."),
            care = CareData(
                planting = "Dùng chậu thoáng và giá thể vỏ thông, than hoặc dớn đã xử lý; không lấp kín cổ rễ.",
                care = "Tưới khi giá thể gần khô, tránh để nước đọng ở nách lá; bón phân lan loãng định kỳ.",
                bestSeason = "Thay chậu sau khi hoa tàn hoặc khi giá thể mục.",
                water = "Tưới đẫm rồi để thoát hết nước; ưu tiên buổi sáng.",
                fertilizer = "NPK cân đối hoặc phân chuyên lan với nồng độ loãng, giảm bón khi cây đang stress.",
                light = "Ánh sáng tán xạ, tránh nắng gắt giữa trưa.",
            ),
            diseases = emptyList(),
            imagePath = "plant_images/lan-ho-diep.jpg",
        ),
        OrchidVariety(
            name = "Lan Dendro",
            scientificName = "Dendrobium hybrid",
            aliases = listOf("dendro", "dendrobium", "lan dendro"),
            description = "Lan Dendro có thân giả hành rõ, nhiều dạng hoa và thường được trồng chậu hoặc làm hoa cắt cành.",
            morphology = "Thân mọng dạng giả hành, lá mọc dọc thân; hoa thường mọc thành chùm trên vòi ngắn.",
            origin = "Nhiều loài Dendrobium có nguồn gốc châu Á và vùng nhiệt đới.",
            uses = listOf("Trồng chậu, làm hoa trang trí và hoa cắt cành."),
            care = CareData(
                planting = "Giá thể thoáng như vỏ thông, than hoặc dớn; chậu phải thoát nước tốt.",
                care = "Tưới và bón phân theo giai đoạn tăng trưởng; giảm tưới khi cây cần phân hóa mầm hoa.",
                bestSeason = "Thay chậu sau mùa hoa hoặc khi rễ đã ổn định.",
                water = "Giữ ẩm vừa, để giá thể khô nhẹ giữa các lần tưới.",
                fertilizer = "NPK loãng, tăng đạm khi cây phát triển thân lá và chuyển sang phân cân đối trước ra hoa.",
                light = "Ánh sáng 50-70%, có che nắng và gió nhẹ.",
            ),
            diseases = emptyList(),
        ),
        OrchidVariety(
            name = "Lan Cattleya",
            scientificName = "Cattleya hybrid",
            aliases = listOf("cattleya", "lan cattleya"),
            description = "Lan Cattleya nổi bật với hoa lớn, màu sắc phong phú và hương thơm ở nhiều giống lai.",
            morphology = "Lan đa thân, có giả hành và rễ khí sinh; hoa lớn mọc từ bẹ lá.",
            origin = "Các vùng nhiệt đới Trung và Nam Mỹ.",
            uses = listOf("Trồng chậu sưu tầm và trang trí hoa cao cấp."),
            care = CareData(
                planting = "Chọn chậu và giá thể thật thoáng; đặt gốc cây cao hơn mặt giá thể, không vùi giả hành.",
                care = "Tưới khi giá thể khô gần hết, tăng thông gió và giữ cây khô trước đêm.",
                bestSeason = "Thay chậu khi chồi mới bắt đầu ra rễ.",
                water = "Tưới kỹ rồi để thoát nước hoàn toàn; tránh úng rễ.",
                fertilizer = "Bón phân lan loãng trong mùa tăng trưởng, ngừng hoặc giảm khi cây nghỉ.",
                light = "Ánh sáng sáng nhưng có lưới che, tránh nắng gắt trực tiếp.",
            ),
            diseases = emptyList(),
        ),
    )

    fun resultFor(variety: OrchidVariety): PlantScanResult = PlantScanResult(
        scientificName = variety.scientificName,
        commonNames = listOf(variety.name),
        confidence = 100.0,
        description = variety.description,
        plantId = variety.name.lowercase().replace(" ", "-"),
        family = "Họ Lan (Orchidaceae)",
        plantGroup = PlantGroup.ORNAMENTAL.key,
        morphology = variety.morphology,
        origin = variety.origin,
        uses = variety.uses,
        care = variety.care,
        commonDiseases = variety.diseases,
        detailAvailable = true,
        imageUrl = variety.imagePath,
    )

    fun enhance(result: PlantScanResult): PlantScanResult {
        val variety = find(result.scientificName, result.commonNames) ?: return result
        val guide = resultFor(variety)
        return result.copy(
            description = result.description ?: guide.description,
            commonNames = result.commonNames.ifEmpty { guide.commonNames },
            family = result.family ?: guide.family,
            plantGroup = result.plantGroup ?: guide.plantGroup,
            morphology = result.morphology ?: guide.morphology,
            origin = result.origin ?: guide.origin,
            uses = result.uses.ifEmpty { guide.uses },
            care = mergeCare(result.care, guide.care ?: variety.care),
            commonDiseases = result.commonDiseases.ifEmpty { guide.commonDiseases },
            detailAvailable = true,
            imageUrl = result.imageUrl ?: guide.imageUrl,
        )
    }

    private fun find(scientificName: String?, commonNames: List<String>): OrchidVariety? {
        val haystack = (listOfNotNull(scientificName) + commonNames).joinToString(" ").lowercase()
        return varieties.firstOrNull { variety ->
            variety.aliases.any { alias -> haystack.contains(alias) } ||
                haystack.contains(variety.name.lowercase())
        }
    }

    private fun mergeCare(scan: CareData?, guide: CareData): CareData = CareData(
        planting = scan?.planting ?: guide.planting,
        care = scan?.care ?: guide.care,
        bestSeason = scan?.bestSeason ?: guide.bestSeason,
        water = scan?.water ?: guide.water,
        fertilizer = scan?.fertilizer ?: guide.fertilizer,
        light = scan?.light ?: guide.light,
        citations = scan?.citations.orEmpty().ifEmpty { guide.citations },
        citationNote = scan?.citationNote ?: guide.citationNote,
    )

    private const val MOKARA_CARE = "Nhiệt độ thích hợp 25-30°C. Mokara cần ẩm độ vườn cao nhưng không chịu úng, vì vậy phải trồng thật thoáng. Độ thông thoáng đặc biệt quan trọng khi trồng chậu do hệ rễ phát triển mạnh. Với luống cắt cành, không để thân lan đè nhiều lên giá thể vì dễ gây thối cây. Làm cỏ và theo dõi cây thường xuyên. Sau 3-4 năm nên trồng lại."

    private const val MOKARA_PLANTING = "Trồng chậu: dùng chậu đất nhiều lỗ, kích thước khoảng 30 × 40 cm; cắm trụ cao 70-100 cm để tựa cây. Lót ít than cục lớn ở đáy và vỏ đậu phộng đã xử lý nấm bệnh ở phía trên. Buộc cây vào trụ bằng kẽm, siết nhẹ; rễ chỉ cần tiếp xúc nhẹ với giá thể hoặc có thể để rễ trần.\n\nTrồng luống: làm luống cao 15-20 cm, rộng khoảng 1 m. Dựng hai hàng cọc cao 1-1,5 m, cách nhau 30-50 cm, có nẹp tre ngang; buộc cây cách nhau khoảng 20 cm, chọn cành dài 40-50 cm có 2-3 tầng rễ. Trải vỏ đậu phộng chạm gốc, phủ xơ dừa đã ngâm nhưng không nén để giữ độ xốp. Khi cây mới trồng, che lưới hoặc tán lá dừa để còn 50-60% ánh sáng rồi gỡ dần khi cây khỏe."
}
