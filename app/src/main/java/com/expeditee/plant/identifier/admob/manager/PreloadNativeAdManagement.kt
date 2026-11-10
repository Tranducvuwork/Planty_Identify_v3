package com.expeditee.plant.identifier.admob.manager

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.dqh.lmtech.ads.billing.AppPurchase
import com.expeditee.plant.identifier.admob.LammaAd
import com.expeditee.plant.identifier.admob.callback.LammaAdCallback
import com.expeditee.plant.identifier.admob.utils.onLoadNativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

typealias NativeCallback = LammaAdCallback<LammaAd<ApNativeAd>?>

class PreloadNativeAdManagement {

    companion object {

        private const val TAG = "PreloadNativeAdManagement"
        private const val LOAD_TIMEOUT = 30000L
    }

    private val managementScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val nativeAdCached = ConcurrentHashMap<String, LammaAd<ApNativeAd>>()
    private val nativeAdLoadingState = ConcurrentHashMap<String, AtomicBoolean>()
    private val nativeAdCallbacks = ConcurrentHashMap<String, CopyOnWriteArrayList<NativeCallback>>()
    private val nativeAdLoadingJob = ConcurrentHashMap<String, Job>()

    fun getNativeAd(placement: String): LammaAd<ApNativeAd>? {
        return nativeAdCached[placement]
    }

    fun consumeNative(placement: String): LammaAd<ApNativeAd>? {
        return nativeAdCached.remove(placement)
    }

    fun removeCache(placement: String) {
        nativeAdCached.remove(placement)
    }

