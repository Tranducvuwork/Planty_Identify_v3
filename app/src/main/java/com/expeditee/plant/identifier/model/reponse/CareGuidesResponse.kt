package com.expeditee.plant.identifier.model.reponse

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class CareGuidesResponse(
    @SerializedName("care_guides")
    val careGuides: List<SuggestedPlant>?
) : Serializable

@Keep
data class SuggestedPlant(
    @SerializedName("scientific_name")
    val scientificName: String?,
    @SerializedName("common_names")
    val commonNames: List<String>?,
    @SerializedName("main_image")
    val mainImage: MainImage?,
    @SerializedName("growth_cultivation")
    val growthCultivation: GrowthCultivation?,
    @SerializedName("care_guide")
    val careGuide: CareGuide?
) : Serializable


@Keep
data class MainImage(
    @SerializedName("url")
    val url: String?,
    @SerializedName("url_small")
    val urlSmall: String?,
    @SerializedName("license")
    val license: License?
) : Serializable

@Keep
data class License(
    @SerializedName("url")
    val url: String?,
    @SerializedName("name")
    val name: String?
) : Serializable


@Keep
data class GrowthCultivation(
    @SerializedName("is_tropical")
    val isTropical: Boolean?,
    @SerializedName("watering")
    val watering: Watering?,
    @SerializedName("light_requirements")
    val lightRequirements: String?,
    @SerializedName("soil_type")
    val soilType: String?,
    @SerializedName("ph_range")
    val phRange: String?,
    @SerializedName("is_indoor")
    val isIndoor: Boolean?,
    @SerializedName("temperature_range")
    val temperatureRange: String?,
    val pruning: Pruning?,
    val sunlight: Sunlight?,
    val tolerant: Tolerance?,
    val humidity: Humidity?,
    val attracts: List<String>?,
    val soil: List<String>?,
    @SerializedName("growth_rate")
    val growthRate: String?,
    @SerializedName("maintenance")
    val maintenance: String?,
    @SerializedName("care_level")
    val careLevel: String?,
    @SerializedName("propagation")
    val propagation: List<String>?,
    @SerializedName("is_pest_susceptible")
    val isPestSusceptible: Boolean?
) : Serializable

@Keep
data class Watering(
    @SerializedName("frequency")
    val frequency: String?,
    @SerializedName("amount")
    val amount: String?,
    @SerializedName("seasonal")
    val seasonal: Map<String, String>?,
    @SerializedName("guide")
    val guide: String?
) : Serializable


@Keep
data class Pruning(
    @SerializedName("when")
    val `when`: String?,
    @SerializedName("how")
    val how: String?,
    @SerializedName("tools")
    val tools: List<String>?,
    val months: List<String>?,
    val guide: String?,
    val count: PruningCount?
) : Serializable


@Keep
data class Tolerance(
    val drought: Boolean?,
    val salt: Boolean?
) : Serializable

@Keep
data class Humidity(
    val min: Int?,
    val max: Int?
) : Serializable

@Keep
data class Sunlight(
    val type: List<String>?,
    val guide: String?
) : Serializable


@Keep
data class PruningCount(
    val amount: Int?,
    val interval: String?
) : Serializable


@Keep
data class CareGuide(
    @SerializedName("watering_schedule")
    val wateringSchedule: String?,
    @SerializedName("fertilizing")
    val fertilizing: String?,
    @SerializedName("pruning")
    val pruning: String?,
    @SerializedName("repotting")
    val repotting: String?,
    @SerializedName("pest_control")
    val pestControl: String?,
    @SerializedName("seasonal_care")
    val seasonalCare: String?
) : Serializable