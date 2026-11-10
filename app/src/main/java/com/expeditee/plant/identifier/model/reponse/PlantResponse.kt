package com.expeditee.plant.identifier.model.reponse

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


@Keep
data class PlantResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("common_names")
    val commonNames: List<String>?,

    @SerializedName("scientific_name")
    val scientificName: String?,

    @SerializedName("main_image")
    val mainImage: PlantImage?
)

@Keep
data class PlantImage(
    @SerializedName("url")
    val url: String?,
    @SerializedName("url_small")
    val urlSmall: String? = null,
)