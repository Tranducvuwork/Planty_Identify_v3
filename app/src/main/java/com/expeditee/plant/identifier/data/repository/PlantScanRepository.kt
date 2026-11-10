package com.expeditee.plant.identifier.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.data.upload.PlantImageUploadHelper
import com.expeditee.plant.identifier.network.PlantBackendApiService
import com.expeditee.plant.identifier.network.PlantNotIdentifiedException
import com.expeditee.plant.identifier.network.model.CommonDiseaseData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import com.expeditee.plant.identifier.thesis.ThesisLocalStore
import com.expeditee.plant.identifier.thesis.ThesisPlantCatalog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PlantScanRepository(
    private val context: Context,
    private val apiService: PlantBackendApiService,
    private val vietnameseTranslationRepository: VietnameseTranslationRepository,
    private val nvidiaVietnameseNameRepository: NvidiaVietnameseNameRepository,
    private val thesisLocalStore: ThesisLocalStore,
) {
    companion object {
        private const val TAG = "PlantScanRepository"
    }

    suspend fun scanPlant(
        imageFile: File,
        language: String? = null,
        enhanceCameraImage: Boolean = false,
        preferOriginalFirst: Boolean = false,
    ): Result<PlantScanResult> {
        val lang = language?.takeIf { it.isNotBlank() }

        Log.i(
            TAG,
            "scanPlant baseUrl=${Constants.PLANT_API_BASE_URL} " +
                "useLocal=${BuildConfig.USE_LOCAL_PLANT_API} file=${imageFile.length()}B lang=${lang ?: "<none>"} " +
                "enhanceCamera=$enhanceCameraImage preferOriginalFirst=$preferOriginalFirst",
        )

        if (!PlantImageUploadHelper.validate(imageFile)) {
            return Result.failure(IllegalArgumentException("Invalid image file"))
        }

        return try {
            if (preferOriginalFirst) {
                return scanOriginalFirst(imageFile, lang, canRetryNormalized = enhanceCameraImage)
            }

            val primary = requestScan(
                imageFile = imageFile,
                lang = lang,
                uploadStrategy = if (enhanceCameraImage) UploadStrategy.NORMALIZED else UploadStrategy.ORIGINAL,
            )
            Log.i(TAG, "scanPlant primary result=${summary(primary)}")
            val finalResult = if (shouldRetryWithOriginal(primary, enhanceCameraImage)) {
                Log.w(TAG, "scanPlant retry original because primary weak/unknown result=${summary(primary)}")
                runCatching { requestScan(imageFile, lang, UploadStrategy.ORIGINAL) }
                    .map { original -> chooseStableResult(primary, original) }
                    .getOrElse {
                        Log.w(TAG, "scanPlant original retry failed: ${it.message}")
                        primary
                    }
            } else {
                primary
            }
            Log.i(TAG, "scanPlant final result=${summary(finalResult)}")
            Result.success(finalResult)
        } catch (e: PlantNotIdentifiedException) {
            Log.w(TAG, "scanPlant not identified: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            if (enhanceCameraImage) {
                runCatching { requestScan(imageFile, lang, UploadStrategy.ORIGINAL) }
                    .onSuccess {
                        Log.i(TAG, "scanPlant recovered by original result=${summary(it)}")
                        return Result.success(it)
                    }
                    .onFailure { retryError ->
                        if (retryError is PlantNotIdentifiedException) {
                            Log.w(TAG, "scanPlant original retry not identified: ${retryError.message}")
                            return Result.failure(retryError)
                        }
                        Log.e(
                            TAG,
                            "scanPlant error and original retry failed baseUrl=${Constants.PLANT_API_BASE_URL}: ${retryError.message}",
                            retryError,
                        )
                    }
            }
            Log.e(TAG, "scanPlant error baseUrl=${Constants.PLANT_API_BASE_URL}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun scanOriginalFirst(
        imageFile: File,
        lang: String?,
        canRetryNormalized: Boolean,
    ): Result<PlantScanResult> {
        return try {
            val original = requestScan(imageFile, lang, UploadStrategy.ORIGINAL)
            Log.i(TAG, "scanPlant original-first primary result=${summary(original)}")
            val finalResult = if (canRetryNormalized && shouldRetryWithNormalized(original)) {
                Log.w(TAG, "scanPlant retry normalized because original weak result=${summary(original)}")
                runCatching { requestScan(imageFile, lang, UploadStrategy.NORMALIZED) }
                    .map { normalized -> chooseStableResult(normalized, original) }
                    .getOrElse {
                        if (it is PlantNotIdentifiedException) {
                            Log.w(TAG, "scanPlant normalized retry not identified, keep original failure path")
                        } else {
                            Log.w(TAG, "scanPlant normalized retry failed: ${it.message}")
                        }
                        original
                    }
            } else {
                original
            }
            Log.i(TAG, "scanPlant original-first final result=${summary(finalResult)}")
            Result.success(finalResult)
        } catch (e: PlantNotIdentifiedException) {
            // Unknown là case nghiệp vụ — không Log.e + stack (tránh hiểu nhầm là crash).
            Log.w(TAG, "scanPlant original-first not identified: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            if (canRetryNormalized) {
                runCatching { requestScan(imageFile, lang, UploadStrategy.NORMALIZED) }
                    .onSuccess {
                        Log.i(TAG, "scanPlant original-first recovered by normalized result=${summary(it)}")
                        return Result.success(it)
                    }
                    .onFailure { retryError ->
                        if (retryError is PlantNotIdentifiedException) {
                            Log.w(TAG, "scanPlant original-first normalized not identified: ${retryError.message}")
                            return Result.failure(retryError)
                        }
                        Log.e(TAG, "scanPlant original-first normalized retry failed: ${retryError.message}", retryError)
                    }
            }
            Log.e(TAG, "scanPlant original-first error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun requestScan(
        imageFile: File,
        lang: String?,
        uploadStrategy: UploadStrategy,
    ): PlantScanResult {
        val prepared = when (uploadStrategy) {
            UploadStrategy.NORMALIZED -> PlantImageUploadHelper.prepareUpload(
                imageFile = imageFile,
                enhanceCameraImage = true,
            )
            UploadStrategy.ORIGINAL -> PlantImageUploadHelper.PreparedUpload(
                file = imageFile,
                compressed = false,
            )
        }
        Log.i(
            TAG,
            "scanPlant prepared upload file=${prepared.file.name} compressed=${prepared.compressed} " +
                "size=${prepared.file.length()}B strategy=$uploadStrategy",
        )

        return try {
            val multipart = PlantImageUploadHelper.createImagePart(prepared.file)
            val languagePart = lang?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.scanPlant(multipart, languagePart)
            Log.i(TAG, "scanPlant server status=${response.status} strategy=$uploadStrategy hasData=${response.data != null}")

            if (!response.status.equals("success", ignoreCase = true)) {
                throw IllegalStateException("Scan failed: status=${response.status}")
            }

            val data = response.data
                ?: throw IllegalStateException("Scan response data is null")

            val scientificName = data.scientificName?.trim().orEmpty()
            Log.i(
                TAG,
                "scanPlant server data strategy=$uploadStrategy scientific='$scientificName' " +
                    "common=${data.commonNames.orEmpty().joinToString("|")} confidence=${data.confidence ?: 0} " +
                    "desc='${data.description.orEmpty().take(180)}'",
            )
            // Tag dùng chung với identify để dễ lọc log trên device.
            Log.i(
                "ScanUploadDebug",
                "server raw strategy=$uploadStrategy scientific='$scientificName' " +
                    "common=${data.commonNames.orEmpty().joinToString("|")} confidence=${data.confidence ?: 0} " +
                    "lang=$lang file=${prepared.file.name} size=${prepared.file.length()}B compressed=${prepared.compressed}",
            )
            if (scientificName.isBlank() ||
                scientificName.equals("Unknown", ignoreCase = true) ||
                scientificName.equals("null", ignoreCase = true)
            ) {
                val reason = vietnameseTranslationRepository.translateDisplayText(data.description)
                    ?: "Ảnh chưa đủ rõ hoặc thiếu đặc điểm nhận dạng như dáng lá, cách mọc, hoa/quả và thân cây."
                throw PlantNotIdentifiedException(
                    message = "Server returned Unknown strategy=$uploadStrategy common=${data.commonNames.orEmpty()} confidence=${data.confidence ?: 0} reason=$reason",
                    userReason = reason,
                )
            }

            val localizedCommonNames = vietnameseTranslationRepository
                .translateDisplayList(data.commonNames)
                .ifEmpty { data.commonNames.orEmpty() }
            val localizedDescription = vietnameseTranslationRepository
                .translateDisplayText(data.description)
            val localizedUses = vietnameseTranslationRepository
                .translateDisplayList(data.uses)
            val localizedToxicity = localizeToxicity(data.toxicity)
            val localizedCare = localizeCare(data.care)
            val visualDiseases = detectVisualSymptoms(imageFile)
            val localizedDiseases = mergeDiseaseHints(
                visualDiseases,
                localizeCommonDiseases(data.commonDiseases),
            )
            val localizedFolkRemedies = localizeFolkRemedies(data.folkRemedies)
            val localizedDisclaimer = vietnameseTranslationRepository
                .translateDisplayText(data.medicalDisclaimer)

            val serverResult = PlantScanResult(
                scientificName = scientificName,
                commonNames = localizedCommonNames,
                confidence = (data.confidence ?: 0).toDouble(),
                description = localizedDescription,
                plantId = data.plantId,
                qrCode = data.qrCode,
                isMedicinal = data.isMedicinal == true,
                medicinalLabel = vietnameseTranslationRepository.translateDisplayText(data.medicinalLabel),
                uses = localizedUses,
                toxicity = localizedToxicity,
                care = localizedCare,
                commonDiseases = localizedDiseases,
                folkRemedies = localizedFolkRemedies,
                medicalDisclaimer = localizedDisclaimer,
                imageUrl = data.imageUrl,
                detailAvailable = data.detailAvailable == true ||
                    data.isMedicinal == true ||
                    data.toxicity?.playAlertSound == true ||
                    data.uses.orEmpty().isNotEmpty() ||
                    data.care != null,
            )
            val enriched = ThesisPlantCatalog.enrich(context, serverResult)
            val named = nvidiaVietnameseNameRepository.fillIfMissing(enriched)
            val withFacts = nvidiaVietnameseNameRepository.enrichFacts(named)
            val overridden = thesisLocalStore.applyOverride(withFacts)
            Log.i(TAG, "scanPlant enriched strategy=$uploadStrategy before=${summary(serverResult)} after=${summary(overridden)}")
            overridden
        } finally {
            if (uploadStrategy == UploadStrategy.NORMALIZED) {
                PlantImageUploadHelper.cleanup(imageFile, prepared.file)
            }
        }
    }

    private enum class UploadStrategy {
        NORMALIZED,
        ORIGINAL,
    }

    private fun shouldRetryWithOriginal(result: PlantScanResult, enhanced: Boolean): Boolean {
        if (!enhanced) return false
        // Chỉ retry ảnh gốc khi bản enhance còn yếu.
        if (result.confidence < 80.0) return true
        val displayName = result.commonNames.firstOrNull().orEmpty()
        return displayName.contains("chưa rõ", ignoreCase = true)
    }

    private fun shouldRetryWithNormalized(result: PlantScanResult): Boolean {
        // Chỉ retry ảnh đã enhance khi kết quả gốc còn yếu — tránh đổi loài khi server đã đủ tin.
        if (result.confidence < 80.0) return true
        val displayName = result.commonNames.firstOrNull().orEmpty()
        return displayName.contains("chưa rõ", ignoreCase = true)
    }

    private fun chooseStableResult(enhanced: PlantScanResult, original: PlantScanResult): PlantScanResult {
        val sameScientific = sameGenusOrScientific(enhanced.scientificName, original.scientificName)
        if (sameScientific) {
            val chosen = if (original.confidence > enhanced.confidence) original else enhanced
            Log.i(TAG, "scanPlant choose same genus/scientific chosen=${summary(chosen)}")
            return chosen
        }

        // Khác loài: ưu tiên ảnh gốc (không crop/enhance) nếu đã đủ tin cậy.
        if (original.confidence >= 80.0) {
            Log.i(
                TAG,
                "scanPlant prefer original (solid) '${original.scientificName}'(${original.confidence}) " +
                    "over enhanced '${enhanced.scientificName}'(${enhanced.confidence})",
            )
            return original
        }

        if (enhanced.confidence >= original.confidence + 12.0) {
            Log.i(
                TAG,
                "scanPlant prefer enhanced (much stronger) '${enhanced.scientificName}'(${enhanced.confidence}) " +
                    "over weak original '${original.scientificName}'(${original.confidence})",
            )
            return enhanced
        }

        Log.i(
            TAG,
            "scanPlant keep original by default '${original.scientificName}'(${original.confidence}) " +
                "enhanced='${enhanced.scientificName}'(${enhanced.confidence})",
        )
        return original
    }

    private fun sameGenusOrScientific(a: String, b: String): Boolean {
        if (a.equals(b, ignoreCase = true)) return true
        val genusA = a.trim().substringBefore(' ').substringBefore('.').lowercase()
        val genusB = b.trim().substringBefore(' ').substringBefore('.').lowercase()
        return genusA.length >= 3 && genusA == genusB
    }

    private fun summary(result: PlantScanResult): String {
        return "scientific='${result.scientificName}' common='${result.commonNames.firstOrNull().orEmpty()}' " +
            "confidence=${result.confidence} image='${result.imageUrl.orEmpty().take(80)}'"
    }

    private suspend fun localizeToxicity(toxicity: com.expeditee.plant.identifier.network.model.ToxicityData?) =
        toxicity?.copy(
            warningLevel = vietnameseTranslationRepository.translateDisplayText(toxicity.warningLevel),
            warningMessage = vietnameseTranslationRepository.translateDisplayText(toxicity.warningMessage),
            firstAid = vietnameseTranslationRepository.translateDisplayText(toxicity.firstAid),
        )

    private suspend fun localizeCare(care: com.expeditee.plant.identifier.network.model.CareData?) =
        care?.copy(
            planting = vietnameseTranslationRepository.translateDisplayText(care.planting),
            care = vietnameseTranslationRepository.translateDisplayText(care.care),
            bestSeason = vietnameseTranslationRepository.translateDisplayText(care.bestSeason),
            citationNote = vietnameseTranslationRepository.translateDisplayText(care.citationNote),
        )

    private suspend fun localizeCommonDiseases(
        diseases: List<com.expeditee.plant.identifier.network.model.CommonDiseaseData>?,
    ) = diseases.orEmpty().map { disease ->
        disease.copy(
            name = vietnameseTranslationRepository.translateDisplayText(disease.name),
            description = vietnameseTranslationRepository.translateDisplayText(disease.description),
            ecoTreatment = vietnameseTranslationRepository.translateDisplayText(disease.ecoTreatment),
        )
    }

    private suspend fun localizeFolkRemedies(
        remedies: List<com.expeditee.plant.identifier.network.model.FolkRemedyData>?,
    ) = remedies.orEmpty().map { remedy ->
        remedy.copy(
            title = vietnameseTranslationRepository.translateDisplayText(remedy.title),
            ailment = vietnameseTranslationRepository.translateDisplayText(remedy.ailment),
            ingredients = vietnameseTranslationRepository.translateDisplayList(remedy.ingredients),
            method = vietnameseTranslationRepository.translateDisplayText(remedy.method),
            region = vietnameseTranslationRepository.translateDisplayText(remedy.region),
            plants = vietnameseTranslationRepository.translateDisplayList(remedy.plants),
            sourceTitle = vietnameseTranslationRepository.translateDisplayText(remedy.sourceTitle),
            caution = vietnameseTranslationRepository.translateDisplayText(remedy.caution),
        )
    }

    private fun mergeDiseaseHints(
        visualDiseases: List<CommonDiseaseData>,
        serverDiseases: List<CommonDiseaseData>,
    ): List<CommonDiseaseData> {
        val seen = linkedSetOf<String>()
        return (visualDiseases + serverDiseases).filter { disease ->
            val key = disease.name.orEmpty().lowercase().trim()
            key.isBlank() || seen.add(key)
        }
    }

    private fun detectVisualSymptoms(imageFile: File): List<CommonDiseaseData> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return emptyList()

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
        ) ?: return emptyList()

        var vegetation = 0
        var yellow = 0
        var brown = 0
        val hsv = FloatArray(3)
        val step = 3
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                Color.colorToHSV(pixel, hsv)
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

        if (vegetation < 260) return emptyList()
        val yellowRatio = yellow.toFloat() / vegetation
        val brownRatio = brown.toFloat() / vegetation
        val hints = mutableListOf<CommonDiseaseData>()
        if (yellowRatio >= 0.20f) {
            hints += CommonDiseaseData(
                name = "Vàng lá / úa lá",
                description = "Ảnh có nhiều vùng lá ngả vàng. Có thể do thiếu dinh dưỡng, úng nước, thiếu sáng hoặc rễ yếu.",
                ecoTreatment = "Kiểm tra độ ẩm đất và rễ, cắt lá vàng nặng, tăng thoáng khí, bón hữu cơ/NPK loãng sau khi cây ổn định.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/73/Leaf_chlorosis.jpg/320px-Leaf_chlorosis.jpg",
            )
        }
        if (brownRatio >= 0.10f) {
            hints += CommonDiseaseData(
                name = "Đốm nâu / cháy mép lá",
                description = "Ảnh có vùng nâu khô trên lá. Thường liên quan nắng gắt, thiếu nước, nấm đốm lá hoặc đất quá ẩm kéo dài.",
                ecoTreatment = "Cắt phần lá hư, tưới gốc thay vì tưới lá, đặt cây nơi sáng thoáng và theo dõi lan rộng trong 3-5 ngày.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Leaf_spot_on_tomato.jpg/320px-Leaf_spot_on_tomato.jpg",
            )
        }
        return hints
    }
}
