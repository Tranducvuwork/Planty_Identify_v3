package com.expeditee.plant.identifier.presenter.splash

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dqh.lmtech.ads.ads.wrapper.ApInterstitialAd
import com.dqh.lmtech.ads.billing.AppPurchase
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.admob.manager.PreloadInterstitialAdManagement
import com.expeditee.plant.identifier.admob.utils.onLoadInterstitialAd
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashInterHelper(private val configManagement: RemoteConfigManagement) {

    companion object {
        private const val TAG = "SplashInterHelper"
    }

    private val loadHelperScope: CoroutineScope = CoroutineScope(
        Dispatchers.Main.immediate + SupervisorJob()
    )

    private val _timeoutState = Channel<Unit>(Channel.BUFFERED)
    private val timeoutState = _timeoutState.receiveAsFlow()

    private var interLoadJob: Job? = null

    private var handlerTimeout: Handler? = null
    private var runnableTimeout: Runnable? = null

    private var onTimeoutCallback: OnTimeoutCallback? = null
    private var onApInterstitialAdCallback: OnLoadInterstitialCallback? = null

    // Track foreground state and loaded ad
    private var isActivityForeground: Boolean = false
    private var loadedInterAd: ApInterstitialAd? = null
    private var loadedAdDelay: Long = 0L
    private var loadedAdOnClosed: (() -> Unit)? = null
    private var isAdShowing: Boolean = false

    init {
        loadHelperScope.launch {
            timeoutState.collectLatest {
                interLoadJob?.cancel()
                interLoadJob = null

                withContext(Dispatchers.Main) {
                    onTimeoutCallback?.onTimeout()
                }
            }
        }
    }

    fun onLoadInterstitialAdSplash(
        activity: Activity,
        timeout: Long = 20000L,
        delay: Long = 0L
    ) {
        // Ads disabled — proceed immediately.
        onApInterstitialAdCallback?.onClosed()
    }

    private fun onRunTimeoutInterstitialAds(timeout: Long) {
        removeCallbackTimeoutHandler()
        handlerTimeout = Handler(Looper.getMainLooper())
        runnableTimeout = Runnable {
            loadHelperScope.launch {
                _timeoutState.send(Unit)
            }
        }
        runnableTimeout?.let {
            handlerTimeout?.postDelayed(it, timeout)
        }
    }

    private fun removeCallbackTimeoutHandler() {
        if (handlerTimeout != null && runnableTimeout != null) {
            handlerTimeout?.removeCallbacks(runnableTimeout!!)
            runnableTimeout = null
            handlerTimeout = null
        }
    }

    private fun onLoadInterstitial(activity: Activity, timeout: Long, delay: Long) {
        interLoadJob?.cancel()
        interLoadJob = loadHelperScope.launch {
            Log.d(TAG, "Starting loading inter splash")
            onRunTimeoutInterstitialAds(timeout)
            try {
                var interAdReload = false
                if (!configManagement.enableInterSplashHighMode) {
                    interAdReload = true
                }
                if (!interAdReload) {
                    Log.d(TAG, "Loading inter splash 2ID: ${BuildConfig.planty_inter_splash_2ID}")
                    try {
                        onLoadInterstitialAd(activity, BuildConfig.planty_inter_splash_2ID)?.let {
                            Log.d(TAG, "Load inter splash 2ID success -> remove timeout")
                            removeCallbackTimeoutHandler()
                            // Store loaded ad info
                            loadedInterAd = it
                            loadedAdDelay = delay
                            loadedAdOnClosed = {
                                Log.d(TAG, "High priority ad closed")
                                onApInterstitialAdCallback?.onClosed()
                            }
                            Log.d(TAG, "Inter splash 2ID is start showing, delay=${delay}")
                            onShowInterstitialDelay(activity, it, delay) {
                                Log.d(TAG, "Inter splash 2ID closed")
                                onApInterstitialAdCallback?.onClosed()
                            }
                            return@launch
                        }
                        Log.d(TAG, "Load inter splash 2ID failed")
                    } catch (e: Exception) {
                        Log.d(TAG, "Error: loading inter splash 2ID: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Log.d(TAG, "Loading inter splash normal: ${BuildConfig.planty_inter_splash}")
                onLoadInterstitialAd(activity, BuildConfig.planty_inter_splash)?.let {
                    Log.d(TAG, "Load inter splash normal success -> remove timeout")
                    removeCallbackTimeoutHandler()
                    // Store loaded ad info
                    loadedInterAd = it
                    loadedAdDelay = delay
                    loadedAdOnClosed = {
                        Log.d(TAG, "High priority ad closed")
                        onApInterstitialAdCallback?.onClosed()
                    }
                    Log.d(TAG, "Inter splash normal is start showing, delay=${delay}")
                    onShowInterstitialDelay(activity, it, delay) {
                        Log.d(TAG, "Inter splash normal closed")
                        onApInterstitialAdCallback?.onClosed()
                    }
                    return@launch
                }
                Log.d(TAG, "Load inter splash 2ID failed")
                withContext(Dispatchers.Main.immediate) {
                    handleLoadFailed(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "onLoadInterstitial: load failed! -", e)
                removeCallbackTimeoutHandler()
                withContext(Dispatchers.Main.immediate) {
                    handleLoadFailed(null)
                }
            }
        }
    }

    private fun onShowInterstitialDelay(
        activity: Activity,
        interAd: ApInterstitialAd,
        delay: Long,
        onClosed: () -> Unit
    ) {
        Log.d(
            TAG,
            "onShowInterstitialDelay: scheduling ad show with delay=$delay ms, isActivityForeground=$isActivityForeground, isAdShowing=$isAdShowing"
        )

        // Check if ad is already showing
        if (isAdShowing) {
            Log.w(TAG, "Ad is already showing - storing ad for later")
            loadedInterAd = interAd
            loadedAdDelay = delay
            loadedAdOnClosed = onClosed
            return
        }

        // Check if activity is in foreground before showing
        if (!isActivityForeground) {
            Log.w(
                TAG,
                "Activity is in background - ad will be shown when activity moves to foreground"
            )
            // Ensure ad is stored for later showing
            loadedInterAd = interAd
            loadedAdDelay = delay
            loadedAdOnClosed = onClosed
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if ad is already showing (might have been shown by another handler)
            if (isAdShowing) {
                Log.w(TAG, "Ad is already showing - storing ad for later")
                loadedInterAd = interAd
                loadedAdDelay = delay
                loadedAdOnClosed = onClosed
                return@postDelayed
            }

            // Double check foreground state before showing
            if (!isActivityForeground) {
                Log.w(
                    TAG,
                    "Activity moved to background during delay - ad will be shown when activity moves to foreground"
                )
                // Ensure ad is stored for later showing
                loadedInterAd = interAd
                loadedAdDelay = delay
                loadedAdOnClosed = onClosed
                return@postDelayed
            }

            Log.d(TAG, "Showing interstitial ad now")
            isAdShowing = true
            PreloadInterstitialAdManagement.showInterAd(activity, interAd) {
                Log.d(TAG, "Interstitial ad show completed - calling onClosed")
                // Clear loaded ad state
                isAdShowing = false
                loadedInterAd = null
                loadedAdDelay = 0L
                loadedAdOnClosed = null
                onClosed.invoke()
            }
        }, delay)
    }

    /**
     * Call this when activity moves to foreground
     * Will show ads if they were loaded successfully but couldn't be shown before
     */
    fun onActivityResumed(activity: Activity, timeout: Long) {
        Log.d(TAG, "onActivityResumed: isActivityForeground=true, isAdShowing=$isAdShowing")
        isActivityForeground = true

        // Don't try to show ad if one is already showing
        if (isAdShowing) {
            Log.d(TAG, "Ad is already showing - skipping")
            return
        }

        // Check if we have a loaded ad that needs to be shown
        val adToShow = loadedInterAd
        if (adToShow != null) {
            Log.d(TAG, "Found loaded ad - attempting to show now that activity is in foreground")
            val delay = loadedAdDelay.takeIf { it != 0L } ?: timeout
            val onClosed = loadedAdOnClosed ?: {
                Log.d(TAG, "Interstitial ad closed")
                onApInterstitialAdCallback?.onClosed()
            }

            // Clear stored ad info before showing (will be restored if can't show)
            loadedInterAd = null
            loadedAdDelay = 0L
            loadedAdOnClosed = null

            onShowInterstitialDelay(activity, adToShow, delay) {
                onClosed.invoke()
            }
        }
    }

    /**
     * Call this when activity moves to background
     */
    fun onActivityPaused() {
        Log.d(TAG, "onActivityPaused: isActivityForeground=false")
        isActivityForeground = false
    }

    /**
     * Check if ad was loaded but not shown (e.g., because activity was in background)
     */
    fun hasLoadedAdPending(): Boolean {
        return loadedInterAd != null
    }

    /**
     * Handle case when load failed - call this to trigger callback
     */
    fun handleLoadFailed(error: LoadAdError?) {
        Log.d(TAG, "handleLoadFailed: clearing loaded ad state and calling callback")
        isAdShowing = false
        loadedInterAd = null
        loadedAdDelay = 0L
        loadedAdOnClosed = null
        onApInterstitialAdCallback?.onFailed(error)
    }


    fun setOnTimeoutCallback(callback: OnTimeoutCallback?) {
        this.onTimeoutCallback = callback
    }

    fun setOnLoadInterstitialCallback(callback: OnLoadInterstitialCallback?) {
        this.onApInterstitialAdCallback = callback
    }

    fun onDestroy() {
        Log.d(TAG, "destroy: cleaning up resources")
        removeCallbackTimeoutHandler()

        onTimeoutCallback = null
        onApInterstitialAdCallback = null

        interLoadJob?.cancel()
        interLoadJob = null


        // Clear loaded ad state
        isAdShowing = false
        loadedInterAd = null
        loadedAdDelay = 0L
        loadedAdOnClosed = null
        isActivityForeground = false

        Log.d(TAG, "destroy: cleanup completed")
    }

    interface OnTimeoutCallback {
        fun onTimeout()
    }

    interface OnLoadInterstitialCallback {
        fun onClosed()
        fun onFailed(error: LoadAdError?)
    }
}
