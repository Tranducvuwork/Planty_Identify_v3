package com.expeditee.plant.identifier.admob

import android.os.SystemClock
import androidx.annotation.Keep
import com.expeditee.plant.identifier.admob.handler.AdmobExpirationHandler
import com.expeditee.plant.identifier.admob.handler.AdmobRetryHandler
import com.expeditee.plant.identifier.admob.handler.ExpirationHandler
import com.expeditee.plant.identifier.admob.handler.RetryHandler

@Keep
data class LammaAd<Ad>(
    var ad: Ad?,
    val adTimeOut: Long = 10 * 1000,
    val attemptReload: Int = 2,
    val retryHandler: RetryHandler = AdmobRetryHandler(maxRetryAttempts = attemptReload),
    val expirationHandler: ExpirationHandler = AdmobExpirationHandler(adTimeOut),
) {

    var timeLoaded: Long = SystemClock.elapsedRealtime()

    fun isAdExpired(currentTimeMillis: Long = SystemClock.elapsedRealtime()): Boolean {
        return expirationHandler.isAdExpired(currentTimeMillis, timeLoaded)
    }

    fun isAvailable(): Boolean {
        return ad != null
    }
}
