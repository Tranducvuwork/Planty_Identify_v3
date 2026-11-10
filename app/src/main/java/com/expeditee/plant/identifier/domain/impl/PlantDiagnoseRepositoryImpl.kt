package com.expeditee.plant.identifier.domain.impl

import android.content.Context
import android.net.Uri
import android.util.Log
import com.expeditee.plant.identifier.data.repository.DiseaseRepository
import com.expeditee.plant.identifier.data.repository.PlantDiseaseScanRepository
import com.expeditee.plant.identifier.data.upload.UploadImageFileResolver
import com.expeditee.plant.identifier.domain.repository.PlantDiagnoseRepository
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.model.CommonDisease
import com.expeditee.plant.identifier.model.DiagnosedDisease
import com.expeditee.plant.identifier.model.IdentifyMetadata
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.model.ProcessingErrorType
import com.expeditee.plant.identifier.model.ScanProcessingResult
import com.expeditee.plant.identifier.model.SimilarImage
import com.expeditee.plant.identifier.network.model.PlantDiseaseResult
import com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlantDiagnoseRepositoryImpl(
    private val context: Context,
    private val uploadImageFileResolver: UploadImageFileResolver,
    private val plantDiseaseScanRepository: PlantDiseaseScanRepository,
    private val diseaseRepository: DiseaseRepository,
    private val appConfigSettings: AppConfigSettings,
) : PlantDiagnoseRepository {

    companion object {
        private const val LOG_TAG = "TAG123123"
    }

    override suspend fun diagnose(
        imageUri: Uri,
        imageSource: String,
    ): ScanProcessingResult = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG, "[Diagnose] START imageUri=$imageUri, imageSource=$imageSource")
        val language = "vi"

        if (!context.isNetwork()) {
            Log.w(LOG_TAG, "[Diagnose] No network connection")
            return@withContext toErrorResult(
                IllegalStateException("No internet connection"),
            )
        }

        val sourceFile = uploadImageFileResolver.resolveToFile(imageUri)
            ?: return@withContext toErrorResult(IllegalArgumentException("Failed to resolve upload image file"))

        try {
            val apiResult = plantDiseaseScanRepository.diagnose(
                imageFile = sourceFile,
                language = language,
                enhanceCameraImage = shouldNormalizeUpload(imageSource),
            ).getOrElse { error ->
                Log.e(LOG_TAG, "[Diagnose] API repository failed: ${error.message}", error)
                return@withContext toErrorResult(error)
            }
            Log.d(
                LOG_TAG,
                "[Diagnose] API result isPlantImage=${apiResult.isPlantImage} isHealthy=${apiResult.isHealthy} diseaseId=${apiResult.diseaseId} diseaseName=${apiResult.diseaseName} confidence=${apiResult.confidence}"
            )
            convertApiResult(apiResult, imageSource, language)
        } finally {
            uploadImageFileResolver.cleanupResolvedFile(sourceFile)
        }
    }

    private fun shouldNormalizeUpload(imageSource: String): Boolean {
        return imageSource.equals("camera", ignoreCase = true) ||
            imageSource.equals("gallery", ignoreCase = true) ||
            imageSource.equals("upload", ignoreCase = true)
    }

    private suspend fun convertApiResult(
        apiResult: PlantDiseaseResult,
        imageSource: String,
        language: String,
    ): ScanProcessingResult {
        if (apiResult.isPlantImage == false) {
            Log.d(LOG_TAG, "[Diagnose] Final semantic => NOT_PLANT")
            return buildResult(
                imageSource = imageSource,
                isPlant = false,
                isHealthy = false,
                disease = null,
                probability = 0.0,
            )
        }

        if (apiResult.isHealthy) {
            Log.d(LOG_TAG, "[Diagnose] Final semantic => HEALTHY from apiResult.isHealthy")
            return createHealthyResult(imageSource)
        }

        val diseaseName = apiResult.diseaseName?.trim().orEmpty()
        if (diseaseName.isBlank()) {
            Log.d(LOG_TAG, "[Diagnose] Final semantic => HEALTHY because normalized diseaseName is blank")
            return createHealthyResult(imageSource)
        }

        val confidence = apiResult.confidence / 100.0
        val matchedDisease = findDisease(apiResult, language)
        val finalDisease = matchedDisease ?: buildApiFallbackDisease(apiResult)

        Log.d(
            LOG_TAG,
            "[Diagnose] Final semantic => DISEASED matched=${matchedDisease != null} finalDisease=${finalDisease.scientificName} confidence=$confidence"
        )

        return buildResult(
            imageSource = imageSource,
            isPlant = true,
            isHealthy = false,
            disease = finalDisease,
            probability = confidence,
        )
    }

    private suspend fun findDisease(apiResult: PlantDiseaseResult, language: String): CommonDisease? {
        apiResult.diseaseId?.let { diseaseId ->
            diseaseRepository.getDiseaseById(diseaseId, language)?.let {
                Log.d(LOG_TAG, "[Diagnose] Matched disease by id=$diseaseId language=$language")
                return it
            }
            if (language != "en") {
                diseaseRepository.getDiseaseById(diseaseId, "en")?.let {
                    Log.d(LOG_TAG, "[Diagnose] Matched disease by id=$diseaseId fallbackLanguage=en")
                    return it
                }
            }
        }

        val diseaseName = apiResult.diseaseName?.takeIf { it.isNotBlank() } ?: return null

        diseaseRepository.getDiseaseByScientificName(diseaseName, language)?.let {
            Log.d(LOG_TAG, "[Diagnose] Matched disease by scientificName='$diseaseName' language=$language")
            return it
        }
        diseaseRepository.getDiseaseByCommonName(diseaseName, language)?.let {
            Log.d(LOG_TAG, "[Diagnose] Matched disease by commonName='$diseaseName' language=$language")
            return it
        }
        if (language == "en") return null
        diseaseRepository.getDiseaseByScientificName(diseaseName, "en")?.let {
            Log.d(LOG_TAG, "[Diagnose] Matched disease by scientificName='$diseaseName' fallbackLanguage=en")
            return it
        }
        return diseaseRepository.getDiseaseByCommonName(diseaseName, "en")?.also {
            Log.d(LOG_TAG, "[Diagnose] Matched disease by commonName='$diseaseName' fallbackLanguage=en")
        }
    }

    private fun buildApiFallbackDisease(apiResult: PlantDiseaseResult): CommonDisease {
        val rawName = apiResult.diseaseName?.takeIf { it.isNotBlank() } ?: "Bệnh cây"
        val name = com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy.vietnameseName(rawName)
        Log.w(LOG_TAG, "[Diagnose] Building fallback disease from API payload name='$name'")

        return CommonDisease(
            id = apiResult.diseaseId,
            commonNames = listOf(name),
            scientificName = name,
            description = com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy.vietnameseDescription(
                rawName,
                apiResult.diseaseDetails ?: apiResult.analysisNotes,
            ),
            symptoms = fallbackSymptoms(name),
            causes = fallbackCauses(name),
            treatment = fallbackTreatment(name),
            prevention = fallbackPrevention(name),
            similarImages = apiResult.visualImages.orderedUrls().map { url ->
                SimilarImage(url = url, urlSmall = url, license = null)
            },
            severity = null,
            affectedPlants = null,
        )
    }

    private fun fallbackSymptoms(name: String): List<String> {
        val key = fold(name)
        return when {
            "vang la" in key -> listOf(
                "Lá vàng, nhạt màu hoặc vàng loang từng mảng.",
                "Cây chậm lớn, lá già rụng sớm nếu kéo dài.",
                "Có thể đi kèm rễ yếu hoặc đất quá ẩm.",
            )
            "dom" in key || "chay mep" in key || "kho mep" in key -> listOf(
                "Lá có đốm vàng, đốm nâu hoặc vết khô ở mép / đầu lá.",
                "Một số vết có quầng vàng, phần mô bệnh khô giòn.",
                "Vết có thể lan dần sang lá khác khi cây ẩm bí hoặc nắng gắt.",
            )
            else -> listOf(
                "Lá có dấu hiệu bất thường như đốm, vàng, khô mép hoặc sâu ăn.",
                "Cây có thể chậm phát triển nếu không xử lý sớm.",
            )
        }
    }

    private fun fallbackCauses(name: String): List<String> {
        val key = fold(name)
        return when {
            "vang la" in key -> listOf(
                "Tưới quá nhiều làm úng rễ hoặc đất thoát nước kém.",
                "Thiếu dinh dưỡng, thiếu vi lượng hoặc thiếu sáng.",
                "Cây bị thay đổi môi trường đột ngột.",
            )
            "dom" in key || "chay mep" in key || "kho mep" in key -> listOf(
                "Cháy nắng hoặc thừa sáng trực tiếp.",
                "Tưới quá nhiều, chậu bí nước làm rễ yếu.",
                "Nấm bệnh đốm lá hoặc lá bị tổn thương cơ học.",
                "Thiếu dinh dưỡng làm mô lá yếu, dễ cháy mép.",
            )
            else -> listOf(
                "Sâu hại, nấm bệnh hoặc điều kiện chăm sóc chưa phù hợp.",
                "Độ ẩm cao, thiếu thông gió hoặc tưới nước sai cách.",
            )
        }
    }

    private fun fallbackTreatment(name: String): List<String> {
        val key = fold(name)
        return when {
            "vang la" in key -> listOf(
                "Cắt bỏ lá vàng nặng và kiểm tra tình trạng rễ.",
                "Chỉ tưới khi mặt đất se khô; đảm bảo chậu có lỗ thoát nước.",
                "Bón phân hữu cơ hoặc NPK loãng, tránh bón quá liều.",
            )
            "dom" in key || "chay mep" in key || "kho mep" in key -> listOf(
                "Cắt bỏ vùng lá bị đốm / cháy mép nặng bằng kéo sạch.",
                "Chuyển cây sang nơi sáng tán xạ, tránh nắng gắt giữa trưa.",
                "Tưới vào gốc, tránh làm ướt lá buổi tối.",
                "Nếu vết lan nhanh, dùng dầu neem hoặc chế phẩm sinh học trị nấm theo hướng dẫn.",
            )
            else -> listOf(
                "Cách ly cây bệnh khỏi cây khỏe.",
                "Cắt bỏ phần bị hại và vệ sinh lá.",
                "Điều chỉnh nước, ánh sáng, thông gió rồi theo dõi 5–7 ngày.",
            )
        }
    }

    private fun fallbackPrevention(name: String): List<String> {
        val key = fold(name)
        return when {
            "vang la" in key -> listOf(
                "Giữ đất ẩm vừa, không để úng kéo dài.",
                "Bổ sung phân loãng định kỳ trong mùa sinh trưởng.",
                "Đặt cây ở nơi đủ sáng tán xạ.",
            )
            "dom" in key || "chay mep" in key || "kho mep" in key -> listOf(
                "Không để cây phơi nắng gắt ngay sau khi chuyển vị trí.",
                "Giữ khoảng cách thoáng giữa các chậu.",
                "Tránh tưới ướt lá vào chiều tối.",
                "Lau lá định kỳ để giảm bụi và mầm bệnh.",
            )
            else -> listOf(
                "Duy trì ánh sáng, nước và thông gió ổn định.",
                "Kiểm tra mặt dưới lá mỗi tuần để phát hiện sớm sâu bệnh.",
            )
        }
    }

    private fun fold(value: String?): String {
        val nfd = java.text.Normalizer.normalize(value.orEmpty().lowercase(), java.text.Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun createHealthyResult(
        imageSource: String,
    ): ScanProcessingResult.Success {
        return ScanProcessingResult.Success(
            label = "Cây khỏe",
            identifiedPlant = null,
            matchedPlant = null,
            confidence = 0.0f,
            metadata = IdentifyMetadata(
                mode = ModeScan.ONLINE,
                imageSource = imageSource,
            ),
            diagnosedDisease = DiagnosedDisease(
                isHealthy = true,
                disease = null,
                probability = 0.0,
                isPlant = true,
            )
        )
    }

    private fun buildResult(
        imageSource: String,
        isPlant: Boolean,
        isHealthy: Boolean,
        disease: CommonDisease?,
        probability: Double,
    ): ScanProcessingResult.Success {
        return ScanProcessingResult.Success(
            label = disease?.scientificName ?: "Cây khỏe",
            identifiedPlant = null,
            matchedPlant = null,
            confidence = probability.toFloat(),
            metadata = IdentifyMetadata(
                mode = ModeScan.ONLINE,
                imageSource = imageSource,
            ),
            diagnosedDisease = DiagnosedDisease(
                isHealthy = isHealthy,
                disease = disease,
                probability = probability,
                isPlant = isPlant,
                solution = disease?.let {
                    ThesisDiseaseCopy.buildSolution(
                        it.commonNames?.firstOrNull() ?: it.scientificName,
                    )
                },
            ),
        )
    }

    private fun toErrorResult(
        error: Throwable,
    ): ScanProcessingResult.Error {
        val message = error.message.orEmpty().lowercase()
        val type = when {
            error is java.net.UnknownHostException ||
                message.contains("unable to resolve host") ||
                message.contains("no address associated with hostname") -> ProcessingErrorType.NETWORK
            error is java.net.SocketTimeoutException ||
                error is java.io.InterruptedIOException ||
                message.contains("timeout") ||
                message.contains("timed out") -> ProcessingErrorType.TIMEOUT
            message.contains("internet") || message.contains("network") -> ProcessingErrorType.NETWORK
            message.contains("image") -> ProcessingErrorType.INVALID_IMAGE
            else -> ProcessingErrorType.SERVER
        }
        Log.e(LOG_TAG, "[Diagnose] Returning $type error: ${error.message}", error)
        return ScanProcessingResult.Error(type, error)
    }
}
