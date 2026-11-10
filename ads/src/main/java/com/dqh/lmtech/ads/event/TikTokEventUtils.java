package com.dqh.lmtech.ads.event;

import android.content.Context;
import android.os.Bundle;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.base.EventName;
import com.tiktok.appevents.base.TTBaseEvent;


public class TikTokEventUtils {
    public static void logEventWithAds(Context context, Bundle params) {
        TikTokBusinessSdk.trackTTEvent(EventName.ACHIEVE_LEVEL, String.valueOf(EventName.IN_APP_AD_IMPR));
    }

    public static void logEventWithAdImpression(Context context, Double revenue) {

        //Report custom events
        TTBaseEvent testInfo = TTBaseEvent.newBuilder(EventName.IN_APP_AD_IMPR.toString()) //event constant
        //If you need to add eventID:
                .addProperty("currency", "USD") //The ISO 4217 currency code
                .addProperty("price", revenue) //The price of the item
                .addProperty("quantity", 1) //The number of items
                .build();
        TikTokBusinessSdk.trackTTEvent(testInfo);
    }
}
