package com.expeditee.plant.identifier.domain.weather

import com.expeditee.plant.identifier.model.reponse.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit"
    ): WeatherResponse
}