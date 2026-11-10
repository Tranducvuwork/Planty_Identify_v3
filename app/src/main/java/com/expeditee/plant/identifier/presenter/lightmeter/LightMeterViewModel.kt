package com.expeditee.plant.identifier.presenter.lightmeter

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.expeditee.plant.identifier.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class LightMeterViewModel(application: Application) :
    BaseViewModel<LightMeterState>(application), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    private val _lightState = MutableStateFlow<LightMeterState>(LightMeterState.Idle)
    val lightState: StateFlow<LightMeterState> = _lightState.asStateFlow()

    fun registerLightSensor() {
        sensorManager = getApplication<Application>().getSystemService(SensorManager::class.java)
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterLightSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0].roundToInt()
            val ratio = (lux - MIN_LUX).toFloat() / (MAX_LUX - MIN_LUX)
            _lightState.value = LightMeterState.LuxChanged(lux, ratio.coerceIn(0f, 1f))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onState(state: LightMeterState) = Unit

    companion object {
        private const val MIN_LUX = 0
        private const val MAX_LUX = 10000
    }
}

sealed class LightMeterState : BaseViewModel.State {
    data object Idle : LightMeterState()
    data class LuxChanged(val lux: Int, val ratio: Float) : LightMeterState()
}
