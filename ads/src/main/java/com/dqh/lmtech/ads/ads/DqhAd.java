package com.dqh.lmtech.ads.ads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.dqh.lmtech.ads.admob.Admob;
import com.dqh.lmtech.ads.admob.AppOpenManager;
import com.dqh.lmtech.ads.ads.wrapper.ApInterstitialAd;
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd;
import com.dqh.lmtech.ads.config.DqhTechAdConfig;
import com.dqh.lmtech.ads.event.DqhAdjust;
import com.dqh.lmtech.ads.funtion.AdCallback;
import com.dqh.lmtech.ads.funtion.InterstitialCallback;
import com.dqh.lmtech.ads.funtion.RewardCallback;
import com.dqh.lmtech.ads.util.AppUtil;
import com.dqh.lmtech.ads.util.SharePreferenceUtils;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.base.EventName;

public class DqhAd {
    public static final String TAG_ADJUST = "BLabAdjust";
    public static final String TAG = "BLabAd";
    private static volatile DqhAd INSTANCE;
    private DqhTechAdConfig adConfig;
    private DqhInitCallback initCallback;
    private Boolean initAdSuccess = false;

    public static synchronized DqhAd getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DqhAd();
        }
        return INSTANCE;
    }

    public DqhTechAdConfig getAdConfig() {
        return adConfig;
    }

    public void setCountClickToShowAds(int countClickToShowAds) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds);
    }

    public void setCountClickToShowAds(int countClickToShowAds, int currentClicked) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds, currentClicked);
    }

    public void init(Application context, DqhTechAdConfig adConfig) {
        if (adConfig == null) {
            throw new RuntimeException("Cant not set GamAdConfig null");
        }
        this.adConfig = adConfig;
        AppUtil.VARIANT_DEV = adConfig.isVariantDev();
        if (adConfig.isEnableAdjust()) {
            DqhAdjust.enableAdjust = true;
            setupAdjust(adConfig.isVariantDev(), adConfig.getAdjustConfig().getAdjustToken());
        }

        Admob.getInstance().init(context, adConfig.getListDeviceTest(), adConfig.getAdjustTokenTiktok());
        if (adConfig.isEnableAdResume()) {
            AppOpenManager.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResume());
        }
        FacebookSdk.setClientToken(adConfig.getFacebookClientToken());
        // Lam Add Debug
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        FacebookSdk.sdkInitialize(context);

        //20250401 Lam add TikTok
        TikTokBusinessSdk.initializeSdk(
                new TikTokBusinessSdk.TTConfig(
                        context,
                        adConfig.getTikTokClientToken()
                )
                        .setAppId(adConfig.getTikTokId())
                        .setTTAppId(adConfig.getTikTokAppId())
                        .openDebugMode()
                        .setLogLevel(TikTokBusinessSdk.LogLevel.DEBUG)
        );

        //Report standard events
        TikTokBusinessSdk.trackTTEvent(EventName.ACHIEVE_LEVEL);
    }

    public void setInitCallback(DqhInitCallback initCallback) {
        this.initCallback = initCallback;
        if (initAdSuccess)
            initCallback.initAdSuccess();
    }

    private void setupAdjust(Boolean buildDebug, String adjustToken) {
        String environment = buildDebug ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        AdjustConfig config = new AdjustConfig(adConfig.getApplication(), adjustToken, environment);

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE);

        // 20250124 Lam - Update Adjust V5 Begin
        config.enablePreinstallTracking();
        config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
            @Override
            public void onAttributionChanged(AdjustAttribution attribution) {
                Log.d(TAG_ADJUST, "Attribution callback called!");
                Log.d(TAG_ADJUST, "Attribution: " + attribution.toString());
            }
        });

        // Set event success tracking delegate.
        config.setOnEventTrackingSucceededListener(eventSuccessResponseData -> {
            Log.d(TAG_ADJUST, "Event success callback called!");
            Log.d(TAG_ADJUST, "Event success data: " + eventSuccessResponseData.toString());
        });

        // Set event failure tracking delegate.
        config.setOnEventTrackingFailedListener(eventFailureResponseData -> {
            Log.d(TAG_ADJUST, "Event failure callback called!");
            Log.d(TAG_ADJUST, "Event failure data: " + eventFailureResponseData.toString());
        });


        // Set session success tracking delegate.
        config.setOnSessionTrackingSucceededListener(sessionSuccessResponseData -> {
            Log.d(TAG_ADJUST, "Session success callback called!");
            Log.d(TAG_ADJUST, "Session success data: " + sessionSuccessResponseData.toString());
        });


        // Set session failure tracking delegate.
        config.setOnSessionTrackingFailedListener(sessionFailureResponseData -> {
            Log.d(TAG_ADJUST, "Session failure callback called!");
            Log.d(TAG_ADJUST, "Session failure data: " + sessionFailureResponseData.toString());
        });

        config.enableSendingInBackground();
        Adjust.initSdk(config);
        // 20250124 Lam - Update Adjust V5 End

        adConfig.getApplication().registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());
    }

    private static final class AdjustLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }
    }

    public void loadBanner(Activity mActivity, String id) {
        Admob.getInstance().loadBanner(mActivity, id);
    }

    public void loadBanner(Activity mActivity, String id, AdCallback adCallback) {
        Admob.getInstance().loadBanner(mActivity, id, adCallback);
    }

    public void loadCollapsibleBanner(Activity activity, String id, String gravity, AdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBanner(activity, id, gravity, adCallback);
    }

    public void loadCollapsibleBannerFragment(Activity activity, String id, View rootView, String gravity, AdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBannerFragment(activity, id, rootView, gravity, adCallback);
    }

    public void loadCollapsibleBannerSizeMedium(Activity activity, String id, String gravity, AdSize sizeBanner, AdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBannerSizeMedium(activity, id, gravity, sizeBanner, adCallback);
    }

    public void loadBannerFragment(Activity mActivity, String id, View rootView) {
        Admob.getInstance().loadBannerFragment(mActivity, id, rootView);
    }

    public void loadBannerFragment(Activity mActivity, String id, View rootView, AdCallback adCallback) {
        Admob.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
    }

    public void loadInlineBanner(Activity mActivity, String idBanner, String inlineStyle) {
        Admob.getInstance().loadInlineBanner(mActivity, idBanner, inlineStyle);
    }

    public void loadInlineBanner(Activity mActivity, String idBanner, String inlineStyle, AdCallback adCallback) {
        Admob.getInstance().loadInlineBanner(mActivity, idBanner, inlineStyle, adCallback);
    }

    public void loadBannerInlineFragment(Activity mActivity, String idBanner, View rootView, String inlineStyle) {
        Admob.getInstance().loadInlineBannerFragment(mActivity, idBanner, rootView, inlineStyle);
    }

    public void loadBannerInlineFragment(Activity mActivity, String idBanner, View rootView, String inlineStyle, AdCallback adCallback) {
        Admob.getInstance().loadInlineBannerFragment(mActivity, idBanner, rootView, inlineStyle, adCallback);
    }

    public void loadSplashInterstitialAds(Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        Admob.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener);
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        Admob.getInstance().onCheckShowSplashWhenFail(activity, callback, timeDelay);
    }

    public ApInterstitialAd getInterstitialAds(Context context, String id, AdCallback adListener) {
        ApInterstitialAd apInterstitialAd = new ApInterstitialAd();
        Admob.getInstance().getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                apInterstitialAd.setInterstitialAd(interstitialAd);
                adListener.onApInterstitialLoad(apInterstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.d(TAG, "Admob onAdFailedToLoad");
                adListener.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                Log.d(TAG, "Admob onAdFailedToShow");
                adListener.onAdFailedToShow(adError);
            }

        });
        return apInterstitialAd;
    }

    public void forceShowInterstitial(@NonNull Context context, ApInterstitialAd mInterstitialAd,
                                      @NonNull final AdCallback callback, boolean shouldReloadAds) {
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(context)
                < DqhAd.getInstance().adConfig.getIntervalInterstitialAd() * 1000L
        ) {
            callback.onNextAction();
            return;
        }
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            callback.onNextAction();
            return;
        }
        AdCallback adCallback = new AdCallback() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                callback.onAdClosed();
                if (shouldReloadAds) {
                    Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                        @Override
                        public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                            super.onInterstitialLoad(interstitialAd);
                            mInterstitialAd.setInterstitialAd(interstitialAd);
                            callback.onInterstitialLoad(mInterstitialAd.getInterstitialAd());
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            mInterstitialAd.setInterstitialAd(null);
                            callback.onAdFailedToLoad(i);
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            callback.onAdFailedToShow(adError);
                        }

                    });
                } else {
                    mInterstitialAd.setInterstitialAd(null);
                }
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                callback.onNextAction();
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
                if (shouldReloadAds) {
                    Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdCallback() {
                        @Override
                        public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                            super.onInterstitialLoad(interstitialAd);
                            mInterstitialAd.setInterstitialAd(interstitialAd);
                            callback.onInterstitialLoad(mInterstitialAd.getInterstitialAd());
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            callback.onAdFailedToLoad(i);
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            callback.onAdFailedToShow(adError);
                        }

                    });
                } else {
                    mInterstitialAd.setInterstitialAd(null);
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }

            @Override
            public void onInterstitialShow() {
                super.onInterstitialShow();
                callback.onInterstitialShow();
            }
        };
        Admob.getInstance().forceShowInterstitial(context, mInterstitialAd.getInterstitialAd(), adCallback);
    }

    public void forceShowInterstitial(
            @NonNull Context context,
            ApInterstitialAd mInterstitialAd,
            @NonNull final AdCallback callback
    ) {
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            callback.onAdClosed();
            return;
        }
        AdCallback adCallback = new AdCallback() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.getWindow() != null) {
                        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
                    }
                }
                callback.onAdClosed();
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                callback.onNextAction();
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }

            @Override
            public void onInterstitialShow() {
                super.onInterstitialShow();
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.getWindow() != null) {
                        activity.getWindow().getDecorView().setSystemUiVisibility(0);
                        activity.getWindow().setStatusBarColor(Color.BLACK);
                    }
                }
                callback.onInterstitialShow();
            }
        };
        Admob.getInstance().forceShowInterstitial(context, mInterstitialAd.getInterstitialAd(), adCallback);
    }

    public void forceShowInterstitial2(
            @NonNull Context context,
            ApInterstitialAd mInterstitialAd,
            @NonNull final InterstitialCallback callback
    ) {
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            callback.onAdClosed();
            return;
        }
        Admob.getInstance().forceShowInterstitial2(context, mInterstitialAd.getInterstitialAd(), new InterstitialCallback() {
            @Override
            public void onAdClosed() {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.getWindow() != null) {
                        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
                        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
                    }
                }
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToShow(int errorCode) {
                super.onAdFailedToShow(errorCode);
                callback.onAdFailedToShow(errorCode);
            }

            @Override
            public void onAdShowLoading() {
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    if (activity.getWindow() != null) {
                        activity.getWindow().setStatusBarColor(Color.BLACK);
                        activity.getWindow().setNavigationBarColor(Color.BLACK);
                    }
                }
                super.onAdShowLoading();
            }

            @Override
            public void onAdDismissLoading() {
                super.onAdDismissLoading();
                callback.onAdDismissLoading();
            }

            @Override
            public void onAdCanPreload() {
                super.onAdCanPreload();
                callback.onAdCanPreload();
            }
        });
    }

    public void loadNativeAdResultCallback(final Activity activity, String id,
                                           int layoutCustomNative, AdCallback callback) {
        Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void loadNativeAdResultCallback(final Context context, String id,
                                           int layoutCustomNative, AdCallback callback) {
        Admob.getInstance().loadNativeAd(context, id, new AdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void loadNativeAdLanguage3ResultCallback(final Activity activity, String id,
                                                    int layoutCustomNative, AdCallback callback) {
        Admob.getInstance().loadNativeAdLanguage3(((Context) activity), id, new AdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }


    public void populateNativeCollapseAdView(Activity activity, ApNativeAd apNativeAd, FrameLayout adPlaceHolder,@Nullable ShimmerFrameLayout containerShimmerLoading, Runnable closeNative) {
        if (apNativeAd.getAdmobNativeAd() == null && apNativeAd.getNativeView() == null) {
            if (containerShimmerLoading != null){
                containerShimmerLoading.setVisibility(View.GONE);
            }
            return;
        }
        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(apNativeAd.getLayoutCustomNative(), null);
        if (containerShimmerLoading != null){
            containerShimmerLoading.stopShimmer();
            containerShimmerLoading.setVisibility(View.GONE);
        }
        adPlaceHolder.setVisibility(View.VISIBLE);
        Admob.getInstance().populateUnifiedNativeAdCollapseView(apNativeAd.getAdmobNativeAd(), adView, closeNative);
        adPlaceHolder.removeAllViews();
        adPlaceHolder.addView(adView);
    }

    public void loadNativeAd(final Activity activity, String id,
                             int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                     containerShimmerLoading, AdCallback callback) {
        Admob.getInstance().loadNativeAd(((Context) activity), id, new AdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                ApNativeAd apNativeAd = new ApNativeAd(layoutCustomNative, unifiedNativeAd);
                populateNativeAdView(activity, apNativeAd, adPlaceHolder, containerShimmerLoading);
                callback.onNativeAdLoaded(apNativeAd);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                callback.onAdImpression();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void populateNativeAdView(Activity activity, ApNativeAd apNativeAd, FrameLayout adPlaceHolder, ShimmerFrameLayout containerShimmerLoading) {
        if (apNativeAd.getAdmobNativeAd() == null && apNativeAd.getNativeView() == null) {
            containerShimmerLoading.setVisibility(View.GONE);
            return;
        }
        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(apNativeAd.getLayoutCustomNative(), null);
        containerShimmerLoading.stopShimmer();
        containerShimmerLoading.setVisibility(View.GONE);
        adPlaceHolder.setVisibility(View.VISIBLE);
        Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.getAdmobNativeAd(), adView);
        adPlaceHolder.removeAllViews();
        adPlaceHolder.addView(adView);
    }

    public void initRewardAds(Context context, String id) {
        Admob.getInstance().initRewardAds(context, id);
    }

    public void initRewardAds(Context context, String id, AdCallback callback) {
        Admob.getInstance().initRewardAds(context, id, callback);
    }

    public void getRewardInterstitial(Context context, String id, AdCallback callback) {
        Admob.getInstance().getRewardInterstitial(context, id, callback);
    }

    public void showRewardInterstitial(Activity activity, RewardedInterstitialAd rewardedInterstitialAd, RewardCallback adCallback) {
        Admob.getInstance().showRewardInterstitial(activity, rewardedInterstitialAd, adCallback);
    }

    public void showRewardAds(Activity context, RewardCallback adCallback) {
        Admob.getInstance().showRewardAds(context, adCallback);
    }

    public void showRewardAds(Activity context, RewardedAd rewardedAd, RewardCallback adCallback) {
        Admob.getInstance().showRewardAds(context, rewardedAd, adCallback);
    }

}
