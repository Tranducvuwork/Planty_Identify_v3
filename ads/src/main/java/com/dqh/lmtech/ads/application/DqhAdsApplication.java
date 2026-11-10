package com.dqh.lmtech.ads.application;

import android.app.Application;

import com.dqh.lmtech.ads.config.DqhTechAdConfig;
import com.dqh.lmtech.ads.util.AppUtil;
import com.dqh.lmtech.ads.util.SharePreferenceUtils;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public abstract class DqhAdsApplication extends Application {

    protected DqhTechAdConfig mDqhTechAdConfig;
    protected List<String> listTestDevice;

    @Override
    public void onCreate() {
        super.onCreate();
        listTestDevice = new ArrayList<String>();
        mDqhTechAdConfig = new DqhTechAdConfig(this);
        if (SharePreferenceUtils.getInstallTime(this) == 0) {
            SharePreferenceUtils.setInstallTime(this);
        }
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this);
    }

}
