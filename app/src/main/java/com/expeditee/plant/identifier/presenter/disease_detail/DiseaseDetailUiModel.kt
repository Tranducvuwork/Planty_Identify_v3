package com.expeditee.plant.identifier.presenter.disease_detail

import com.expeditee.plant.identifier.model.Solution

data class DiseaseDetailUiModel(
    val imagePath: String?,
    val probability: Int,
    val isFromDisease: Boolean,
    val basicInfo: DiseaseBasicInfoUiModel,
    val overview: DiseaseOverviewUiModel,
    val treatment: DiseaseTreatmentUiModel,
    val similarImages: List<SimilarImageUiModel>,
    val otherDiseases: List<OtherDiseaseUiModel>,
    val symptoms: List<String> = emptyList(),
    val causes: List<String> = emptyList(),
    val prevention: List<String> = emptyList(),
    val videos: List<DiseaseVideoUiModel> = emptyList(),
    val visuals: DiseaseVisualUiModel = DiseaseVisualUiModel(),
    val solution: Solution? = null,
)

data class DiseaseBasicInfoUiModel(
    val displayName: String,
    val scientificName: String,
    val otherNames: String,
)

data class DiseaseOverviewUiModel(
    val description: String,
)

data class DiseaseTreatmentUiModel(
    val biological: List<String>,
    val chemical: List<String>,
    val fallback: List<String>,
)

data class SimilarImageUiModel(
    val url: String?,
    val urlSmall: String?,
    val licenseName: String?,
    val licenseUrl: String?,
)

data class OtherDiseaseUiModel(
    val id: Int,
    val displayName: String,
    val imageUrl: String?,
)

data class DiseaseVideoUiModel(
    val title: String,
    val url: String,
)

data class DiseaseVisualUiModel(
    val pestImageUrl: String? = null,
    val symptomImageUrl: String? = null,
    val healthyImageUrl: String? = null,
    val diseasedImageUrl: String? = null,
)
