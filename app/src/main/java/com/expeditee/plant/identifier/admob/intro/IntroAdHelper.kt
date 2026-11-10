package com.expeditee.plant.identifier.admob.intro

import android.content.Context
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.admob.LammaAd
import com.expeditee.plant.identifier.admob.manager.NativeCallback
import com.expeditee.plant.identifier.admob.manager.PreloadNativeAdManagement
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class IntroAdHelper(
    private val configManagement: RemoteConfigManagement,
    private val nativeAdManagement: PreloadNativeAdManagement
) {

    companion object {

        const val NATIVE_LANGUAGE = "nat_language"
        const val NATIVE_LANGUAGE_CLICK = "nat_language_click"
        const val NATIVE_LANGUAGE_CLICK2 = "nat_language_click2"
        const val NATIVE_LANGUAGE_CLICK_PENDING_OB1 = "nat_language_click_onboarding_1"

        const val NATIVE_ONBOARDING = "nat_onboarding_1"
        const val NATIVE_ONBOARDING_2 = "nat_onboarding_2"
        const val NATIVE_ONBOARDING_FULL = "nat_onboarding_full"
        const val NATIVE_ONBOARDING_LAST = "nat_onboarding_last"
    }

    fun getNativeAd(placement: String): LammaAd<ApNativeAd>? {
        return nativeAdManagement.getNativeAd(placement)
    }

    fun addNativeAdCallback(placement: String, adCallback: NativeCallback) {
        nativeAdManagement.addAdCallback(placement, adCallback)
    }

    fun removeNativeAdCallback(adCallback: NativeCallback) {
        nativeAdManagement.removeAdCallback(adCallback)
    }

    fun removeNative(placement: String) {
        nativeAdManagement.removeCache(placement)
    }

    suspend fun onLoadAllNativeLanguage(context: Context) = coroutineScope {
        if (!configManagement.enableNativeLanguage) return@coroutineScope
        awaitAll(
            async { onLoadNativeLanguage(context) },
            async { onLoadNativeLanguageClick(context) },
        )
    }

    private fun onLoadNativeLanguage(context: Context) {
        nativeAdManagement.preloadNativeAd(
            context = context,
            placement = NATIVE_LANGUAGE,
            shouldLoad = true,
            admobIds = listOf(
                BuildConfig.planty_native_language_2ID,
                BuildConfig.planty_native_language,
            ),
        )
    }

    private fun onLoadNativeLanguageClick(context: Context) {
        nativeAdManagement.preloadNativeAd(
            context = context,
            placement = NATIVE_LANGUAGE_CLICK,
            shouldLoad = true,
            admobIds = listOf(
                BuildConfig.planty_native_language_click_2ID,
                BuildConfig.planty_native_language_click,
            ),
            callback = object : NativeCallback {
                override fun onLoaded(data: LammaAd<ApNativeAd>?) {}
                override fun onFailed() = Unit
            }
        )
    }


    fun loadNativeLanguageClickForSelection(
        context: Context,
        callback: NativeCallback? = null
    ) {
        if (!context.isNetwork()) {
            callback?.onFailed()
            return
        }
        nativeAdManagement.preloadNativeAd(
            context = context.applicationContext,
            placement = NATIVE_LANGUAGE_CLICK_PENDING_OB1,
            shouldLoad = true,
            admobIds = listOf(
                BuildConfig.planty_native_language_click_2ID,
                BuildConfig.planty_native_language_click,
            ),
            callback = callback,
        )
    }

    fun consumePendingLanguageClick(): LammaAd<ApNativeAd>? {
        return nativeAdManagement.consumeNative(NATIVE_LANGUAGE_CLICK_PENDING_OB1)
            ?.takeIf { it.isAvailable() }
    }

    fun removePendingLanguageClickCallback(callback: NativeCallback) {
        nativeAdManagement.removeAdCallback(callback)
    }

    fun onLoadNativeOnboarding1(context: Context, callback: NativeCallback? = null) {
        if (!context.isNetwork()) return
        if (!configManagement.enableNativeOnboarding1) {
            callback?.onFailed()
            return
        }
        consumeNativeAd(
            context = context,
            placement = NATIVE_ONBOARDING,
            admobIds = listOf(
                BuildConfig.planty_native_onboarding_1_2ID,
                BuildConfig.planty_native_onboarding_1,
            ),
            callback = callback
        )
    }

    fun onLoadNativeOnboarding2(context: Context, callback: NativeCallback? = null) {
        if (!context.isNetwork()) return
        if (!configManagement.enableNativeOnboarding2) {
            callback?.onFailed()
            return
        }
        consumeNativeAd(
            context = context,
            placement = NATIVE_ONBOARDING_2,
            admobIds = listOf(
                BuildConfig.planty_native_onboarding_1_2ID,
                BuildConfig.planty_native_onboarding_1,
            ),
            callback = callback
        )
    }

    fun onLoadNativeOnboardingFull(context: Context, callback: NativeCallback? = null) {
        if (!context.isNetwork()) return
        if (!configManagement.enableNativeOnboardingFull) {
            callback?.onFailed()
            return
        }
        consumeNativeAd(
            context = context,
            placement = NATIVE_ONBOARDING_FULL,
            admobIds = listOf(
                BuildConfig.planty_native_onboarding_full_2ID,
                BuildConfig.planty_native_onboarding_full,
            ),
            callback = callback
        )
    }

    fun onLoadNativeOnboardingLast(context: Context, callback: NativeCallback? = null) {
        if (!context.isNetwork()) return
        if (!configManagement.enableNativeOnboardingLast) {
            callback?.onFailed()
            return
        }
        consumeNativeAd(
            context = context,
            placement = NATIVE_ONBOARDING_LAST,
            admobIds = listOf(
                BuildConfig.planty_native_onboarding_last_2ID,
                BuildConfig.planty_native_onboarding_last,
            ),
            callback = callback
        )
    }

    private fun consumeNativeAd(
        context: Context,
        placement: String,
        admobIds: List<String>,
        callback: NativeCallback? = null
    ) {
        val cachedAd = nativeAdManagement.consumeNative(placement)
        if (cachedAd?.isAvailable() == true) {
            callback?.onLoaded(cachedAd)
            return
        }

        nativeAdManagement.preloadNativeAd(
            context = context,
            placement = placement,
            shouldLoad = true,
            admobIds = admobIds,
            callback = callback
        )
    }
}
