//package com.dqh.lmtech.ads.event;
//
//import android.content.Context;
//import android.os.Bundle;
//
//import com.dqh.lmtech.ads.config.DqhTechAdConfig;
//import com.facebook.appevents.AppEventsConstants;
//import com.facebook.appevents.AppEventsLogger;
//
//public class FacebookEventUtils {
//    public static void logEventWithAds(Context context, Bundle params) {
//        AppEventsLogger.newLogger(context).logEvent("paid_ad_impression", params);
//    }
//
//    public static void logEventWithAdImpression(Context context, Double revenue) {
//        if (revenue <= 0) {
//            return;
//        }
//
//        Bundle params = new Bundle();
//        params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD"); // Example: "USD"
//        params.putDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, revenue); // Example: 9.99
//
//        AppEventsLogger.newLogger(context).logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, revenue, params);
//        AppEventsLogger.newLogger(context).logEvent(AppEventsConstants.EVENT_NAME_PURCHASED, revenue, params);
//    }
//
//
//    static void logPaidAdImpressionValue(Context context, Bundle bundle, int mediationProvider) {
//        if (mediationProvider == DqhTechAdConfig.PROVIDER_MAX)
//            AppEventsLogger.newLogger(context).logEvent("max_paid_ad_impression_value", bundle);
//        else
//            AppEventsLogger.newLogger(context).logEvent("paid_ad_impression_value", bundle);
//    }
//
//    public static void logClickAdsEvent(Context context, Bundle bundle) {
//        AppEventsLogger.newLogger(context).logEvent("event_user_click_ads", bundle);
//    }
//
//    public static void logCurrentTotalRevenueAd(Context context, String eventName, Bundle bundle) {
//        AppEventsLogger.newLogger(context).logEvent(eventName, bundle);
//    }
//}
