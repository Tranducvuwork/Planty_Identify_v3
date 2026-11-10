package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class GardenStatisticsDto(
    val plantCount: Int,
    val flowerCount: Int,
    val totalCount: Int
)