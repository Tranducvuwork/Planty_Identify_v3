package com.expeditee.plant.identifier.model.reponse

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PlantDetailResponse(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("common_names")
    val commonNames: List<String>?,
    @SerializedName("scientific_name")
    val scientificName: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("main_image")
    val mainImage: PlantImage?,
    @SerializedName("similar_images")
    val similarImages: List<PlantImage>? = null,
    @SerializedName("hardiness")
    val hardiness: HardinessInfo?,
    @SerializedName("details")
    val details: PlantDetailInfo?,
)

data class PlantDetailInfo(
    @SerializedName("description")
    val description: String?,
    @SerializedName("family")
    val family: String?,
    @SerializedName("genus")
    val genus: String?,
    @SerializedName("species")
    val species: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("origin")
    val origin: List<String>?,
    @SerializedName("height")
    val height: String?,
    @SerializedName("spread")
    val spread: String?,
    @SerializedName("bloom_time")
    val bloomTime: String?,
    @SerializedName("hardiness")
    val hardiness: HardinessInfo?,
    @SerializedName("dimensions")
    val dimensions: DimensionInfo?,
    @SerializedName("growth_cultivation")
    val growthCultivation: GrowthCultivationInfo?,
)

data class GrowthCultivationInfo(
    @SerializedName("is_tropical")
    val isTropical: Boolean = false,
    @SerializedName("is_indoor")
    val isIndoor: Boolean = false,
    @SerializedName("cycle")
    val cycle: String?,
    @SerializedName("growth_rate")
    val growthRate: String?,
    @SerializedName("soil")
    val soil: List<String>? = null,
    @SerializedName("attracts")
    val attracts: List<String>? = null,
    @SerializedName("seeds")
    val seeds: Int? = null,
    @SerializedName("watering")
    val watering: WateringInfo?,
    @SerializedName("sunlight")
    val sunlight: SunlightInfo?,
    @SerializedName("light_requirements")
    val lightRequirements: String?,
    @SerializedName("temperature_range")
    val temperatureRange: String?,
    @SerializedName("pruning")
    val pruning: PruningInfo?,
    @SerializedName("humidity")
    val humidity: HumidityInfo?,
    @SerializedName("propagation")
    val propagation: List<String>?,
    @SerializedName("care_level")
    val careLevel: String?,
    @SerializedName("maintenance")
    val maintenance: String?,
    @SerializedName("tolerant")
    val tolerant: ToleranceInfo?,
)

data class SunlightInfo(
    @SerializedName("type")
    val type: List<String>?,
    @SerializedName("guide")
    val guide: String?,
)

data class DimensionInfo(
    @SerializedName("min")
    val min: Double?,
    @SerializedName("max")
    val max: Double?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("description")
    val description: String?,
)

data class ToleranceInfo(
    @SerializedName("drought")
    val drought: Boolean = false,
    @SerializedName("salt")
    val salt: Boolean = false,
)

data class HardinessInfo(
    @SerializedName("min")
    val min: Int?,
    @SerializedName("max")
    val max: Int?,
    @SerializedName("full_location_url")
    val fullLocationUrl: String?,
)

data class WateringInfo(
    @SerializedName("frequency")
    val frequency: String?,
    @SerializedName("guide")
    val guide: String?,
)

data class PruningInfo(
    @SerializedName("when")
    val whenText: String?,
    @SerializedName("how")
    val how: String?,
    @SerializedName("guide")
    val guide: String?,
    @SerializedName("months")
    val months: List<String>?,
)

data class HumidityInfo(
    @SerializedName("min")
    val min: Int?,
    @SerializedName("max")
    val max: Int?,
)