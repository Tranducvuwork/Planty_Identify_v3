package com.expeditee.plant.identifier.presenter.home.uihome

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.WeatherRepository
import com.expeditee.plant.identifier.helper.WeatherHelper
import com.expeditee.plant.identifier.model.WeatherInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class HomeTabViewModel(
    private val weatherRepository: WeatherRepository,
    application: Application
) : BaseViewModel<HomeTabViewModel.HomeState>(application) {
    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Initial)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var currentUnit: String = HOME_TEMPERATURE_UNIT

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    init {
        _homeState.value = HomeState.Success(data = null)
    }

    fun checkUnitChanged(context: Context) {
        val newUnit = HOME_TEMPERATURE_UNIT
        if (newUnit != currentUnit) {
            currentUnit = newUnit
            _currentLocation.value?.let {
                fetchWeatherAndLocation(it.latitude, it.longitude)
            }
        }
    }

    fun initLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(
            context.applicationContext,
        )
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(context: Context) {
        if (!hasLocationPermission(context)) {
            _weatherState.value = WeatherState.PermissionRequired
            return
        }
        if (!isLocationEnabled(context)) {
            _weatherState.value = WeatherState.PermissionRequired
            return
        }

        _weatherState.value = WeatherState.Loading
        if (!::fusedLocationClient.isInitialized) {
            initLocationClient(context)
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null) {
                        onLocationReady(last)
                    } else {
                        requestCurrentLocation()
                    }
                }
                .addOnFailureListener { requestCurrentLocation() }
        } catch (_: Exception) {
            requestCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReady(location)
                } else {
                    requestFreshLocationUpdate()
                }
            }.addOnFailureListener {
                requestFreshLocationUpdate()
            }
        } catch (_: Exception) {
            requestFreshLocationUpdate()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocationUpdate() {
        removeLocationUpdates()
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2_000L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .setDurationMillis(12_000L)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                removeLocationUpdates()
                val location = result.lastLocation
                if (location != null) {
                    onLocationReady(location)
                } else {
                    _weatherState.value = WeatherState.Error()
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper(),
            )
        } catch (_: Exception) {
            _weatherState.value = WeatherState.Error()
        }
    }

    private fun onLocationReady(location: Location) {
        _currentLocation.value = location
        fetchWeatherAndLocation(location.latitude, location.longitude)
    }

    private fun fetchWeatherAndLocation(latitude: Double, longitude: Double) {
        launchBlock {
            val unit = HOME_TEMPERATURE_UNIT
            val weatherResult = weatherRepository.getWeatherData(latitude, longitude, unit)
            val locationName = resolveLocationName(latitude, longitude)

            weatherResult.onSuccess { response ->
                val current = response.current_weather
                val daily = response.daily
                val unitSymbol = if (unit.equals("celsius", true)) "°C" else "°F"
                val temperature = "${current.temperature.toInt()} $unitSymbol"
                val minTemp = if (daily.temperature_2m_min.isNotEmpty()) {
                    "min ${daily.temperature_2m_min[0].toInt()}$unitSymbol"
                } else {
                    ""
                }
                val maxTemp = if (daily.temperature_2m_max.isNotEmpty()) {
                    "max ${daily.temperature_2m_max[0].toInt()}$unitSymbol"
                } else {
                    ""
                }
                _weatherState.value = WeatherState.Success(
                    WeatherInfo(
                        temperature = temperature,
                        minTemperature = minTemp,
                        maxTemperature = maxTemp,
                        location = locationName,
                        weatherIcon = WeatherHelper.getWeatherIcon(
                            current.weathercode, latitude, longitude,
                        ),
                    ),
                )
            }.onFailure {
                _weatherState.value = WeatherState.Error(locationName)
            }
        }
    }

    private fun resolveLocationName(latitude: Double, longitude: Double): String {
        return try {
            val addresses = Geocoder(applicationContext, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                val city = address.locality ?: address.subAdminArea ?: address.adminArea
                val country = address.countryName
                when {
                    city != null && country != null -> "$city, $country"
                    city != null -> city
                    country != null -> country
                    else -> null
                }
            } ?: applicationContext.getString(com.expeditee.plant.identifier.R.string.current_location_label)
        } catch (_: Exception) {
            applicationContext.getString(com.expeditee.plant.identifier.R.string.current_location_label)
        }
    }

    private fun removeLocationUpdates() {
        val callback = locationCallback ?: return
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun hasLocationPermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED


    override fun onState(state: HomeState) {}


    sealed class HomeState : State {
        data object Loading : HomeState()
        data class Success(val data: Any?) : HomeState()
        data class Error(val message: String) : HomeState()
    }

    sealed class WeatherState {
        data object Initial : WeatherState()
        data object Loading : WeatherState()
        data object PermissionRequired : WeatherState()
        data class Error(val locationName: String? = null) : WeatherState()
        data class Success(val data: WeatherInfo) : WeatherState()
    }

    override fun onCleared() {
        removeLocationUpdates()
        super.onCleared()
    }

    companion object {
        private const val HOME_TEMPERATURE_UNIT = "celsius"
    }
}
