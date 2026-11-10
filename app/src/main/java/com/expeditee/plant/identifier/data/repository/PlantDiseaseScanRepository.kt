package com.expeditee.plant.identifier.data.repository

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.expeditee.plant.identifier.network.PlantBackendApiService
import com.expeditee.plant.identifier.network.model.DiseaseVisualImages
import com.expeditee.plant.identifier.network.model.PlantDiseaseResult
import com.expeditee.plant.identifier.data.upload.PlantImageUploadHelper
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.Normalizer

class PlantDiseaseScanRepository(
    private val apiService: PlantBackendApiService,
    private val vietnameseTranslationRepository: VietnameseTranslationRepository,
) {
    companion object {
        private const val TAG = "TAG123123"
        private val INVALID_DISEASE_NAMES = setOf(
            "Unknown", "Unknown Disease", "Unidentified", "null", "",
        )
    }

    suspend fun diagnose(
        imageFile: File,
        language: String? = null,
        enhanceCameraImage: Boolean = false,
    ): Result<PlantDiseaseResult> {
        val lang = language?.takeIf { it.isNotBlank() } ?: "vi"

        Log.d(
            TAG,
            "diagnose start file=${imageFile.absolutePath} exists=${imageFile.exists()} size=${imageFile.length()} lang=$lang enhanceCamera=$enhanceCameraImage"
        )

        if (!PlantImageUploadHelper.validate(imageFile)) {
            Log.e(TAG, "diagnose invalid image file=${imageFile.absolutePath}")
            return Result.failure(IllegalArgumentException("Invalid image file"))
        }

        val prepared = PlantImageUploadHelper.prepareUpload(
            imageFile = imageFile,
            enhanceCameraImage = enhanceCameraImage,
        )
        val visualDiseaseName = detectVisualDiseaseName(prepared.file)
        Log.d(
            TAG,
            "diagnose prepared upload file=${prepared.file.absolutePath} compressed=${prepared.compressed} size=${prepared.file.length()}"
        )

        return try {
            val multipart = PlantImageUploadHelper.createImagePart(prepared.file)
            val languagePart = lang.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.diagnoseDisease(multipart, languagePart)

            Log.d(
                TAG,
                "diagnose response status=${response.status} hasData=${response.data != null}"
            )

            if (!response.status.equals("success", ignoreCase = true)) {
                Log.e(TAG, "diagnose backend status failure status=${response.status}")
                return Result.failure(IllegalStateException("Diagnose failed: status=${response.status}"))
            }

            val data = response.data ?: run {
                if (visualDiseaseName != null) {
                    Log.d(TAG, "diagnose response data is null but visual symptom='$visualDiseaseName'")
                    return Result.success(symptomResult(visualDiseaseName, 76.0))
                }
                Log.d(TAG, "diagnose response data is null => healthy")
                return Result.success(healthyResult())
            }

            if (data.isPlantImage == false) {
                Log.d(TAG, "diagnose result is not a plant image")
                return Result.success(
                    PlantDiseaseResult(
                        isHealthy = true,
                        isPlantImage = false,
                        diseaseId = null,
                        diseaseName = null,
                        matchedDiseaseName = null,
                        confidence = 0.0,
                        analysisNotes = vietnameseTranslationRepository.translateDisplayText(data.analysisNotes),
                        diseaseDetails = vietnameseTranslationRepository.translateDisplayText(
                            normalizeDiseaseDetails(data.diseaseDetails),
                        ),
                        isKnownDisease = data.isKnownDisease,
                    ),
                )
            }

            val isHealthy = data.isHealthy ?: true
            val confidence = (data.confidence ?: 0).toDouble()
            val diseaseId = data.diseaseId?.takeIf { it > 0 }
            val diseaseName = data.diseaseName?.trim().orEmpty()
            val matchedDiseaseName = data.matchedDiseaseName?.trim().orEmpty()
            val rawDiseaseDetails = normalizeDiseaseDetails(data.diseaseDetails)
            val parsedVisualImages = extractVisualImages(data.diseaseDetails, diseaseName, matchedDiseaseName)
            val symptomText = listOf(
                diseaseName,
                matchedDiseaseName,
                data.analysisNotes.orEmpty(),
                rawDiseaseDetails.orEmpty(),
            ).joinToString(" ")
            val textDiseaseName = detectTextDiseaseName(symptomText)
            val hasYellowLeafSymptom = visualDiseaseName == "Vàng lá" ||
                textDiseaseName == "Vàng lá" ||
                containsYellowLeafSymptom(symptomText)
            val resolvedDiseaseName = when {
                visualDiseaseName != null -> visualDiseaseName
                textDiseaseName != null -> textDiseaseName
                isValidDiseaseName(matchedDiseaseName) -> matchedDiseaseName
                isValidDiseaseName(diseaseName) -> diseaseName
                hasYellowLeafSymptom -> "Vàng lá"
                else -> ""
            }
            val isInvalidName = !isValidDiseaseName(resolvedDiseaseName)
            val isDiseased = visualDiseaseName != null ||
                hasYellowLeafSymptom ||
                (!isHealthy &&
                    !isInvalidName &&
                    confidence >= 65.0 &&
                    (diseaseId != null || resolvedDiseaseName.isNotBlank()))

            Log.d(
                TAG,
                "diagnose semantic isHealthy=$isHealthy confidence=$confidence diseaseId=$diseaseId diseaseName='$diseaseName' matchedDiseaseName='$matchedDiseaseName' resolvedDiseaseName='$resolvedDiseaseName' isDiseased=$isDiseased diseaseDetailsType=${describeJsonType(data.diseaseDetails)}"
            )

            if (!isDiseased) {
                Log.d(TAG, "diagnose treated as healthy after semantic normalization")
                return Result.success(
                    healthyResult(
                        analysisNotes = vietnameseTranslationRepository.translateDisplayText(data.analysisNotes),
                    ),
                )
            }

            Log.d(TAG, "diagnose treated as diseased with resolvedDiseaseName='$resolvedDiseaseName'")
            val localizedDiseaseName = if (textDiseaseName != null) {
                textDiseaseName
            } else {
                vietnameseTranslationRepository
                    .translateDisplayText(resolvedDiseaseName)
                    ?.takeIf { it.isNotBlank() }
                    ?: resolvedDiseaseName
            }
            val localizedMatchedDiseaseName = vietnameseTranslationRepository
                .translateDisplayText(matchedDiseaseName)
                ?.takeIf { it.isNotBlank() }
            val localizedDiseaseDetails = vietnameseTranslationRepository
                .translateDisplayText(rawDiseaseDetails)
                ?.takeIf { it.isNotBlank() && !isThinDiagnosisText(it) }
                ?: fetchDiagnosisAdvice(localizedDiseaseName, data.analysisNotes, rawDiseaseDetails)
                ?: fallbackDiagnosisDetails(localizedDiseaseName)
            Result.success(
                PlantDiseaseResult(
                    isHealthy = false,
                    isPlantImage = data.isPlantImage,
                    diseaseId = diseaseId,
                    diseaseName = localizedDiseaseName.takeIf { it.isNotBlank() },
                    matchedDiseaseName = localizedMatchedDiseaseName,
                    confidence = if (visualDiseaseName != null) confidence.coerceAtLeast(76.0) else confidence,
                    analysisNotes = vietnameseTranslationRepository.translateDisplayText(data.analysisNotes),
                    diseaseDetails = localizedDiseaseDetails,
                    isKnownDisease = data.isKnownDisease,
                    visualImages = parsedVisualImages.takeIf { it.orderedUrls().isNotEmpty() }
                        ?: fallbackVisualImages(localizedDiseaseName),
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "diagnose error: ${e.message}", e)
            Result.failure(e)
        } finally {
            Log.d(
                TAG,
                "diagnose cleanup original=${imageFile.absolutePath} prepared=${prepared.file.absolutePath}"
            )
            PlantImageUploadHelper.cleanup(imageFile, prepared.file)
        }
    }

    private fun healthyResult(analysisNotes: String? = null) = PlantDiseaseResult(
        isHealthy = true,
        isPlantImage = true,
        diseaseId = null,
        diseaseName = null,
        matchedDiseaseName = null,
        confidence = 100.0,
        analysisNotes = analysisNotes,
        diseaseDetails = null,
        isKnownDisease = null,
    )

    private fun symptomResult(name: String, confidence: Double) = PlantDiseaseResult(
        isHealthy = false,
        isPlantImage = true,
        diseaseId = null,
        diseaseName = name,
        matchedDiseaseName = null,
        confidence = confidence,
        analysisNotes = "Ảnh có dấu hiệu $name.",
        diseaseDetails = when (name) {
            "Vàng lá" -> fallbackDiagnosisDetails(name)
            "Đốm lá / cháy mép lá" -> fallbackDiagnosisDetails(name)
            else -> "Ảnh có dấu hiệu bất thường trên lá."
        },
        isKnownDisease = true,
        visualImages = fallbackVisualImages(name),
    )

    private fun extractVisualImages(
        diseaseDetails: JsonElement?,
        diseaseName: String?,
        matchedDiseaseName: String?,
    ): DiseaseVisualImages {
        if (diseaseDetails == null || diseaseDetails.isJsonNull) {
            return fallbackVisualImages(diseaseName?.takeIf { it.isNotBlank() } ?: matchedDiseaseName)
        }

        val pest = firstUrlForKeys(
            diseaseDetails,
            "pestImage", "pest_image", "pestImages", "pest_images",
            "insectImage", "insect_image", "insectImages", "insect_images",
            "pathogenImage", "pathogen_image", "pathogenImages", "pathogen_images",
            "pest", "insect", "pathogen",
        )
        val symptom = firstUrlForKeys(
            diseaseDetails,
            "symptomImage", "symptom_image", "symptomImages", "symptom_images",
            "damageImage", "damage_image", "damageImages", "damage_images",
            "affectedImage", "affected_image", "affectedImages", "affected_images",
            "symptom", "symptoms", "damage",
        )
        val healthy = firstUrlForKeys(
            diseaseDetails,
            "healthyImage", "healthy_image", "healthyImages", "healthy_images",
            "healthyPlantImage", "healthy_plant_image", "healthyPlantImages", "healthy_plant_images",
            "healthy", "normalImage", "normal_image",
        )
        val diseased = firstUrlForKeys(
            diseaseDetails,
            "diseasedImage", "diseased_image", "diseasedImages", "diseased_images",
            "diseaseImage", "disease_image", "diseaseImages", "disease_images",
            "infectedImage", "infected_image", "infectedImages", "infected_images",
            "comparisonDiseaseImage", "comparison_disease_image",
        )

        val generic = collectUrls(diseaseDetails)
            .filterNot { it.startsWith("content://") || it.startsWith("/") }
            .distinct()
        val fallback = fallbackVisualImages(diseaseName?.takeIf { it.isNotBlank() } ?: matchedDiseaseName)
        val resolvedPest = pest ?: generic.getOrNull(0) ?: fallback.pestImageUrl
        val resolvedSymptom = symptom ?: generic.firstOrNull { it != resolvedPest } ?: fallback.symptomImageUrl
        val resolvedHealthy = healthy ?: generic.firstOrNull { url ->
            (fold(url).contains("healthy") || fold(url).contains("normal")) &&
                url != resolvedPest &&
                url != resolvedSymptom
        } ?: fallback.healthyImageUrl
        val resolvedDiseased = diseased ?: generic.firstOrNull {
            it != resolvedPest && it != resolvedSymptom && it != resolvedHealthy
        } ?: fallback.diseasedImageUrl
        val visual = DiseaseVisualImages(
            pestImageUrl = resolvedPest,
            symptomImageUrl = resolvedSymptom,
            healthyImageUrl = resolvedHealthy,
            diseasedImageUrl = resolvedDiseased,
        )
        return visual.takeIf { it.orderedUrls().isNotEmpty() }
            ?: fallback
    }

    private fun firstUrlForKeys(element: JsonElement, vararg keys: String): String? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        keys.forEach { key ->
            obj.findValueIgnoreCase(key)?.let { candidate ->
                collectUrls(candidate).firstOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun JsonObject.findValueIgnoreCase(key: String): JsonElement? {
        entrySet().firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.let { return it }
        return null
    }

    private fun collectUrls(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        if (element.isJsonPrimitive) {
            val value = runCatching { element.asString }.getOrNull()?.trim().orEmpty()
            return if (isImageUrl(value)) listOf(value) else emptyList()
        }
        if (element.isJsonArray) {
            return element.asJsonArray.flatMap { collectUrls(it) }.distinct()
        }
        if (element.isJsonObject) {
            return element.asJsonObject.entrySet().flatMap { collectUrls(it.value) }.distinct()
        }
        return emptyList()
    }

    private fun isImageUrl(value: String): Boolean {
        val lower = value.lowercase()
        return (lower.startsWith("http://") || lower.startsWith("https://")) &&
            (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") ||
                lower.contains(".webp") || lower.contains("images") || lower.contains("image"))
    }

    private fun fallbackVisualImages(diseaseName: String?): DiseaseVisualImages {
        val key = fold(diseaseName.orEmpty())
        val pest = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Aphis_fabae01.jpg?width=640"
        val leafSpot = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Early_blight_on_tomato_leaves_(7871930010).jpg?width=640"
        val leafScorch = "https://commons.wikimedia.org/wiki/Special:Redirect/file/OleanderLeafScorch.jpg?width=640"
        val healthyLeaf = "https://commons.wikimedia.org/wiki/Special:Redirect/file/Healthy_green_plant.jpg?width=640"
        return when {
            key.contains("rep") || key.contains("sau") || key.contains("aphid") || key.contains("mite") || key.contains("mealy") ->
                DiseaseVisualImages(
                    pestImageUrl = pest,
                    symptomImageUrl = leafScorch,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafSpot,
                )
            key.contains("dom") || key.contains("spot") || key.contains("nam") || key.contains("fung") ->
                DiseaseVisualImages(
                    pestImageUrl = pest,
                    symptomImageUrl = leafSpot,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafScorch,
                )
            key.contains("vang la") || key.contains("yellow") || key.contains("chlorosis") ->
                DiseaseVisualImages(
                    pestImageUrl = pest,
                    symptomImageUrl = leafScorch,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafSpot,
                )
            else ->
                DiseaseVisualImages(
                    pestImageUrl = pest,
                    symptomImageUrl = leafSpot,
                    healthyImageUrl = healthyLeaf,
                    diseasedImageUrl = leafScorch,
                )
        }
    }

    private fun normalizeDiseaseDetails(diseaseDetails: JsonElement?): String? {
        if (diseaseDetails == null || diseaseDetails.isJsonNull) return null
        if (diseaseDetails.isJsonPrimitive) {
            return diseaseDetails.asString.takeIf { it.isNotBlank() }
        }
        if (diseaseDetails.isJsonObject) {
            val obj = diseaseDetails.asJsonObject
            val prioritizedKeys = listOf("description", "summary", "details", "content", "title")
            prioritizedKeys.forEach { key ->
                obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }?.let { return it }
            }

            val flattened = prioritizedKeys.mapNotNull { key ->
                obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
            }.distinct()
            if (flattened.isNotEmpty()) return flattened.joinToString("\n")
        }
        return diseaseDetails.toString()
    }

    private fun containsYellowLeafSymptom(text: String): Boolean {
        val key = fold(text)
        return "vang la" in key ||
            "la vang" in key ||
            "la ua" in key ||
            "ua vang" in key ||
            "dom vang" in key ||
            "vang mep la" in key ||
            "mat diep luc" in key ||
            "yellow leaf" in key ||
            "yellow leaves" in key ||
            "yellowing" in key ||
            "yellow spot" in key ||
            "yellow spots" in key ||
            "chlorosis" in key ||
            "interveinal chlorosis" in key ||
            "leaf yellow" in key
    }

    private fun detectTextDiseaseName(text: String): String? {
        val key = fold(text)
        return when {
            "chay mep" in key ||
                "kho mep" in key ||
                "brown spot" in key ||
                "brown spots" in key ||
                "necrotic" in key ||
                "dry edge" in key ||
                "leaf scorch" in key ||
                "dom nau" in key ||
                "dom la" in key -> "Đốm lá / cháy mép lá"
            containsYellowLeafSymptom(text) -> "Vàng lá"
            else -> null
        }
    }

    private fun isValidDiseaseName(value: String?): Boolean {
        val name = value?.trim().orEmpty()
        if (name.isBlank()) return false
        return INVALID_DISEASE_NAMES.none { name.equals(it, ignoreCase = true) }
    }

    private fun isThinDiagnosisText(value: String): Boolean {
        val key = fold(value)
        return value.length < 80 ||
            key == "unknown" ||
            key == "unknown disease" ||
            key == "benh cay chua xac dinh"
    }

    private suspend fun fetchDiagnosisAdvice(
        diseaseName: String,
        analysisNotes: String?,
        rawDiseaseDetails: String?,
    ): String? {
        val prompt = """
            Bạn là chuyên gia bệnh cây. Hãy bổ sung dữ liệu chẩn đoán bằng tiếng Việt cho app.
            Chỉ trả về văn bản tiếng Việt, không markdown bảng.
            Tên vấn đề: $diseaseName
            Ghi chú model: ${analysisNotes.orEmpty()}
            Dữ liệu server: ${rawDiseaseDetails.orEmpty()}

            Cần có đủ các mục:
            Triệu chứng:
            Nguyên nhân có thể:
            Cách xử lý:
            Hướng dẫn chăm sóc cơ bản:
        """.trimIndent()
        return runCatching {
            apiService.sendChat(prompt, "vi").data?.answer?.trim()
        }.onFailure {
            Log.w(TAG, "fetchDiagnosisAdvice failed for '$diseaseName': ${it.message}")
        }.getOrNull()
            ?.takeIf { it.isNotBlank() && !isThinDiagnosisText(it) }
    }

    private fun fallbackDiagnosisDetails(diseaseName: String?): String {
        val name = diseaseName?.takeIf { it.isNotBlank() } ?: "Bệnh cây"
        val key = fold(name)
        return if ("vang la" in key) {
            """
            Vàng lá.
            Triệu chứng: Lá chuyển vàng, nhạt màu hoặc vàng loang từng mảng; cây có thể chậm lớn, rễ yếu.
            Nguyên nhân có thể: Tưới quá nhiều gây úng rễ, thiếu dinh dưỡng, thiếu sáng hoặc thay đổi môi trường đột ngột.
            Cách xử lý: Kiểm tra lỗ thoát nước của chậu, chỉ tưới khi mặt đất se khô, cắt lá vàng nặng và bón phân hữu cơ hoặc NPK loãng.
            Hướng dẫn chăm sóc cơ bản: Đặt cây nơi sáng tán xạ, giữ đất ẩm vừa, lau lá sạch bụi và theo dõi 5–7 ngày sau khi xử lý.
            """.trimIndent()
        } else {
            """
            Đốm lá / cháy mép lá.
            Triệu chứng: Lá có đốm vàng, đốm nâu, vùng khô ở mép hoặc đầu lá; một số vết có quầng vàng và lan dần nếu môi trường ẩm.
            Nguyên nhân có thể: Cháy nắng hoặc thừa sáng, tưới quá nhiều gây úng rễ, nấm bệnh đốm lá, thiếu dinh dưỡng hoặc lá bị tổn thương cơ học.
            Cách xử lý: Cắt bỏ phần lá bệnh nặng, chuyển cây sang nơi có ánh sáng gián tiếp, tưới gốc thay vì tưới ướt lá, kiểm tra đất thoát nước và dùng dầu neem hoặc chế phẩm sinh học nếu vết lan nhanh.
            Hướng dẫn chăm sóc cơ bản: Tưới vừa phải 1–2 lần/tuần tùy độ ẩm đất, bón phân loãng định kỳ, vệ sinh lá bằng khăn ẩm và cách ly cây bệnh khỏi cây khỏe.
            """.trimIndent()
        }
    }

    private fun detectVisualDiseaseName(imageFile: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = when {
            maxSide > 2400 -> 8
            maxSide > 1200 -> 4
            maxSide > 700 -> 2
            else -> 1
        }
        val bitmap = BitmapFactory.decodeFile(
            imageFile.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            },
        ) ?: return null

        var vegetation = 0
        var yellow = 0
        var brown = 0
        val hsv = FloatArray(3)
        val step = 3
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                Color.colorToHSV(bitmap.getPixel(x, y), hsv)
                val hue = hsv[0]
                val saturation = hsv[1]
                val value = hsv[2]
                val isLeafTone = hue in 35f..170f && saturation > 0.16f && value > 0.18f
                if (!isLeafTone) continue
                vegetation++
                if (hue in 38f..72f && saturation > 0.18f && value > 0.38f) {
                    yellow++
                }
                if (hue in 12f..42f && saturation > 0.22f && value in 0.18f..0.66f) {
                    brown++
                }
            }
        }
        bitmap.recycle()

        if (vegetation < 260) return null
        val yellowRatio = yellow.toFloat() / vegetation
        val brownRatio = brown.toFloat() / vegetation
        return when {
            brownRatio >= 0.025f && (yellowRatio >= 0.04f || brownRatio >= 0.045f) -> "Đốm lá / cháy mép lá"
            yellowRatio >= 0.18f && yellowRatio >= brownRatio * 1.15f -> "Vàng lá"
            else -> null
        }
    }

    private fun fold(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun describeJsonType(element: JsonElement?): String {
        return when {
            element == null || element.isJsonNull -> "null"
            element.isJsonPrimitive -> "primitive"
            element.isJsonObject -> "object"
            element.isJsonArray -> "array"
            else -> "unknown"
        }
    }
}
