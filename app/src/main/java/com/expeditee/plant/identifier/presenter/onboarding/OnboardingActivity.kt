package com.expeditee.plant.identifier.presenter.onboarding

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.dqh.lmtech.ads.admob.AppOpenManager
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.admob.LammaAd
import com.expeditee.plant.identifier.admob.NativeAdView
import com.expeditee.plant.identifier.admob.intro.IntroAdHelper
import com.expeditee.plant.identifier.admob.manager.NativeCallback
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityOnboardingBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.presenter.home.HomeActivity
import com.expeditee.plant.identifier.presenter.onboarding.page.AdPageFragment
import com.expeditee.plant.identifier.presenter.onboarding.page.OnboardingPagerAdapter
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class OnboardingActivity :
    BaseActivity<ActivityOnboardingBinding, OnboardingViewModel, OnboardingState>(),
    AdPageFragment.Listener {

    private val introAdHelper by inject<IntroAdHelper>()
    private lateinit var onboardingPager: OnboardingPagerAdapter

    private val autoAdvanceHandler = Handler(Looper.getMainLooper())
    private val autoAdvanceDelay = 3_000L
    private val autoAdvanceRunnable = Runnable { handleAutoAdvance() }

    override fun getLazyViewModel() = viewModel<OnboardingViewModel>()
    override fun provideLayout() = R.layout.activity_onboarding
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        enableConfigEdgeSystemBar(isFitsSystemWindows = false, isLight = false)
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        // Ads disabled
        super.initAds(block)
    }

    private fun scheduleAutoAdvanceIfNeeded() {
        if (!configManagement.enableOnboardingAutoNext) return
        if (!::onboardingPager.isInitialized) return
        cancelAutoAdvance()
        autoAdvanceHandler.postDelayed(autoAdvanceRunnable, autoAdvanceDelay)
    }

    private fun cancelAutoAdvance() {
        autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable)
    }

    private fun handleAutoAdvance() {
        if (isFinishing || isDestroyed) return
        val currentIndex = viewBinding.vpOnboarding.currentItem
        if (currentIndex < onboardingPager.lastPageIndex) {
            viewBinding.vpOnboarding.currentItem = currentIndex + 1
            scheduleAutoAdvanceIfNeeded()
        } else {
            appConfigSettings.introCompleted = true
            onNavigateToNextScreen()
        }
    }

    private fun updateUiForPage(pageIndex: Int) {
        when (val page = onboardingPager.page(pageIndex)) {
            is OnboardingPage.FullscreenAd -> {
                viewBinding.nativeAdView.beGone()
                viewBinding.showIndicator = false
                viewBinding.showSwipe = true
                viewBinding.fullContent = true
            }

            is OnboardingPage.Content -> {
                viewBinding.showIndicator = true
                viewBinding.showSwipe = false
                viewBinding.fullContent = false

                if (!page.admobEnable || true) {
                    viewBinding.nativeAdView.removeAllViews()
                    viewBinding.nativeAdView.beGone()
                    return
                }
                viewBinding.nativeAdView.beVisible()
                loadAndShowNativeOnboarding(page)
            }
        }
    }

    private fun loadAndShowNativeOnboarding(page: OnboardingPage.Content) {
        fun getNativeCallback() = object : NativeCallback {
            override fun onLoaded(data: LammaAd<ApNativeAd>?) {
                introAdHelper.removeNativeAdCallback(this)
                if (isFinishing || isDestroyed) return
                viewBinding.nativeAdView.populateUnifiedView(data)
            }

            override fun onFailed() {
                introAdHelper.removeNativeAdCallback(this)
                if (isFinishing || isDestroyed) return
                viewBinding.nativeAdView.takeIf { !it.populated }?.beGone()
            }
        }

        when (page.placement) {
            IntroAdHelper.NATIVE_ONBOARDING -> {
                val pendingLanguageClickAd = introAdHelper.consumePendingLanguageClick()
                if (pendingLanguageClickAd?.ad != null) {
                    viewBinding.nativeAdView.populateUnifiedView(
                        pendingLanguageClickAd,
                        R.layout.ads_native_large
                    )
                    return
                }
                introAdHelper.onLoadNativeOnboarding1(
                    context = this@OnboardingActivity,
                    callback = getNativeCallback()
                )
            }
            IntroAdHelper.NATIVE_ONBOARDING_2 -> introAdHelper.onLoadNativeOnboarding2(
                context = this@OnboardingActivity,
                callback = getNativeCallback()
            )
            IntroAdHelper.NATIVE_ONBOARDING_LAST -> introAdHelper.onLoadNativeOnboardingLast(
                context = this@OnboardingActivity,
                callback = getNativeCallback()
            )
        }
    }

    private fun setupViewPager(pages: List<OnboardingPage>) {
        onboardingPager = OnboardingPagerAdapter(this, pages)
        with(viewBinding.vpOnboarding) {
            adapter = onboardingPager
            offscreenPageLimit = pages.lastIndex
            clipToPadding = false
            clipChildren = false
            getChildAt(0)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setPageTransformer(getPageTransformer())
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.setCurrentPage(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        scheduleAutoAdvanceIfNeeded()
                    }
                }
            })
            viewBinding.dotIndicator.attachTo(this)
        }
        scheduleAutoAdvanceIfNeeded()
    }

    private fun getPageTransformer(): CompositePageTransformer {
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(100))
        compositePageTransformer.addTransformer { view, position ->
            val r = 1 - abs(position)
            view.scaleY = 0.8f + r * 0.2f
            val absPosition = abs(position)
            view.alpha = 1.0f - (1.0f - 0.3f) * absPosition
        }
        return compositePageTransformer
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.onboardingPages, Lifecycle.State.CREATED) {
            val pages = it.takeIf { it.isNotEmpty() } ?: return@collectLatestRepeatOnLifecycle
            if (viewBinding.vpOnboarding.adapter == null) {
                setupViewPager(pages)
            }
        }
        collectLatestRepeatOnLifecycle(viewModel.currentPage, Lifecycle.State.CREATED) { position ->
            if (position == RecyclerView.NO_POSITION) return@collectLatestRepeatOnLifecycle
            try {
                if (position in viewModel.onboardingPagesValue.indices) {
                    if (viewModel.onboardingPagesValue[position] is OnboardingPage.FullscreenAd) {
                        AppOpenManager.getInstance().disableAppResume()
                    } else {
                        adsManager.applyOpenResumeAdConfig()
                    }
                    updateUiForPage(position)
                    viewBinding.tvAction.text = getString(
                        if (position == onboardingPager.lastPageIndex) R.string.txt_continue
                        else R.string.next
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.onNextPageClicked = View.OnClickListener {
            scheduleAutoAdvanceIfNeeded()
            val currentIndex = viewBinding.vpOnboarding.currentItem
            if (currentIndex < onboardingPager.lastPageIndex) {
                viewBinding.vpOnboarding.currentItem = currentIndex + 1
            } else {
                appConfigSettings.introCompleted = true
                onNavigateToNextScreen()
            }
        }
    }

    private fun onNavigateToNextScreen() {
        cancelAutoAdvance()
        startActivity<HomeActivity> {}
        finish()
    }

    override fun onLoadNativeFull(nativeAdView: NativeAdView, pagePosition: Int) {
        introAdHelper.onLoadNativeOnboardingFull(
            context = this@OnboardingActivity,
            callback = object : NativeCallback {
                override fun onLoaded(data: LammaAd<ApNativeAd>?) {
                    introAdHelper.removeNativeAdCallback(this)
                    if (isFinishing || isDestroyed) return
                    nativeAdView.populateUnifiedView(data)
                }

                override fun onFailed() {
                    introAdHelper.removeNativeAdCallback(this)
                    if (isFinishing || isDestroyed) return
                    if (viewModel.currentPage.value == pagePosition) {
                        nextPage()
                    }
                }
            }
        )
    }

    private fun nextPage() {
        try {
            val newIndex = viewBinding.vpOnboarding.currentItem + 1
            if (newIndex in viewModel.onboardingPagesValue.indices) {
                viewBinding.vpOnboarding.currentItem = newIndex
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        cancelAutoAdvance()
    }

    override fun onResume() {
        super.onResume()
        scheduleAutoAdvanceIfNeeded()
    }

    override fun onDestroy() {
        cancelAutoAdvance()
        super.onDestroy()
    }
}
