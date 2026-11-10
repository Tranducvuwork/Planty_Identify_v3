package com.expeditee.plant.identifier.model

import androidx.annotation.Keep
import androidx.annotation.DrawableRes

@Keep

data class GardenOption(
    val id: String,
    val name: String,
    @DrawableRes
    val imageRes: Int = 0,
    val imageUrl: String? = null
)