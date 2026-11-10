package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class HealthStatisticsDto(
    val healthyCount: Int,
    val unhealthyCount: Int
)