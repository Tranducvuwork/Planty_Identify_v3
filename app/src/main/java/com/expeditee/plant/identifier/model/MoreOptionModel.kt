package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class MoreOptionModel(
    val title: String,
    @DrawableRes val iconRes: Int
)