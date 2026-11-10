@file:Suppress("CustomSplashScreen")

package com.expeditee.plant.identifier.presenter.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.CommonViewModel
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.databinding.ActivitySplashBinding
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.presenter.home.HomeActivity
import com.expeditee.plant.identifier.presenter.language.LanguageActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : BaseActivity<ActivitySplashBinding, CommonViewModel, BaseViewModel.State>() {

    companion object {
        private const val MIN_SPLASH_MS = 800L
    }

    override fun getLazyViewModel() = viewModel<CommonViewModel>()
    override fun provideLayout() = R.layout.activity_splash

    private var nextScreen: AtomicBoolean = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    override fun isFitsSystemWindows(): Boolean = false
    override fun isHideSystemBars(): Boolean = true

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.maxProgress = 100
        viewBinding.currentProgress = 30
        configManagement.fetchRemoteConfig {
            viewBinding.currentProgress = 90
            adsManager.applyOpenResumeAdConfig()
        }
        handler.postDelayed({ moveNextScreen() }, MIN_SPLASH_MS)
    }

    private fun moveNextScreen() {
        if (!nextScreen.compareAndSet(false, true)) return
        viewBinding.currentProgress = 100

        if (!viewModel.introCompleted) {
            startActivity<LanguageActivity> {
                finish()
            }
        } else {
            startActivity<HomeActivity> {
                finish()
            }
        }
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?): Boolean {
        return super.onHandleBackPressed {
            /* no-op */
        }
    }
}
