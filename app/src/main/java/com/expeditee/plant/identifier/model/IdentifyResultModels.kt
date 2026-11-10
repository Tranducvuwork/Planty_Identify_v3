package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class DiseaseImageModel(
    val title: String,
    @DrawableRes val imageRes: Int
)

@Keep
data class ClassificationItemModel(
    val label: String,
    val value: String
)

@Keep
data class RequirementItemModel(
    val title: String,
    val value: String,
    @DrawableRes val iconRes: Int
)
