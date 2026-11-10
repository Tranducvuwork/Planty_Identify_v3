package com.expeditee.plant.identifier.helper

import com.expeditee.plant.identifier.R

object WeatherHelper {

    fun getWeatherIcon(weatherCode: Int, isDay: Boolean = true): Int {
        return when (weatherCode) {
            0 -> if (isDay) R.drawable.ic_weather_sunny_day else R.drawable.ic_weather_clear_night

            1, 2, 3 -> if (isDay) R.drawable.ic_weather_partly_cloudy_day else R.drawable.ic_weather_partly_cloudy_night

            45, 48 -> if (isDay) R.drawable.ic_weather_fog_day else R.drawable.ic_weather_fog_night

            51, 53, 55, 56, 57 -> if (isDay) R.drawable.ic_weather_drizzle_day else R.drawable.ic_weather_drizzle_night

            61, 63, 65, 66, 67 -> if (isDay) R.drawable.ic_weather_rain_day else R.drawable.ic_weather_rain_night

            71, 73, 75, 77 -> if (isDay) R.drawable.ic_weather_snow_day else R.drawable.ic_weather_snow_night

            80, 81, 82 -> if (isDay) R.drawable.ic_weather_rain_shower_day else R.drawable.ic_weather_rain_shower_night

            85, 86 -> if (isDay) R.drawable.ic_weather_snow_shower_day else R.drawable.ic_weather_snow_shower_night

            95, 96, 99 -> if (isDay) R.drawable.ic_weather_thunderstorm_day else R.drawable.ic_weather_thunderstorm_night

            else -> R.drawable.ic_weather_home
        }
    }

    fun getWeatherIcon(weatherCode: Int, latitude: Double, longitude: Double): Int {
        val isDay = isDayTimeAtLocation(latitude, longitude)
        return getWeatherIcon(weatherCode, isDay)
    }


    private fun isDayTimeAtLocation(latitude: Double, longitude: Double): Boolean {
        return try {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..18

            when {
                latitude > 60.0 -> isDay
                latitude < -60.0 -> !isDay
                else -> isDay
            }
        } catch (e: Exception) {
            true
        }
    }
}