package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class ExploreCategoryModel(
    val name: String,
    val key: String,
    @DrawableRes val imageRes: Int
)