package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class GardenStats(
    val totalPlants: Int,
    val totalFlowers: Int,
    val totalCount: Int
)