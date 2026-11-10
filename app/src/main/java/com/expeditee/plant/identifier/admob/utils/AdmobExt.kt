package com.expeditee.plant.identifier.admob.utils

import android.content.Context
import com.dqh.lmtech.ads.R
import com.dqh.lmtech.ads.ads.wrapper.ApInterstitialAd
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.dqh.lmtech.ads.funtion.AdCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ads disabled — never load from network. */
internal suspend fun onLoadNativeAd(
    context: Context,
    unitIdAds: String,
    layoutRes: Int = R.layout.ads_native_large,
    callback: AdCallback? = null,
): ApNativeAd? = withContext(Dispatchers.Main) { null }

internal suspend fun onLoadInterstitialAd(
    context: Context,
    unitIdAds: String,
): ApInterstitialAd? = withContext(Dispatchers.Main) { null }
