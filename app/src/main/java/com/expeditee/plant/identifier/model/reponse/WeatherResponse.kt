package com.expeditee.plant.identifier.model.reponse

import androidx.annotation.Keep

@Keep
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current_weather: CurrentWeather,
    val daily: DailyWeather
)

@Keep
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)

@Keep
data class DailyWeather(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weathercode: List<Int>
)
