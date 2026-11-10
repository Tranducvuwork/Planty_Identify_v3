package com.expeditee.plant.identifier.model

import androidx.annotation.Keep


@Keep
data class PlantUIModel(
    val basic: BasicSection,
    val classification: ClassificationSection,
    val physical: PhysicalSection,
    val care: CareSection,
    val environment: EnvironmentSection,
    val images: ImagesSection,
)

@Keep
data class BasicSection(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val description: String?,
    val commonNames: List<String>? = null,
)

@Keep
data class ClassificationSection(
    val family: String?,
    val genus: String?,
    val species: String?,
    val plantType: String?,
    val origin: List<String>?,
    val isTropical: Boolean,
    val isIndoor: Boolean,
) {

}
@Keep
data class PhysicalSection(
    val height: String?,
    val heightMin: Double?,
    val heightMax: Double?,
    val heightUnit: String?,
    val dimensionsDescription: String? = null,
    val spread: String?,
    val bloomTime: String?,
) {
    fun getHeightDisplay(): String? = when {
        heightMin != null && heightMax != null && heightUnit != null -> {
            if (heightMin == heightMax) {
                "$heightMax $heightUnit"
            } else {
                "$heightMin-$heightMax $heightUnit"
            }
        }
        height != null -> height
        else -> null
    }
}

@Keep
data class CareSection(
    val careLevel: String?,
    val growthRate: String?,
    val cycle: String?,
    val maintenance: String? = null,
    val propagation: List<String>?,
    val watering: String?,
    val wateringGuide: String?,
    val pruningMonths: List<String>?,
    val pruningGuide: String?,
    val hardiness: String?,
    val hardinessMin: Int? = null,
    val hardinessMax: Int? = null,
) {
    fun hasPruningInfo(): Boolean = !pruningMonths.isNullOrEmpty() || !pruningGuide.isNullOrBlank()
    fun getPruningDisplay(): String? = when {
        !pruningMonths.isNullOrEmpty() -> "Prune: ${pruningMonths.take(3).joinToString(", ")}"
        else -> null
    }
}

@Keep
data class EnvironmentSection(
    val lightNeeds: String?,
    val sunlightTypes: List<String>?,
    val sunlightGuide: String?,
    val temperature: String?,
    val humidity: String?,
    val humidityMin: Int?,
    val humidityMax: Int?,
    val soil: List<String>? = null,
    val attracts: List<String>? = null,
    val droughtTolerant: Boolean,
    val saltTolerant: Boolean,
) {
    fun getSunlightDisplay(): String? = when {
        !sunlightTypes.isNullOrEmpty() -> sunlightTypes.joinToString(", ")
        lightNeeds != null -> lightNeeds
        else -> null
    }

    fun getTolerances(): List<String> = listOfNotNull(
        "Drought Tolerant".takeIf { droughtTolerant },
        "Salt Tolerant".takeIf { saltTolerant },
    )
}

@Keep
data class ImagesSection(
    val mainImage: String?,
    val gallery: List<String>,
) {
    fun hasGallery(): Boolean = gallery.size > 1
}
