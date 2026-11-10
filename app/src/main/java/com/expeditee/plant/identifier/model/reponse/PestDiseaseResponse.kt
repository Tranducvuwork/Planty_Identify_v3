package com.expeditee.plant.identifier.model.reponse

import com.expeditee.plant.identifier.model.PestDisease
import com.google.gson.annotations.SerializedName

data class PestDiseaseResponse(
    @SerializedName("data")
    val data: List<PestDiseaseDto>?
)

data class PestDiseaseDto(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("common_name")
    val commonName: String?,
    @SerializedName("scientific_name")
    val scientificName: String?,
    @SerializedName("other_name")
    val otherName: List<String>?,
    @SerializedName("family")
    val family: String?,
    @SerializedName("description")
    val description: List<PestDiseaseSectionDto>?,
    @SerializedName("solution")
    val solution: List<PestDiseaseSectionDto>?,
    @SerializedName("host")
    val host: List<String>?,
    @SerializedName("images")
    val images: List<PestDiseaseImageDto>?
)

data class PestDiseaseSectionDto(
    @SerializedName("subtitle")
    val subtitle: String?,
    @SerializedName("description")
    val description: String?
)

data class PestDiseaseImageDto(
    @SerializedName("original_url")
    val originalUrl: String?,
    @SerializedName("regular_url")
    val regularUrl: String?,
    @SerializedName("medium_url")
    val mediumUrl: String?,
    @SerializedName("small_url")
    val smallUrl: String?,
    @SerializedName("thumbnail")
    val thumbnail: String?
)

fun PestDiseaseDto.toModel(): PestDisease {
    return PestDisease(
        id = id ?: 0,
        commonName = commonName.orEmpty().trim(),
        scientificName = scientificName.orEmpty().trim(),
        family = family.orEmpty().trim(),
        otherName = otherName.orEmpty(),
        description = description.orEmpty().map {
            PestDisease.Section(
                subtitle = it.subtitle.orEmpty().trim(),
                description = it.description.orEmpty().trim()
            )
        },
        solution = solution.orEmpty().map {
            PestDisease.Section(
                subtitle = it.subtitle.orEmpty().trim(),
                description = it.description.orEmpty().trim()
            )
        },
        host = host.orEmpty(),
        images = images.orEmpty().map {
            PestDisease.Image(
                originalUrl = it.originalUrl.orEmpty(),
                regularUrl = it.regularUrl.orEmpty(),
                mediumUrl = it.mediumUrl.orEmpty(),
                smallUrl = it.smallUrl.orEmpty(),
                thumbnail = it.thumbnail.orEmpty()
            )
        }
    )
}
