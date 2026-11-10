package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class WeatherInfo(
    val temperature: String = "",
    val minTemperature: String = "",
    val maxTemperature: String = "",
    val location: String = "Location unavailable",
    val weatherIcon: Int = 0,
)