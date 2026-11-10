package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.PlantScanResult

/**
 * Holds last identify / QR lookup payload for thesis detail screen.
 */
object PlantScanSession {
    @Volatile
    var lastResult: PlantScanResult? = null
        private set

    @Volatile
    var lastLocalImagePath: String? = null
        private set

    fun store(result: PlantScanResult, localImagePath: String? = null) {
        lastResult = result
        if (localImagePath != null) {
            lastLocalImagePath = localImagePath
        }
    }

    fun clear() {
        lastResult = null
        lastLocalImagePath = null
    }
}
