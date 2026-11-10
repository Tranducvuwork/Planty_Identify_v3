package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class PopularPlantModel(
    val id: String = "",
    val name: String,
    val scientificName: String = "",
    val imageUrl: String?,
    val type: String = TYPE_PLANT
) {
    companion object {
        const val TYPE_FLOWER = "Flower"
        const val TYPE_PLANT = "Plant"
    }
}