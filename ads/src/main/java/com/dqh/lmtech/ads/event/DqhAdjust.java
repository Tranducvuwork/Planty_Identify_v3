package com.dqh.lmtech.ads.event;

import android.content.Context;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.applovin.mediation.MaxAd;
import com.dqh.lmtech.ads.ads.DqhAd;
import com.google.android.gms.ads.AdValue;

public class DqhAdjust {
    public static boolean enableAdjust = false;

    public static void onTrackRevenue(String eventName, float revenue, String currency) {
        AdjustEvent event = new AdjustEvent(eventName);
        // Add revenue 1 cent of an euro.
        event.setRevenue(revenue / 1000000.0, currency);
        Adjust.trackEvent(event);
    }

    public static void onTrackRevenuePurchase(float revenue, String currency) {
        if (DqhAdjust.enableAdjust) {
            String eventNamePurchase = "";
            onTrackRevenue(eventNamePurchase, revenue, currency);
        }
    }

    public static void pushTrackEventAdmob(AdValue adValue) {
        if (DqhAdjust.enableAdjust) {
            AdjustAdRevenue adRevenue = new AdjustAdRevenue("admob_sdk");
            adRevenue.setRevenue(adValue.getValueMicros() / 1000000.0, adValue.getCurrencyCode());

            Adjust.trackAdRevenue(adRevenue);

            AdjustEvent event = new AdjustEvent(DqhAd.getInstance().getAdConfig().getAdjustConfig().getEventAdImpression());
            event.setRevenue(adValue.getValueMicros() / 1000000.0, adValue.getCurrencyCode());
            Adjust.trackEvent(event);
        }
    }

    public static void onTrackEvent(String eventName) {
        AdjustEvent event = new AdjustEvent(eventName);
        Adjust.trackEvent(event);
    }

    public static void onTrackEvent(String eventName, String id) {
        AdjustEvent event = new AdjustEvent(eventName);
        event.setCallbackId(id);
        Adjust.trackEvent(event);
    }
}
