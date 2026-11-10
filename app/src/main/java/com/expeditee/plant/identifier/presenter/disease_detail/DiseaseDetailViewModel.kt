package com.expeditee.plant.identifier.presenter.disease_detail

import android.app.Application
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.DiseaseRepository
import com.expeditee.plant.identifier.model.CommonDisease
import com.expeditee.plant.identifier.model.Solution
import com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiseaseDetailViewModel(
    application: Application,
    private val diseaseRepository: DiseaseRepository,
) : BaseViewModel<DiseaseDetailUiState>(application) {
    private val _state = MutableStateFlow<DiseaseDetailUiState>(DiseaseDetailUiState.Loading)
    val state: StateFlow<DiseaseDetailUiState> = _state.asStateFlow()

    private var currentDisease: CommonDisease? = null

    override fun onState(state: DiseaseDetailUiState) {
        when (state) {
            is DiseaseDetailUiState.LoadDisease -> loadDisease(
                diseaseId = state.diseaseId,
                imagePath = state.imagePath,
                probability = state.probability,
                solution = state.solution,
                fallbackDiseaseName = state.fallbackDiseaseName,
                fallbackDiseaseDescription = state.fallbackDiseaseDescription,
            )
            else -> emitUiState(state)
        }
    }

    private fun loadDisease(
        diseaseId: Int,
        imagePath: String?,
        probability: Int,
        solution: Solution? = null,
        fallbackDiseaseName: String? = null,
        fallbackDiseaseDescription: String? = null,
    ) {
        emitUiState(DiseaseDetailUiState.Loading)
        launchBlock(Dispatchers.IO) {
            if (diseaseId < 0) {
                val fallbackDisease = buildFallbackDisease(
                    fallbackDiseaseName = fallbackDiseaseName,
                    fallbackDiseaseDescription = fallbackDiseaseDescription,
                )
                if (fallbackDisease != null) {
                    emitUiState(
                        toContentState(
                            disease = fallbackDisease,
                            imagePath = imagePath,
                            probability = probability,
                            otherDiseases = emptyList(),
                            solution = solution,
                        ),
                    )
                    return@launchBlock
                }
                emitUiState(DiseaseDetailUiState.Error(getString(R.string.disease_detail_error_not_found)))
                return@launchBlock
            }
            val language = "vi"
            val disease = diseaseRepository.getDiseaseById(diseaseId, language)
                ?: diseaseRepository.getDiseaseById(diseaseId, "en")
            if (disease == null) {
                val fallbackDisease = buildFallbackDisease(
                    fallbackDiseaseName = fallbackDiseaseName,
                    fallbackDiseaseDescription = fallbackDiseaseDescription,
                )
                if (fallbackDisease != null) {
                    emitUiState(
                        toContentState(
                            disease = fallbackDisease,
                            imagePath = imagePath,
                            probability = probability,
                            otherDiseases = emptyList(),
                            solution = solution,
                        ),
                    )
                    return@launchBlock
                }
                emitUiState(DiseaseDetailUiState.Error(getString(R.string.disease_detail_error_not_found)))
                return@launchBlock
            }
            currentDisease = disease
            val otherDiseases = diseaseRepository.getOtherDiseases(diseaseId, 10, language)
            emitUiState(toContentState(disease, imagePath, probability, otherDiseases, solution))
        }
    }

    private fun toContentState(
        disease: CommonDisease,
        imagePath: String?,
        probability: Int,
        otherDiseases: List<CommonDisease>,
        solution: Solution? = null,
    ): DiseaseDetailUiState.Content {
        val treatmentGroups = vietnameseTreatment(disease.treatment.orEmpty(), disease)
        val solutionVn = solution?.takeIf {
            !it.title.isNullOrBlank() && !looksEnglishText(it.title)
        } ?: ThesisDiseaseCopy.buildSolution(
            disease.commonNames?.firstOrNull() ?: disease.scientificName,
        )
        return DiseaseDetailUiState.Content(
            uiModel = DiseaseDetailUiModel(
                imagePath = imagePath,
                probability = probability,
                isFromDisease = true,
                basicInfo = DiseaseBasicInfoUiModel(
                    displayName = resolveDiseaseName(disease),
                    scientificName = "Tên khoa học: " + (
                        disease.scientificName?.takeIf { it.isNotBlank() && it != resolveDiseaseName(disease) }
                            ?: getString(R.string.not_available_short)
                        ),
                    otherNames = disease.otherNames
                        ?.map { ThesisDiseaseCopy.vietnameseName(it) }
                        ?.distinct()
                        ?.joinToString(", ")
                        .orEmpty()
                        .ifBlank { getString(R.string.not_available_short) },
                ),
                overview = DiseaseOverviewUiModel(
                    description = ThesisDiseaseCopy.vietnameseDescription(
                        disease.commonNames?.firstOrNull() ?: disease.scientificName.orEmpty(),
                        disease.description,
                    ),
                ),
                treatment = treatmentGroups,
                similarImages = disease.similarImages?.map { image ->
                    SimilarImageUiModel(
                        url = image.url,
                        urlSmall = image.urlSmall,
                        licenseName = image.license?.name,
                        licenseUrl = image.license?.url,
                    )
                } ?: emptyList(),
                otherDiseases = otherDiseases.mapNotNull { other ->
                    val otherId = other.id ?: return@mapNotNull null
                    OtherDiseaseUiModel(
                        id = otherId,
                        displayName = resolveDiseaseName(other),
                        imageUrl = other.similarImages?.firstOrNull()?.urlSmall
                            ?: other.similarImages?.firstOrNull()?.url,
                    )
                },
                symptoms = vietnameseListOrFallback(
                    source = disease.symptoms,
                    fallback = defaultSymptoms(disease),
                ),
                causes = vietnameseListOrFallback(
                    source = disease.causes,
                    fallback = defaultCauses(disease),
                ),
                prevention = vietnameseListOrFallback(
                    source = disease.prevention,
                    fallback = defaultPrevention(disease),
                ),
                videos = defaultVideos(resolveDiseaseName(disease), disease.affectedPlants.orEmpty()),
                visuals = buildVisuals(disease),
                solution = solutionVn,
            ),
        )
    }

    private fun buildVisuals(disease: CommonDisease): DiseaseVisualUiModel {
        val images = disease.similarImages.orEmpty()
            .mapNotNull { it.url ?: it.urlSmall }
            .filter { it.isNotBlank() }
            .distinct()
        val fallback = fallbackVisuals(resolveDiseaseName(disease))
        val pest = images.getOrNull(0) ?: fallback.pestImageUrl
        val symptom = images.firstOrNull { it != pest } ?: fallback.symptomImageUrl
        val healthy = images.firstOrNull { it != pest && it != symptom && isHealthyImageUrl(it) }
            ?: fallback.healthyImageUrl
        val diseased = images.firstOrNull { it != pest && it != symptom && it != healthy }
            ?: fallback.diseasedImageUrl
        return DiseaseVisualUiModel(
            pestImageUrl = pest,
            symptomImageUrl = symptom,
            healthyImageUrl = healthy,
            diseasedImageUrl = diseased,
        )
    }

    private fun isHealthyImageUrl(url: String): Boolean {
        val key = fold(url)
        return "healthy" in key || "normal" in key || "khoe" in key
    }

    private fun fallbackVisuals(diseaseName: String): DiseaseVisualUiModel {
        val key = fold(diseaseName)
        val aphid = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Aphis_fabae01.jpg?width=640"
        val leafSpot = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Early_blight_on_tomato_leaves_(7871930010).jpg?width=640"
        val leafScorch = "https://commons.wikimedia.org/wiki/Special:Redirect/file/OleanderLeafScorch.jpg?width=640"
        val healthyLeaf = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Healthy_green_plant.jpg?width=640"
        return when {
            "rep" in key || "sau" in key || "aphid" in key || "mite" in key || "mealy" in key ->
                DiseaseVisualUiModel(
                    pestImageUrl = aphid,
                    symptomImageUrl = leafScorch,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafSpot,
                )
            "vang la" in key || "yellow" in key || "chlorosis" in key ->
                DiseaseVisualUiModel(
                    pestImageUrl = aphid,
                    symptomImageUrl = leafScorch,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafSpot,
                )
            else ->
                DiseaseVisualUiModel(
                    pestImageUrl = aphid,
                    symptomImageUrl = leafSpot,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafScorch,
                )
        }
    }

    private fun vietnameseListOrFallback(source: List<String>?, fallback: List<String>): List<String> {
        val clean = source.orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() && !looksEnglishText(it) }
        return clean.ifEmpty { fallback }
    }

    private fun defaultSymptoms(disease: CommonDisease): List<String> {
        val key = foldDisease(disease)
        return when {
            "leaf miner" in key || "duc la" in key ->
                listOf("Lá có đường ngoằn ngoèo màu trắng hoặc nâu.", "Mô lá bị rỗng, lá non chậm phát triển.", "Cây yếu nếu mật độ sâu cao.")
            "aphid" in key || "mealy" in key || "mite" in key || "rep" in key || "nhen" in key ->
                listOf("Chồi non xoăn, lá biến dạng.", "Mặt dưới lá có côn trùng nhỏ hoặc mảng trắng.", "Lá dính mật, có thể xuất hiện nấm bồ hóng.")
            "root rot" in key || "thoi re" in key || "wilt" in key ->
                listOf("Cây héo dù đất còn ẩm.", "Lá vàng từ gốc, rụng dần.", "Rễ nâu đen, mềm hoặc có mùi.")
            "powdery" in key || "phan trang" in key ->
                listOf("Lá có lớp bột trắng như phấn.", "Lá cong, vàng hoặc khô mép.", "Bệnh lan nhanh khi tán cây bí và ẩm.")
            "yellow" in key || "vang la" in key || "chlorosis" in key ->
                listOf("Lá chuyển vàng, gân lá có thể còn xanh.", "Cây chậm lớn, chồi non yếu.", "Một số lá già rụng sớm.")
            else ->
                listOf("Lá xuất hiện đốm, vệt cháy hoặc vùng biến màu.", "Mô lá bệnh lan rộng nếu ẩm kéo dài.", "Cây giảm sinh trưởng, lá non yếu hơn bình thường.")
        }
    }

    private fun defaultCauses(disease: CommonDisease): List<String> {
        val key = foldDisease(disease)
        return when {
            "aphid" in key || "mealy" in key || "mite" in key || "rep" in key || "nhen" in key ->
                listOf("Côn trùng chích hút phát triển mạnh khi cây rậm và thiếu thông thoáng.", "Cây yếu do thiếu nước hoặc thừa đạm dễ bị tấn công.")
            "root rot" in key || "thoi re" in key ->
                listOf("Đất úng, chậu thoát nước kém.", "Tưới quá nhiều hoặc giá thể bí làm rễ thiếu oxy.")
            "yellow" in key || "vang la" in key || "chlorosis" in key ->
                listOf("Thiếu dinh dưỡng, rễ yếu hoặc đất úng.", "Ánh sáng không phù hợp hoặc cây bị sốc sau khi thay chậu.")
            else ->
                listOf("Nấm hoặc vi khuẩn phát triển khi lá ẩm lâu.", "Tán cây quá rậm, thiếu nắng và thiếu thông gió.", "Mầm bệnh có thể lây từ lá bệnh, đất bẩn hoặc dụng cụ cắt tỉa.")
        }
    }

    private fun defaultPrevention(disease: CommonDisease): List<String> {
        val key = foldDisease(disease)
        return when {
            "root rot" in key || "thoi re" in key ->
                listOf("Dùng chậu có lỗ thoát nước và đất tơi xốp.", "Chỉ tưới khi mặt đất se khô.", "Không để chậu ngâm nước lâu.")
            "aphid" in key || "mealy" in key || "mite" in key || "rep" in key || "nhen" in key ->
                listOf("Kiểm tra mặt dưới lá mỗi tuần.", "Cách ly cây mới mua vài ngày trước khi đặt chung.", "Giữ cây khỏe, tránh bón thừa đạm.")
            else ->
                listOf("Tỉa lá già và giữ tán cây thông thoáng.", "Tưới vào gốc, tránh làm ướt lá buổi tối.", "Loại bỏ lá bệnh sớm và vệ sinh dụng cụ cắt tỉa.")
        }
    }

    private fun defaultVideos(displayName: String, affectedPlants: List<String>): List<DiseaseVideoUiModel> {
        val plant = affectedPlants.firstOrNull()?.takeIf { it.isNotBlank() }.orEmpty()
        val disease = displayName.ifBlank { "sâu bệnh cây trồng" }
        val queryBase = listOf(disease, plant).filter { it.isNotBlank() }.joinToString(" ")
        return listOf(
            DiseaseVideoUiModel(
                title = "Video hướng dẫn xử lý $disease",
                url = youtubeSearch("cách xử lý $queryBase"),
            ),
            DiseaseVideoUiModel(
                title = "Video nhận diện sâu bệnh",
                url = youtubeSearch("nhận diện $queryBase triệu chứng"),
            ),
        )
    }

    private fun youtubeSearch(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return "https://www.youtube.com/results?search_query=$encoded"
    }

    private fun fold(value: String?): String {
        return java.text.Normalizer.normalize(
            value.orEmpty().lowercase(),
            java.text.Normalizer.Form.NFD,
        ).replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun foldDisease(disease: CommonDisease): String {
        return fold(
            listOfNotNull(
                disease.scientificName,
                disease.commonNames?.joinToString(" "),
                disease.otherNames?.joinToString(" "),
            ).joinToString(" "),
        )
    }

    private fun vietnameseTreatment(
        treatments: List<String>,
        disease: CommonDisease,
    ): DiseaseTreatmentUiModel {
        val hint = ThesisDiseaseCopy.treatmentHint(
            disease.commonNames?.firstOrNull() ?: disease.scientificName,
        )
        if (treatments.isEmpty() || treatments.all { looksEnglishText(it) }) {
            return DiseaseTreatmentUiModel(
                biological = hint.steps,
                chemical = emptyList(),
                fallback = emptyList(),
            )
        }
        return splitTreatmentGroups(treatments)
    }

    private fun looksEnglishText(text: String?): Boolean {
        val letters = text.orEmpty().filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.count { it.code < 128 }.toFloat() / letters.length > 0.9f &&
            text.orEmpty().none {
                it in "ăâêôơưđáàảãạéèẻẽẹíìỉĩịóòỏõọúùủũụýỳỷỹỵĂÂÊÔƠƯĐ"
            }
    }

    private fun resolveDiseaseName(disease: CommonDisease): String {
        val raw = disease.commonNames?.firstOrNull()
            ?: disease.scientificName
            ?: getString(R.string.disease_detail_unknown)
        return com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy.vietnameseName(raw)
    }

    private fun splitTreatmentGroups(treatments: List<String>): DiseaseTreatmentUiModel {
        if (treatments.isEmpty()) {
            return DiseaseTreatmentUiModel(
                biological = emptyList(), chemical = emptyList(), fallback = emptyList()
            )
        }
        val biological = treatments.filter {
            it.contains(
                "organic", ignoreCase = true
            ) || it.contains("biological", ignoreCase = true)
        }
        val chemical = treatments.filter {
            it.contains(
                "chemical", ignoreCase = true
            ) || it.contains("fungicide", ignoreCase = true) || it.contains(
                "pesticide", ignoreCase = true
            )
        }
        val fallback = if (biological.isEmpty() && chemical.isEmpty()) treatments else emptyList()
        return DiseaseTreatmentUiModel(
            biological = biological, chemical = chemical, fallback = fallback
        )
    }

    private fun emitUiState(state: DiseaseDetailUiState) {
        _state.value = state
    }

    private fun buildFallbackDisease(
        fallbackDiseaseName: String?,
        fallbackDiseaseDescription: String?,
    ): CommonDisease? {
        val raw = fallbackDiseaseName?.trim().orEmpty()
        if (raw.isBlank()) return null
        val name = com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy.vietnameseName(raw)
        return CommonDisease(
            id = null,
            commonNames = listOf(name),
            scientificName = name,
            description = com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy.vietnameseDescription(
                raw,
                fallbackDiseaseDescription,
            ),
        )
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}


sealed class DiseaseDetailUiState : BaseViewModel.State {
    data class LoadDisease(
        val diseaseId: Int,
        val imagePath: String?,
        val probability: Int,
        val solution: Solution? = null,
        val fallbackDiseaseName: String? = null,
        val fallbackDiseaseDescription: String? = null,
    ) : DiseaseDetailUiState()
    data object Loading : DiseaseDetailUiState()
    data class Content(val uiModel: DiseaseDetailUiModel) : DiseaseDetailUiState()
    data class Error(val message: String) : DiseaseDetailUiState()
}
