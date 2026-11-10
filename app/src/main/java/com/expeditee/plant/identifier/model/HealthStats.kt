package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class HealthStats(
    val healthyCount: Int,
    val unhealthyCount: Int
)