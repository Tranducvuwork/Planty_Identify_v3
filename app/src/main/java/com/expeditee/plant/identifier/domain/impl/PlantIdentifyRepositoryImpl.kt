package com.expeditee.plant.identifier.domain.impl

import android.net.Uri
import com.expeditee.plant.identifier.data.repository.OfflinePlantIdentifier
import com.expeditee.plant.identifier.data.repository.OnlinePlantIdentifier
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.domain.repository.PlantIdentifyRepository
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.helper.SafeImageDecoder
import com.expeditee.plant.identifier.model.IdentifyMetadata
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.model.ProcessingErrorType
import com.expeditee.plant.identifier.model.ScanProcessingResult
import com.expeditee.plant.identifier.services.OfflineModeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlantIdentifyRepositoryImpl(
    private val imageDecoder: SafeImageDecoder,
    private val onlinePlantIdentifier: OnlinePlantIdentifier,
    private val offlinePlantIdentifier: OfflinePlantIdentifier,
    private val plantRepository: PlantRepository,
    private val appConfigSettings: AppConfigSettings,
    @Suppress("unused") private val _offlineModeService: OfflineModeService,
) : PlantIdentifyRepository {
    override suspend fun identify(
        imageUri: Uri,
        imageSource: String,
        requestedMode: ModeScan,
    ): ScanProcessingResult = withContext(Dispatchers.IO) {
        val startTimeMs = System.currentTimeMillis()
        val language = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }

        val mode = requestedMode

        val identifyResult = if (mode == ModeScan.OFFLINE) {
            val bitmap = imageDecoder.decode(imageUri)
                ?: return@withContext toErrorResult(
                    IllegalArgumentException("Failed to decode image"),
                    startTimeMs,
                )
            offlinePlantIdentifier.identify(bitmap, imageUri.toString())
        } else {
            onlinePlantIdentifier.identify(imageUri, imageSource)
        }

        val identified = identifyResult.getOrElse { throwable ->
            return@withContext toErrorResult(throwable, startTimeMs)
        }

        val catalogReadyAtIdentify = plantRepository.isDataAvailable()
        val resolutionAttemptedAtIdentify = catalogReadyAtIdentify

        val matchedPlant = if (catalogReadyAtIdentify) {
            plantRepository.resolvePlantForIdentify(
                scientificName = identified.scientificName,
                commonNames = identified.commonNames,
                language = language,
            )
        } else {
            null
        }

        val finalIdentified = identified.copy(
            matchedFromCatalog = matchedPlant != null,
        )
        return@withContext ScanProcessingResult.Success(
            label = finalIdentified.scientificName,
            identifiedPlant = finalIdentified,
            matchedPlant = matchedPlant,
            confidence = finalIdentified.confidence.toFloat(),
            metadata = IdentifyMetadata(
                mode = mode,
                imageSource = imageSource,
                resolutionAttemptedAtIdentify = resolutionAttemptedAtIdentify,
                catalogReadyAtIdentify = catalogReadyAtIdentify,
            ),
        )
    }

    private fun toErrorResult(
        error: Throwable,
        startTimeMs: Long,
    ): ScanProcessingResult.Error {
        val message = error.message.orEmpty().lowercase()
        val errorType = when {
            error is com.expeditee.plant.identifier.network.PlantNotIdentifiedException ->
                ProcessingErrorType.NOT_IDENTIFIED
            message.contains("offline model is not ready") ||
                message.contains("tf lite interpreter is not initialized") ||
                message.contains("model file not found") -> ProcessingErrorType.SERVER
            error is java.net.UnknownHostException ||
                message.contains("unable to resolve host") ||
                message.contains("no address associated with hostname") -> ProcessingErrorType.NETWORK
            error is java.net.SocketTimeoutException ||
                error is java.io.InterruptedIOException ||
                message.contains("timeout") ||
                message.contains("timed out") -> ProcessingErrorType.TIMEOUT
            message.contains("internet") || message.contains("network") -> ProcessingErrorType.NETWORK
            message.contains("decode") || message.contains("unable to load image") ->
                ProcessingErrorType.INVALID_IMAGE
            message.contains("plant not identified") ||
                message.contains("server returned unknown") ||
                message.contains("no predictions") ||
                message.contains("could not determine") ||
                message.contains("missing scientificname") ||
                message.contains("invalid scientificname") ||
                message.contains("below threshold") ||
                message.contains("ambiguous offline") -> ProcessingErrorType.NOT_IDENTIFIED
            else -> ProcessingErrorType.SERVER
        }

        val userMessage = when {
            message.contains("offline model is not ready") ||
                message.contains("tf lite interpreter is not initialized") ||
                message.contains("model file not found") ->
                "Model offline chưa sẵn sàng. Hãy mở Wi‑Fi để tải model, rồi thử lại."
            else -> (error as? com.expeditee.plant.identifier.network.PlantNotIdentifiedException)
                ?.userReason
                ?.takeIf { it.isNotBlank() }
                ?: error.message
                    ?.substringAfter("reason=", missingDelimiterValue = "")
                    ?.takeIf { it.isNotBlank() && errorType == ProcessingErrorType.NOT_IDENTIFIED }
        }

        return ScanProcessingResult.Error(
            type = errorType,
            throwable = error,
            userMessage = userMessage,
        )
    }
}