    fun preloadNativeAd(
        context: Context,
        placement: String,
        unitId: String,
        adTimeout: Long = Long.MAX_VALUE,
        retryAttempts: Int = 1,
        callback: NativeCallback? = null
    ) {
        callback?.let { addAdCallback(placement, callback) }
        managementScope.launch {
            try {
                if (AppPurchase.getInstance().isPurchased) {
                    Log.d(TAG, "Ad can't load or premium for $placement")
                    notifyAdFailed(placement)
                    return@launch
                }
                nativeAdCached[placement]?.let { cachedAd ->
                    Log.d(TAG, "Ad already cached for $placement: $cachedAd")
                    notifyAdLoaded(placement, cachedAd)
                    if (cachedAd.isAdExpired()) {
                        Log.d(TAG, "Ad is expired, reload by time for $placement: $cachedAd")
                        return@let
                    }
                    return@launch
                }
                withTimeout(LOAD_TIMEOUT) {
                    val loading = nativeAdLoadingState.getOrPut(placement) { AtomicBoolean(false) }
                    if (!loading.compareAndSet(false, true)) {
                        Log.d(TAG, "Ad loading already in progress for $placement")
                        return@withTimeout
                    }
                    try {
                        nativeAdLoadingJob[placement]?.cancel()
                        nativeAdLoadingJob[placement] = preloadNativeAdRetry(
                            context, placement, unitId, adTimeout, retryAttempts
                        )
                        nativeAdLoadingJob[placement]?.join()
                    } finally {
                        loading.set(false)
                        nativeAdLoadingJob.remove(placement)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Preload timeout after ${LOAD_TIMEOUT}ms")
                notifyAdFailed(placement)
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading ads ${e.message}")
                notifyAdFailed(placement)
            }
        }
    }

    fun preloadNativeAd(
        context: Context,
        placement: String,
        shouldLoad: Boolean,
        admobIds: List<String>,
        callback: NativeCallback? = null
    ) {
        callback?.let { addAdCallback(placement, callback) }
        managementScope.launch {
            try {
                if (!shouldLoad || AppPurchase.getInstance().isPurchased) {
                    Log.d(TAG, "Ad can't load or premium for $placement")
                    notifyAdFailed(placement)
                    return@launch
                }
                nativeAdCached[placement]?.let { cachedAd ->
                    Log.d(TAG, "Ad already cached for $placement: $cachedAd")
                    notifyAdLoaded(placement, cachedAd)
                    return@launch
                }
                withTimeout(LOAD_TIMEOUT) {
                    val loading = nativeAdLoadingState.getOrPut(placement) { AtomicBoolean(false) }
                    if (!loading.compareAndSet(false, true)) {
                        Log.d(TAG, "Ad loading already in progress for $placement")
                        return@withTimeout
                    }
                    try {
                        nativeAdLoadingJob[placement]?.cancel()
                        nativeAdLoadingJob[placement] = preloadNativeAdRetry(
                            context, placement, admobIds
                        )
                        nativeAdLoadingJob[placement]?.join()
                    } finally {
                        loading.set(false)
                        nativeAdLoadingJob.remove(placement)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Preload timeout after ${LOAD_TIMEOUT}ms")
                notifyAdFailed(placement)
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading ads ${e.message}")
                notifyAdFailed(placement)
            }
        }
    }

    private fun preloadNativeAdRetry(
        context: Context,
        placement: String,
        admobIds: List<String>
    ): Job = managementScope.launch {
        runCatching {
            val loadedAd = loadFirstAvailableAd(context, admobIds)
            requireNotNull(loadedAd) {
                "All ad ids failed for placement: $placement"
            }
            loadedAd
        }.onSuccess { ad ->
            Log.d(TAG, "Ad loaded successfully for $placement")
            nativeAdCached.getOrPut(placement) { LammaAd(null) }
                .also { it.ad = ad }
                .let { notifyAdLoaded(placement, it) }
        }.onFailure { throwable ->
            Log.e(TAG, "Error preloading retry ads", throwable)
            nativeAdCached.remove(placement)
            notifyAdFailed(placement)
        }
    }

    private suspend fun loadFirstAvailableAd(
        context: Context,
        unitAdIds: List<String>
    ): ApNativeAd? {
        for (unitAdId in unitAdIds) {
            val result = runCatching { withTimeout(20_000L) { onLoadNativeAd(context, unitAdId) } }
            val ad = result.getOrNull()
            if (ad != null) {
                Log.d(TAG, "Loaded ad with unitId=$unitAdId")
                return ad
            }
            Log.w(TAG, "Failed loading ad with unitId=$unitAdId", result.exceptionOrNull())
        }
        return null
    }

    private fun preloadNativeAdRetry(
        context: Context,
        placement: String,
        unitAdId: String,
        adTimeout: Long,
        retryAttempts: Int = 2
    ): Job = managementScope.launch {
        try {
            val nativeAd = nativeAdCached.getOrPut(placement) {
                LammaAd(null, adTimeout, retryAttempts)
            }
            val nativeAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, unitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (!nativeAdded) {
                Log.e(
                    TAG,
                    "Failed to load ad for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
                )
                nativeAdCached.remove(placement)
                notifyAdFailed(placement)
                return@launch
            }
            Log.d(TAG, "Ad loaded successfully for $placement")
            notifyAdLoaded(placement, nativeAd)
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading retry ads", e)
            notifyAdFailed(placement)
        }
    }

    fun preloadNativeAdRetry(
        context: Context,
        placement: String = context::class.java.simpleName,
        unitAdId: String,
        retryAttempts: Int = 1,
        callback: NativeCallback?
    ): Job = managementScope.launch {
        try {
            val nativeAd = LammaAd<ApNativeAd>(null, Long.MAX_VALUE, retryAttempts)
            val nativeAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, unitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (!nativeAdded) {
                Log.e(
                    TAG,
                    "Failed to load ad for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
                )
                withContext(Dispatchers.Main.immediate) {
                    callback?.onFailed()
                }
                return@launch
            }
            Log.d(TAG, "Ad loaded successfully for $placement")
            withContext(Dispatchers.Main.immediate) {
                callback?.onLoaded(nativeAd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading retry ads", e)
            withContext(Dispatchers.Main.immediate) {
                callback?.onFailed()
            }
        }
    }

    fun preloadNativeAd2ID(
        context: Context,
        placement: String,
        primaryUnitAdId: String,
        fallbackUnitAdId: String,
        adTimeout: Long = Long.MAX_VALUE,
        attemptReload: Int = 1,
        callback: NativeCallback? = null
    ) {
        callback?.let { addAdCallback(placement, callback) }
        managementScope.launch {
            try {
                if (AppPurchase.getInstance().isPurchased) {
                    Log.d(TAG, "Ad can't load or premium for $placement")
                    notifyAdFailed(placement)
                    return@launch
                }
                nativeAdCached[placement]?.let { cachedAd ->
                    Log.d(TAG, "Ad already cached for $placement: $cachedAd")
                    notifyAdLoaded(placement, cachedAd)
                    if (cachedAd.isAdExpired()) {
                        Log.d(TAG, "Ad is expired, reload by time for $placement: $cachedAd")
                        return@let
                    }
                    return@launch
                }
                withTimeout(LOAD_TIMEOUT) {
                    val loading = nativeAdLoadingState.getOrPut(placement) { AtomicBoolean(false) }
                    if (!loading.compareAndSet(false, true)) {
                        Log.d(TAG, "Ad loading already in progress for $placement")
                        return@withTimeout
                    }
                    try {
                        nativeAdLoadingJob[placement]?.cancel()
                        nativeAdLoadingJob[placement] = preloadNativeAdRetry2ID(
                            context, placement,
                            primaryUnitAdId, fallbackUnitAdId,
                            adTimeout, attemptReload
                        )
                        nativeAdLoadingJob[placement]?.join()
                    } finally {
                        loading.set(false)
                        nativeAdLoadingJob.remove(placement)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Preload timeout after ${LOAD_TIMEOUT}ms")
                notifyAdFailed(placement)
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading ads ${e.message}")
                notifyAdFailed(placement)
            }
        }
    }

    private suspend fun preloadNativeAdRetry2ID(
        context: Context,
        placement: String,
        primaryUnitAdId: String,
        fallbackUnitAdId: String,
        adTimeout: Long,
        attemptReload: Int
    ): Job = managementScope.launch {
        try {
            val nativeAd = nativeAdCached.getOrPut(placement) {
                LammaAd(null, adTimeout, attemptReload)
            }
            val primaryAdAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, primaryUnitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (primaryAdAdded) {
                Log.d(TAG, "Ad loaded primary id successfully for $placement ")
                notifyAdLoaded(placement, nativeAd)
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed primary id for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
            )
            val fallbackAdAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, fallbackUnitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (fallbackAdAdded) {
                Log.d(TAG, "Ad loaded fallback id successfully for $placement ")
                notifyAdLoaded(placement, nativeAd)
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed fallback id for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
            )
            nativeAdCached.remove(placement)
            notifyAdFailed(placement)
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading retry ads", e)
            notifyAdFailed(placement)
        }
    }

    fun preloadNativeAdRetry2ID(
        context: Context,
        placement: String = context::class.java.simpleName,
        primaryUnitAdId: String,
        fallbackUnitAdId: String,
        attemptReload: Int = 1,
        callback: NativeCallback?
    ): Job = managementScope.launch {
        try {
            val nativeAd = LammaAd<ApNativeAd>(null, Long.MAX_VALUE, attemptReload)
            val primaryAdAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, primaryUnitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (primaryAdAdded) {
                Log.d(TAG, "Ad loaded primary id successfully for $placement ")
                withContext(Dispatchers.Main.immediate) {
                    callback?.onLoaded(nativeAd)
                }
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed primary id for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
            )
            val fallbackAdAdded = nativeAd.retryHandler.retry(placement) {
                val apNativeAd = onLoadNativeAd(context, fallbackUnitAdId)
                requireNotNull(apNativeAd) { "Ad failed to load for $placement" }
                nativeAd.ad = apNativeAd
                nativeAd.timeLoaded = SystemClock.elapsedRealtime()
            }
            if (fallbackAdAdded) {
                Log.d(TAG, "Ad loaded fallback id successfully for $placement ")
                withContext(Dispatchers.Main.immediate) {
                    callback?.onLoaded(nativeAd)
                }
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed fallback id for $placement after ${nativeAd.retryHandler.maxRetryAttempts} attempts"
            )
            withContext(Dispatchers.Main.immediate) {
                callback?.onFailed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading retry ads", e)
            withContext(Dispatchers.Main.immediate) {
                callback?.onFailed()
            }
        }
    }

    fun addAdCallback(placement: String, lammaAdCallback: NativeCallback) {
        val callbacks = nativeAdCallbacks.getOrPut(placement) { CopyOnWriteArrayList() }
        if (callbacks.any { it === lammaAdCallback }.not()) {
            callbacks.add(lammaAdCallback)
        }
    }

    fun removeAdCallback(placement: String) {
        nativeAdCallbacks.remove(placement)
    }

    fun removeAdCallback(callback: NativeCallback) {
        Log.d(TAG, "removeAdCallback: $callback")
        Log.d(
            TAG,
            "removeAdCallback1: ${nativeAdCallbacks.values.any { it.any { it === callback } }}"
        )
        nativeAdCallbacks.values.forEach { list ->
            list.removeAll { it === callback }
        }
        Log.d(
            TAG,
            "removeAdCallback2: ${nativeAdCallbacks.values.any { it.any { it === callback } }}"
        )
    }

    fun removeAdLoading(placement: String) {
        nativeAdLoadingJob[placement]?.cancel()
        nativeAdLoadingJob.remove(placement)
        nativeAdLoadingState.remove(placement)
    }

    private suspend fun notifyAdLoaded(placement: String, lammaAd: LammaAd<ApNativeAd>) {
        return withContext(Dispatchers.Main.immediate) {
            nativeAdCallbacks[placement]?.forEach { callback ->
                try {
                    callback.onLoaded(lammaAd)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in callback", e)
                }
            }
        }
    }

    private suspend fun notifyAdFailed(placement: String) {
        return withContext(Dispatchers.Main.immediate) {
            nativeAdCallbacks[placement]?.forEach { callback ->
                try {
                    callback.onFailed()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in callback", e)
                }
            }
        }
    }

    fun clearAll() {
        nativeAdCached.clear()
        nativeAdLoadingJob.values.forEach { it.cancel() }
        nativeAdLoadingJob.clear()
        nativeAdCallbacks.clear()
        nativeAdLoadingState.clear()
    }

    fun cancelScope() {
        if (managementScope.isActive) {
            managementScope.cancel()
        }
    }
}
