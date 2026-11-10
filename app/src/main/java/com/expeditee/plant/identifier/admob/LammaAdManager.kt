package com.expeditee.plant.identifier.admob

import android.app.Activity
import android.content.Context
import android.view.View
import com.dqh.lmtech.ads.R
import com.dqh.lmtech.ads.admob.AppOpenManager
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.dqh.lmtech.ads.funtion.AdCallback
import com.expeditee.plant.identifier.admob.manager.NativeCallback
import com.expeditee.plant.identifier.admob.manager.PreloadInterstitialAdManagement
import com.expeditee.plant.identifier.admob.manager.PreloadNativeAdManagement
import com.expeditee.plant.identifier.admob.manager.PreloadRewardAdManagement
import com.expeditee.plant.identifier.common.AppMonetization
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.Runnable

/**
 * Ads disabled for thesis build — every method is a no-op / immediate success callback.
 */
class LammaAdManager(
    private val applicationContext: Context,
    private val configManagement: RemoteConfigManagement,
    private val interAdManagement: PreloadInterstitialAdManagement,
    private val nativeManagement: PreloadNativeAdManagement,
    private val rewardAdManagement: PreloadRewardAdManagement,
) {

    companion object {
        const val INTER_SPLASH = "inter_splash"
        const val INTER_SHARED = "inter_shared"
        const val NATIVE_HOME = "native_home"

        private var lastTimeShowAds: Long = 0L
        fun onLogTimeShowAds() {
            lastTimeShowAds = System.currentTimeMillis()
        }

        fun isEligibleShowInter(distanceTimeShowAds: Long): Boolean = false
    }

    fun applyOpenResumeAdConfig() {
        AppOpenManager.getInstance().disableAppResume()
    }

    fun loadInterShared(context: Context) = Unit

    fun showInterShared(activity: Activity, runnable: Runnable) {
        runnable.run()
    }

    fun onLoadNativeAd(
        nativeAd: NativeAdView,
        unitAdId: String,
        configEnable: Boolean,
        callback: NativeCallback? = null,
    ) {
        hideNative(nativeAd)
        runCatching { callback?.onFailed() }
    }

    fun onLoadNativeAd2ID(
        nativeAd: NativeAdView,
        unitAd2Id: String,
        unitAdId: String,
        configEnable: Boolean,
        callback: NativeCallback? = null,
    ) {
        hideNative(nativeAd)
        runCatching { callback?.onFailed() }
    }

    fun loadRewardAd(context: Context) = Unit

    fun showReward(
        activity: Activity,
        onClosed: () -> Unit,
        onFailed: () -> Unit,
    ) {
        // Treat reward as granted immediately (no ad).
        onClosed()
    }

    fun onLoadCollapseBanner(
        activity: Activity,
        unitAdId: String = "",
        configEnable: Boolean = true,
        adCallback: AdCallback? = null,
    ) {
        hideBannerContainer(activity)
    }

    fun onLoadCollapseBannerFragment(
        activity: Activity,
        unitAdId: String,
        rootView: View,
        configEnable: Boolean,
        adCallback: AdCallback? = null,
    ) {
        hideBannerContainer(activity)
        runCatching {
            rootView.findViewById<View>(R.id.ll_ads)?.beGone()
        }
    }

    fun onLoadBanner(
        activity: Activity,
        unitAdId: String,
        configEnable: Boolean,
        adCallback: AdCallback? = null,
    ) {
        hideBannerContainer(activity)
    }

    fun consumeNativeAd(
        context: Context,
        nativeAdView: NativeAdView,
        unitAdId: String = "",
        configEnable: Boolean = true,
    ) {
        hideNative(nativeAdView)
    }

    fun onLoadNativeAdSplash(
        nativeAdView: NativeAdView,
        configEnable: Boolean = true,
        populatedCallback: (() -> Unit)? = null,
    ) {
        hideNative(nativeAdView)
        populatedCallback?.invoke()
    }

    private fun hideNative(nativeAd: NativeAdView) {
        nativeAd.removeAllViews()
        nativeAd.beGone()
    }

    private fun hideBannerContainer(activity: Activity) {
        runCatching {
            activity.findViewById<View>(R.id.ll_ads)?.beGone()
        }
    }

    @Suppress("unused")
    private fun adsEnabled(): Boolean = AppMonetization.ADS_ENABLED
}
