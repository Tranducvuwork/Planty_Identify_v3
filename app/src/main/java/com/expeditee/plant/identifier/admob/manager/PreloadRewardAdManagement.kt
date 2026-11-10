package com.expeditee.plant.identifier.admob.manager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dqh.lmtech.ads.ads.DqhAd
import com.dqh.lmtech.ads.funtion.AdCallback
import com.dqh.lmtech.ads.funtion.RewardCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.admob.loading.AdLoadingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class PreloadRewardAdManagement(application: Application) {

    companion object {
        private const val LOAD_TIMEOUT = 5000L
    }

    private val context: Context = application.applicationContext
    private val managementScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var rewardAd: RewardedAd? = null
    private var rewardedAdJob: Job? = null
    private var rewardedAdLoading = AtomicBoolean(false)

    fun initRewardAd(onLoad: ((RewardedAd?) -> Unit)? = null) {
        if (rewardAd != null || !rewardedAdLoading.compareAndSet(false, true)) {
            return
        }
        rewardedAdJob?.cancel()
        rewardedAdJob = managementScope.launch {
            try {
                onLoadRewardAd(context, BuildConfig.planty_scan_reward)?.also {
                    rewardAd = it
                    onLoad?.invoke(it)
                    return@launch
                }
                rewardAd = null
                onLoad?.invoke(null)
            } catch (e: Exception) {
                Log.e("PlantyReward", "Exception loading ad", e)
                e.printStackTrace()
            } finally {
                rewardedAdLoading.set(false)
            }
        }
    }

    fun showReward(
        activity: Activity,
        waitLoad: Boolean = true,
        onRewardAdClosed: () -> Unit,
        onRewardFailed: () -> Unit
    ) {
        fun notifyFailed() {
            AdLoadingManager.dismiss(activity)
            onRewardFailed.invoke()
            rewardedAdLoading.set(false)
            rewardAd = null
        }

        try {
            AdLoadingManager.show(activity)
            if (rewardedAdLoading.get() && waitLoad) {
                var timeRemaining = 0L
                val handler = Handler(Looper.getMainLooper())
                val waitRunnable = object : Runnable {
                    override fun run() {
                        timeRemaining += 200L
                        when {
                            rewardAd != null -> {
                                handler.removeCallbacks(this)
                                showReward(activity, false, onRewardAdClosed, onRewardFailed)
                            }

                            timeRemaining >= LOAD_TIMEOUT -> {
                                notifyFailed()
                                handler.removeCallbacks(this)
                            }

                            else -> {
                                handler.postDelayed(this, 200L)
                            }
                        }
                    }
                }

                handler.postDelayed(waitRunnable, 200L)
                return
            }
            val rewardedAd = rewardAd
            if (rewardedAd == null) {
                initRewardAd()
                notifyFailed()
                return
            }
            AdLoadingManager.dismiss(activity)
            DqhAd.getInstance().showRewardAds(
                activity, rewardedAd, object : RewardCallback {
                    override fun onUserEarnedReward(var1: RewardItem?) {
                        Log.d("PlantyReward", "onUserEarnedReward: $var1")
                    }

                    override fun onRewardedAdClosed() {
                        rewardAd = null
                        onRewardAdClosed()
                        initRewardAd()
                    }

                    override fun onRewardedAdFailedToShow(codeError: Int) {
                        Log.e("PlantyReward", "Ad show failed: $codeError")
                        notifyFailed()
                        initRewardAd()
                    }

                    override fun onAdClicked() = Unit
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlantyReward", "Show exception", e)
            notifyFailed()
        }
    }

    private suspend fun onLoadRewardAd(
        context: Context,
        unitAdId: String
    ): RewardedAd? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            DqhAd.getInstance().initRewardAds(context, unitAdId, object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    if (isActive) continuation.resume(rewardedAd)
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e("PlantyReward", "Ad load failed: ${i?.message}")
                    if (isActive) continuation.resume(null)
                }
            })
        }
    }
}
