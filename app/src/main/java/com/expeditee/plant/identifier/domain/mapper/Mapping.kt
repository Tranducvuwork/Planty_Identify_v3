package com.expeditee.plant.identifier.domain.mapper

import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.reponse.PlantResponse


private const val GITHUB_RAW_BASE =
    "https://raw.githubusercontent.com/LMT2025/plant_scan_data/main/PlantImage"


fun PlantResponse.toPopularPlantModel(): PopularPlantModel {
    val fileName = mainImage?.url?.substringAfterLast("/")
    val name = commonNames?.firstOrNull() ?: scientificName ?: "Unknown"
    val type = if (name.contains("Flowering", ignoreCase = true) || name.contains("Flower", ignoreCase = true)) {
        PopularPlantModel.TYPE_FLOWER
    } else {
        PopularPlantModel.TYPE_PLANT
    }
    return PopularPlantModel(
        id = id.toString(),
        name = name,
        imageUrl = fileName?.let { "$GITHUB_RAW_BASE/$it" },
        type = type
    )
}