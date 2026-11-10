package com.expeditee.plant.identifier.services

import android.util.Log
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.model.ModeScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineModeService(
    private val appConfigSettings: AppConfigSettings,
) {
    private val _scanModeFlow = MutableStateFlow(ModeScan.ONLINE)
    val scanModeFlow: StateFlow<ModeScan> = _scanModeFlow.asStateFlow()

    init {
        initializeScanMode()
    }

    fun initializeScanMode() {
        val mode = appConfigSettings.scanMode
        _scanModeFlow.value = mode
    }

    fun setScanMode(mode: ModeScan) {
        appConfigSettings.scanMode = mode
        _scanModeFlow.value = mode
    }
}
