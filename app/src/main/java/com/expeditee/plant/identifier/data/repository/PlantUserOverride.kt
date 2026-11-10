package com.expeditee.plant.identifier.data.repository

import com.expeditee.plant.identifier.network.model.CareData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import com.google.gson.annotations.SerializedName

data class PlantUserOverride(
    @SerializedName("scientificName") val scientificName: String,
    @SerializedName("commonNames") val commonNames: List<String> = emptyList(),
    @SerializedName("description") val description: String? = null,
    @SerializedName("uses") val uses: List<String> = emptyList(),
    @SerializedName("imagePath") val imagePath: String? = null,
    @SerializedName("family") val family: String? = null,
    @SerializedName("origin") val origin: String? = null,
    @SerializedName("morphology") val morphology: String? = null,
    @SerializedName("planting") val planting: String? = null,
    @SerializedName("care") val care: String? = null,
) {
    fun applyTo(result: PlantScanResult): PlantScanResult {
        val merged = merge(
            commonNames = result.commonNames,
            description = result.description,
            uses = result.uses,
            imageUrl = result.imageUrl,
            origin = result.origin,
            override = this,
        )
        val nextCare = result.care?.copy(
            planting = planting?.takeIf { it.isNotBlank() } ?: result.care.planting,
            care = care?.takeIf { it.isNotBlank() } ?: result.care.care,
        ) ?: if (!planting.isNullOrBlank() || !care.isNullOrBlank()) {
            CareData(planting = planting, care = care)
        } else {
            null
        }
        return result.copy(
            scientificName = scientificName.takeIf { it.isNotBlank() } ?: result.scientificName,
            commonNames = merged.commonNames,
            description = merged.description,
            uses = merged.uses,
            imageUrl = merged.imageUrl,
            family = family?.takeIf { it.isNotBlank() } ?: result.family,
            origin = merged.origin,
            morphology = morphology?.takeIf { it.isNotBlank() } ?: result.morphology,
            care = nextCare,
        )
    }

    companion object {
        data class MergedFields(
            val commonNames: List<String>,
            val description: String?,
            val uses: List<String>,
            val imageUrl: String?,
            val origin: String?,
        )

        fun merge(
            commonNames: List<String>,
            description: String?,
            uses: List<String>,
            imageUrl: String?,
            origin: String?,
            override: PlantUserOverride?,
        ): MergedFields {
            if (override == null) {
                return MergedFields(commonNames, description, uses, imageUrl, origin)
            }
            return MergedFields(
                commonNames = override.commonNames.filter { it.isNotBlank() }.ifEmpty { commonNames },
                description = override.description?.takeIf { it.isNotBlank() } ?: description,
                uses = override.uses.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { uses },
                imageUrl = override.imagePath?.takeIf { it.isNotBlank() } ?: imageUrl,
                origin = override.origin?.takeIf { it.isNotBlank() } ?: origin,
            )
        }
    }
}
