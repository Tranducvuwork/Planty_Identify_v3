package com.expeditee.plant.identifier.network.model

import androidx.annotation.Keep
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

@Keep
data class PlantDiseaseData(
    @SerializedName("analysis_notes") val analysisNotes: String?,
    @SerializedName("isPlantImage") val isPlantImage: Boolean?,
    @SerializedName("isDiseasedLeafImage") val isDiseasedLeafImage: Boolean?,
    @SerializedName("isHealthy") val isHealthy: Boolean?,
    @SerializedName("diseaseId") val diseaseId: Int?,
    @SerializedName("diseaseName") val diseaseName: String?,
    @SerializedName("matchedDiseaseName") val matchedDiseaseName: String?,
    @SerializedName("isKnownDisease") val isKnownDisease: Boolean?,
    @SerializedName("diseaseDetails") val diseaseDetails: JsonElement?,
    @SerializedName("confidence") val confidence: Int?,
)

@Keep
data class PlantDiseaseApiResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: PlantDiseaseData?,
)
