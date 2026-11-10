package com.expeditee.plant.identifier.presenter.home

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.databinding.ActivityHomeBinding
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.utils.NetworkUtil
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel, HomeState>() {

    private lateinit var pagerAdapter: HomePagerAdapter
    private var selectedTab = 0
    private var centerScanPulse: ObjectAnimator? = null

    override fun getLazyViewModel() = viewModel<HomeViewModel>()
    override fun provideLayout() = R.layout.activity_home
    override fun isFitsSystemWindows(): Boolean = false

    override fun initViews(savedInstanceState: Bundle?) {
        enableConfigEdgeSystemBar(false, isLight = false)
        setupViewPager()
        setupBottomNav()
        selectTab(0)
    }

    override fun onResume() {
        super.onResume()
        if (NetworkUtil.isNetworkAvailable(this)) {
            viewModel.onState(HomeState.PreloadPlants)
        }
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?): Boolean {
        super.onHandleBackPressed {
            finish()
        }
        return true
    }

    private fun setupViewPager() {
        pagerAdapter = HomePagerAdapter(this@HomeActivity)
        with(viewBinding.viewPage2) {
            adapter = pagerAdapter
            offscreenPageLimit = pagerAdapter.itemCount
            isUserInputEnabled = false
            currentItem = 0
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    selectTab(position, fromPager = true)
                }
            })
        }
    }

    private fun setupBottomNav() {
        val nav = viewBinding.bottomNavInclude
        nav.tabHome.setOnClickListener { selectTab(0) }
        nav.tabHistory.setOnClickListener { selectTab(1) }
        nav.tabFavorite.setOnClickListener { selectTab(2) }
        nav.tabSettings.setOnClickListener { selectTab(3) }
        nav.btnCenterScan.setOnClickListener {
            ScanCameraActivity.start(this, CameraMode.IDENTIFY)
        }
        startCenterScanPulse()
    }

    private fun startCenterScanPulse() {
        centerScanPulse?.cancel()
        val target = viewBinding.bottomNavInclude.btnCenterScan
        centerScanPulse = ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f, 1f),
        ).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun selectTab(index: Int, fromPager: Boolean = false) {
        selectedTab = index
        if (!fromPager) {
            viewBinding.viewPage2.setCurrentItem(index, false)
        }
        val nav = viewBinding.bottomNavInclude
        val green = ContextCompat.getColor(this, R.color.primary_green)
        val gray = 0xFF757575.toInt()
        fun style(tab: LinearLayout, icon: ImageView, label: TextView, selected: Boolean) {
            val color = if (selected) green else gray
            icon.setColorFilter(color)
            label.setTextColor(color)
            tab.isSelected = selected
        }
        style(nav.tabHome, nav.ivTabHome, nav.tvTabHome, index == 0)
        style(nav.tabHistory, nav.ivTabHistory, nav.tvTabHistory, index == 1)
        style(nav.tabFavorite, nav.ivTabFavorite, nav.tvTabFavorite, index == 2)
        style(nav.tabSettings, nav.ivTabSettings, nav.tvTabSettings, index == 3)
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        // No banner under bottom navigation.
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
    }

    override fun onDestroy() {
        centerScanPulse?.cancel()
        centerScanPulse = null
        super.onDestroy()
    }
}
