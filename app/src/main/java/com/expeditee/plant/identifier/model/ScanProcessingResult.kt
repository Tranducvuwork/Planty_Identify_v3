package com.expeditee.plant.identifier.model

import java.io.Serializable

sealed interface ScanProcessingResult {
    data class Success(
        val label: String,
        val identifiedPlant: IdentifiedPlant? = null,
        val matchedPlant: PopularPlantModel? = null,
        val confidence: Float? = null,
        val metadata: IdentifyMetadata = IdentifyMetadata(),
        val diagnosedDisease: DiagnosedDisease? = null,
    ) : ScanProcessingResult, Serializable

    data class Error(
        val type: ProcessingErrorType,
        val throwable: Throwable? = null,
        /** Lý do hiển thị cho user (vd. description từ server khi Unknown). */
        val userMessage: String? = null,
    ) : ScanProcessingResult, Serializable
}

data class IdentifyMetadata(
    val mode: ModeScan? = null,
    val imageSource: String? = null,
    val resolutionAttemptedAtIdentify: Boolean = false,
    val catalogReadyAtIdentify: Boolean = false,
) : Serializable

data class Solution(
    val title: String? = null,
    val summary: String? = null,
    val steps: List<String>? = null,
    val precautions: List<String>? = null,
) : Serializable

data class DiagnosedDisease(
    val isHealthy: Boolean,
    val disease: CommonDisease? = null,
    val probability: Double = 0.0,
    val isPlant: Boolean = true,
    val solution: Solution? = null,
) : Serializable

enum class ProcessingErrorType {
    INVALID_IMAGE,
    QUOTA_EXCEEDED,
    NETWORK,
    SERVER,
    NOT_IDENTIFIED,
    TIMEOUT,
    UNKNOWN,
}
