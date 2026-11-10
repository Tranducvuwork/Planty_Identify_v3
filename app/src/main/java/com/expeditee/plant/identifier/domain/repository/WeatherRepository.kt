package com.expeditee.plant.identifier.domain.repository

import com.expeditee.plant.identifier.domain.weather.WeatherApiService
import com.expeditee.plant.identifier.model.reponse.WeatherResponse

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    suspend fun getWeatherData(
        latitude: Double,
        longitude: Double,
        temperatureUnit: String = "fahrenheit"
    ): Result<WeatherResponse> {
        return try {
            val response = weatherApiService.getWeather(
                lat = latitude,
                lon = longitude,
                temperatureUnit = temperatureUnit
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
