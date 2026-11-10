package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class MyGardenPlantModel(
    val id: Int,
    val name: String,
    val scientificName: String = "",
    val imageUrl: String?,
    val status: String = "Unknown"
)