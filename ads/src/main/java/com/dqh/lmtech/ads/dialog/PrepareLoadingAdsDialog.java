package com.dqh.lmtech.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.dqh.lmtech.ads.R;

public class PrepareLoadingAdsDialog extends Dialog {

    public PrepareLoadingAdsDialog(Context context) {
        super(context, R.style.FullScreenDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_prepair_loading_ads);
    }
}
