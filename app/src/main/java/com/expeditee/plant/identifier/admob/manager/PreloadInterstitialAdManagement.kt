package com.expeditee.plant.identifier.admob.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dqh.lmtech.ads.admob.AppOpenManager
import com.dqh.lmtech.ads.ads.wrapper.ApInterstitialAd
import com.dqh.lmtech.ads.billing.AppPurchase
import com.expeditee.plant.identifier.admob.LammaAd
import com.expeditee.plant.identifier.admob.callback.LammaAdCallback
import com.expeditee.plant.identifier.admob.loading.AdLoadingManager
import com.expeditee.plant.identifier.admob.utils.onLoadInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

typealias InterstitialCallback = LammaAdCallback<ApInterstitialAd?>

class PreloadInterstitialAdManagement {

    companion object {
        private const val TAG = "PreloadInterstitialAdManagement"
        private const val LOAD_TIMEOUT = 30000L

        fun showInterAd(
            activity: Activity,
            interAd: ApInterstitialAd?,
            onClosed: Runnable
        ) {
            if (activity.isChangingConfigurations) {
                return onClosed.run()
            }
            if (activity.isDestroyed || activity.isFinishing) {
                return onClosed.run()
            }
            if (interAd == null || interAd.isNotReady) {
                return onClosed.run()
            }
            val interstitialAd = interAd.interstitialAd ?: return onClosed.run()
            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    AppOpenManager.getInstance().isInterstitialShowing = true
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                    onClosed.run()
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    AppOpenManager.getInstance().isInterstitialShowing = false
                    onClosed.run()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AppOpenManager.getInstance().disableAdResumeByClickAction()
                }
            }
            AdLoadingManager.show(activity)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    AppOpenManager.getInstance().isInterstitialShowing = true
                } catch (e: Exception) {
                    AdLoadingManager.dismiss(activity)
                    onClosed.run()
                    return@postDelayed
                }
                AdLoadingManager.dismiss(activity)
                interstitialAd.show(activity)
            }, 800L)
        }
    }

    private val managementScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val interAdCached = ConcurrentHashMap<String, LammaAd<ApInterstitialAd?>>()
    private val interAdLoadingState = ConcurrentHashMap<String, AtomicBoolean>()
    private val interAdCallbacks = ConcurrentHashMap<String, MutableList<InterstitialCallback>>()
    private val interAdLoadingJob = ConcurrentHashMap<String, Job>()

    fun preloadInterAds(
        context: Context,
        placement: String,
        shouldLoad: Boolean,
        admobIds: List<String>,
        callback: InterstitialCallback? = null
    ) {
        callback?.let { addAdCallback(placement, callback) }
        managementScope.launch {
            try {
                if (!shouldLoad || AppPurchase.getInstance().isPurchased) {
                    Log.d(TAG, "Ad can't load or premium for $placement")
                    notifyAdFailed(placement)
                    return@launch
                }
                interAdCached[placement]?.let { interAd ->
                    Log.d(TAG, "Ad already cached for $placement: $interAd")
                    notifyAdLoaded(placement, interAd)
                    return@launch
                }
                withTimeout(LOAD_TIMEOUT) {
                    val isLoading = interAdLoadingState.getOrPut(placement) { AtomicBoolean(false) }
                    if (!isLoading.compareAndSet(false, true)) {
                        Log.d(TAG, "Ad loading already in progress for $placement")
                        return@withTimeout
                    }
                    try {
                        interAdLoadingJob[placement]?.cancel()
                        interAdLoadingJob[placement] = preloadInterAds(
                            context, placement, admobIds
                        )
                        interAdLoadingJob[placement]?.join()
                    } finally {
                        isLoading.set(false)
                        interAdLoadingJob.remove(placement)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Preload timeout after ${LOAD_TIMEOUT}ms")
                notifyAdFailed(placement)
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading ads", e)
                notifyAdFailed(placement)
            }
        }
    }

    private fun preloadInterAds(
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
            interAdCached.getOrPut(placement) { LammaAd(null) }
                .also { it.ad = ad }
                .let { notifyAdLoaded(placement, it) }
        }.onFailure { throwable ->
            Log.e(TAG, "Error preloading retry ads", throwable)
            interAdCached.remove(placement)
            notifyAdFailed(placement)
        }
    }

    private suspend fun loadFirstAvailableAd(
        context: Context,
        unitAdIds: List<String>
    ): ApInterstitialAd? {
        for (unitAdId in unitAdIds) {
            val result = runCatching {
                withTimeout(20_000L) { onLoadInterstitialAd(context, unitAdId) }
            }
            val ad = result.getOrNull()
            if (ad != null) {
                Log.d(TAG, "Loaded ad with unitId=$unitAdId")
                return ad
            }
            Log.w(TAG, "Failed loading ad with unitId=$unitAdId", result.exceptionOrNull())
        }
        return null
    }

    fun preloadInterAd2ID(
        context: Context,
        placement: String,
        shouldLoad: Boolean,
        primaryUnitAdId: String,
        fallbackUnitAdId: String,
        callback: InterstitialCallback? = null
    ) {
        callback?.let { addAdCallback(placement, callback) }
        managementScope.launch {
            try {
                if (!shouldLoad || AppPurchase.getInstance().isPurchased) {
                    Log.d(TAG, "Ad can't load or premium for $placement")
                    notifyAdFailed(placement)
                    return@launch
                }
                interAdCached[placement]?.let { interAd ->
                    Log.d(TAG, "Ad already cached for $placement: $interAd")
                    notifyAdLoaded(placement, interAd)
                    return@launch
                }
                withTimeout(LOAD_TIMEOUT) {
                    val isLoading = interAdLoadingState.getOrPut(placement) { AtomicBoolean(false) }
                    if (!isLoading.compareAndSet(false, true)) {
                        Log.d(TAG, "Ad loading already in progress for $placement")
                        return@withTimeout
                    }
                    try {
                        interAdLoadingJob[placement]?.cancel()
                        interAdLoadingJob[placement] = preloadInterAd2IDRetry(
                            context, placement, primaryUnitAdId, fallbackUnitAdId
                        )
                        interAdLoadingJob[placement]?.join()
                    } finally {
                        isLoading.set(false)
                        interAdLoadingJob.remove(placement)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Preload timeout after ${LOAD_TIMEOUT}ms")
                notifyAdFailed(placement)
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading ads", e)
                notifyAdFailed(placement)
            }
        }
    }

    private fun preloadInterAd2IDRetry(
        context: Context,
        placement: String,
        primaryUnitAdId: String,
        fallbackUnitAdId: String
    ): Job = managementScope.launch {
        try {
            val interAd = interAdCached.getOrPut(placement) { LammaAd(null, Long.MAX_VALUE) }
            val primaryAdAdded = interAd.retryHandler.retry(placement) {
                val apInterstitialAd = onLoadInterstitialAd(context, primaryUnitAdId)
                requireNotNull(apInterstitialAd) { "Ad primary failed to load for $placement" }
                interAd.ad = apInterstitialAd
            }
            if (primaryAdAdded) {
                Log.d(TAG, "Ad loaded primary id successfully for $placement ")
                notifyAdLoaded(placement, interAd)
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed primary id for $placement after ${interAd.retryHandler.maxRetryAttempts} attempts"
            )
            val fallbackAdAdded = interAd.retryHandler.retry(placement) {
                val apInterstitialAd = onLoadInterstitialAd(context, fallbackUnitAdId)
                requireNotNull(apInterstitialAd) { "Ad fallback failed to load for $placement" }
                interAd.ad = apInterstitialAd
            }
            if (fallbackAdAdded) {
                Log.d(TAG, "Ad loaded fallback id successfully for $placement ")
                notifyAdLoaded(placement, interAd)
                return@launch
            }
            Log.d(
                TAG,
                "Ad load failed fallback id for $placement after ${interAd.retryHandler.maxRetryAttempts} attempts"
            )
            interAdCached.remove(placement)
            notifyAdFailed(placement)
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading retry ads ${e.message}")
            notifyAdFailed(placement)
        }
    }

    fun show(
        activity: Activity,
        placement: String,
        runnable: Runnable,
    ) {
        val interAd = interAdCached.remove(placement) ?: return runnable.run()
        showInterAd(activity, interAd.ad, runnable)
    }

    fun showInterstitialAdSplash(
        activity: Activity,
        placement: String,
        runnable: Runnable
    ) {
        show(activity, placement) {
            interAdCached.remove(placement)
            removeAdCallback(placement)
            runnable.run()
        }
    }

    fun showAndReload(
        activity: Activity,
        placement: String,
        primaryUnitAdId: String,
        fallbackUnitAdId: String,
        runnable: Runnable
    ) {
        show(activity, placement) {
            removeAdCallback(placement)
            preloadInterAd2IDRetry(activity, placement, primaryUnitAdId, fallbackUnitAdId)
            runnable.run()
        }
    }

    fun addAdCallback(placement: String, lammaAdCallback: InterstitialCallback) {
        interAdCallbacks.getOrPut(placement) { mutableListOf() }.add(lammaAdCallback)
    }

    fun removeAdCallback(placement: String) {
        interAdCallbacks.remove(placement)
    }

    fun removeAdLoading(placement: String) {
        interAdLoadingJob[placement]?.cancel()
        interAdLoadingJob.remove(placement)
        interAdLoadingState.remove(placement)
    }

    private suspend fun notifyAdLoaded(placement: String, lammaAd: LammaAd<ApInterstitialAd?>) {
        return withContext(Dispatchers.Main.immediate) {
            interAdCallbacks[placement]?.forEach { callback ->
                try {
                    callback.onLoaded(lammaAd.ad)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in callback", e)
                }
            }
        }
    }

    private suspend fun notifyAdFailed(placement: String) {
        return withContext(Dispatchers.Main.immediate) {
            interAdCallbacks[placement]?.forEach { callback ->
                try {
                    callback.onFailed()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in callback", e)
                }
            }
        }
    }

    fun clearAll() {
        interAdCached.clear()
        interAdLoadingJob.values.forEach { it.cancel() }
        interAdLoadingJob.clear()
        interAdCallbacks.clear()
        interAdLoadingState.clear()
    }

    fun cancelScope() {
        if (managementScope.isActive) {
            managementScope.cancel()
        }
    }
}
