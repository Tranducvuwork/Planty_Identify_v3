package com.expeditee.plant.identifier.admob.loading

import android.os.Bundle
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.CommonViewModel
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.databinding.ActivityAdLoadingBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdLoadingActivity :
    BaseActivity<ActivityAdLoadingBinding, CommonViewModel, BaseViewModel.State>() {

    override fun getLazyViewModel() = viewModel<CommonViewModel>()
    override fun provideLayout() = R.layout.activity_ad_loading

    override fun isFitsSystemWindows(): Boolean = false
    override fun isHideSystemBars(): Boolean = true
    override fun initViews(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?): Boolean {
        return super.onHandleBackPressed {
            /* no-op */
        }
    }
}
