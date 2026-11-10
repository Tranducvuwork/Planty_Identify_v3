package com.expeditee.plant.identifier.services

import com.expeditee.plant.identifier.data.local.ScanLimitPreferences
import com.expeditee.plant.identifier.model.ScanCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScanLimitService(private val prefs: ScanLimitPreferences) {

    private val lock = Any()

    private val _scanCountFlow = MutableStateFlow(prefs.snapshot())
    val scanCountFlow: StateFlow<ScanCount> = _scanCountFlow.asStateFlow()

    fun refreshQuotaFromStorage() = synchronized(lock) {
        prefs.checkAndResetDailyScans()
        _scanCountFlow.value = prefs.snapshot()
    }

    fun consumeFreeScan(): Boolean = synchronized(lock) {
        prefs.checkAndResetDailyScans()
        if (!prefs.snapshot().isFreeAvailable) return@synchronized false
        prefs.incrementFreeScansUsed()
        _scanCountFlow.value = prefs.snapshot()
        true
    }

    fun consumeRewardScan(): Boolean = synchronized(lock) {
        if (!prefs.snapshot().isRewardAvailable) return@synchronized false
        prefs.incrementRewardScansUsed()
        _scanCountFlow.value = prefs.snapshot()
        true
    }
}
