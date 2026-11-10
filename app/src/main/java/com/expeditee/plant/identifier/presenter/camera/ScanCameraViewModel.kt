package com.expeditee.plant.identifier.presenter.camera

import android.app.Application
import android.util.Log
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.FlashState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ScanCameraState : BaseViewModel.State {
    data class SetMode(val mode: CameraMode) : ScanCameraState()
    data object ToggleFlash : ScanCameraState()
}

class ScanCameraViewModel(application: Application) : BaseViewModel<ScanCameraState>(application) {
    companion object {
        private const val TAG = "TAG123213"
    }

    private val _cameraModes = MutableStateFlow(CameraMode.ALL_MODES)
    val cameraModes: StateFlow<List<CameraMode>> = _cameraModes.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.IDENTIFY)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    private val _flashState = MutableStateFlow(FlashState.OFF)
    val flashState: StateFlow<FlashState> = _flashState.asStateFlow()

    override fun onState(state: ScanCameraState) {
        Log.d(TAG, "New state received: $state")
        when (state) {
            is ScanCameraState.SetMode -> applyScanMode(state.mode)
            ScanCameraState.ToggleFlash -> toggleFlash()
        }
    }

    fun applyInitialScanMode(mode: CameraMode) {
        Log.d(TAG, "Applying initial scan mode: $mode")
        applyScanMode(mode)
    }

    private fun applyScanMode(mode: CameraMode) {
        Log.i(TAG, "Setting camera mode to: $mode")
        _cameraMode.value = mode
        _cameraModes.value = CameraMode.ALL_MODES.map {
            it.copy(isSelected = it.typeMode == mode.typeMode)
        }
    }

    private fun toggleFlash() {
        val newState = when (_flashState.value) {
            FlashState.OFF -> FlashState.ON
            FlashState.ON -> FlashState.OFF
        }
        Log.d(TAG, "Toggling flash. Old state: ${_flashState.value}, New state: $newState")
        _flashState.value = newState
    }
}
