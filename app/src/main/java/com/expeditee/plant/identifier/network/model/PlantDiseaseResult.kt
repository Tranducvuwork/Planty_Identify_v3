package com.expeditee.plant.identifier.network.model

import androidx.annotation.Keep

@Keep
data class PlantDiseaseResult(
    val isHealthy: Boolean,
    val isPlantImage: Boolean?,
    val diseaseId: Int?,
    val diseaseName: String?,
    val matchedDiseaseName: String?,
    val confidence: Double,
    val analysisNotes: String?,
    val diseaseDetails: String?,
    val isKnownDisease: Boolean?,
    val visualImages: DiseaseVisualImages = DiseaseVisualImages(),
)

data class DiseaseVisualImages(
    val pestImageUrl: String? = null,
    val symptomImageUrl: String? = null,
    val healthyImageUrl: String? = null,
    val diseasedImageUrl: String? = null,
) {
    fun orderedUrls(): List<String> = listOfNotNull(
        pestImageUrl,
        symptomImageUrl,
        healthyImageUrl,
        diseasedImageUrl,
    ).distinct()
}
