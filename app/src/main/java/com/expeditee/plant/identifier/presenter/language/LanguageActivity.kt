package com.expeditee.plant.identifier.presenter.language

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.dqh.lmtech.ads.billing.AppPurchase
import com.dqh.lmtech.ads.funtion.AdCallback
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.admob.LammaAd
import com.expeditee.plant.identifier.admob.intro.IntroAdHelper
import com.expeditee.plant.identifier.admob.manager.NativeCallback
import com.expeditee.plant.identifier.admob.utils.onLoadNativeAd
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityLanguageBinding
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.delayTimeTicker
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.extension.makeGone
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.runDelayOnUiThread
import com.expeditee.plant.identifier.extension.serializable
import com.expeditee.plant.identifier.extension.setEnable
import com.expeditee.plant.identifier.extension.showToast
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.presenter.home.HomeActivity
import com.expeditee.plant.identifier.services.LanguageDataPrefetchService
import com.expeditee.plant.identifier.widget.dialog.LanguageClickAdDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject

class LanguageActivity :
    BaseActivity<ActivityLanguageBinding, LanguageViewModel, BaseViewModel.State>() {

    companion object {
        private const val TAG_KOG = "LanguageActivity_12312"

        const val EXTRA_SCREEN = ".EXTRA_SCREEN"

        private const val LOADING_TIMEOUT_MS = 8000L
        private const val LOADING_LIMIT_MS = 2000L
        private const val CLICK_AD_TIMEOUT_MS = 5000L
        private const val SECONDARY_AD_DELAY_MS = 100L

        private const val NATIVE_LANGUAGE = 0
        private const val NATIVE_LANGUAGE_CLICK = 1
        private const val NATIVE_LANGUAGE_CLICK2 = 2
    }

    override fun getLazyViewModel() = viewModel<LanguageViewModel>()
    override fun provideLayout() = R.layout.activity_language

    private var mNativeLanguages: Array<ApNativeAd?> = arrayOf(null, null, null)
    private val languagePrefetchService: LanguageDataPrefetchService by inject()
    private val introAdHelper: IntroAdHelper by inject()

    private val nextScreen by lazy { intent?.serializable(EXTRA_SCREEN) as? Class<*> }
    private val shouldBackScreen: Boolean by lazy { nextScreen != null }

    private val enableAutoNextLanguage
        get() = configManagement.enableLanguageAutoNext

    private val languageAdapter: LanguageAdapter by lazy {
        LanguageAdapter(emptyList()) {
            if (enableAutoNextLanguage) onEventLanguageClicked() else onLanguageClicked()
        }
    }

    private var autoApplyLanguageRunnable: Runnable? = null
    private val autoApplyLanguageHandler by lazy { Handler(mainLooper) }
    private var autoRemainingMs = 3000L
    private var autoStartAt = 0L
    private var isAutoWaiting = false

    private var wasShowAnimTap: Boolean = false
    private var wasShowAnimApply: Boolean = false
    private var languageClickTimeoutJob: Job? = null
    private var languageClickNativeCallback: NativeCallback? = null
    private var languageClickDialog: LanguageClickAdDialog? = null
    private var displayedLanguageClickAd: ApNativeAd? = null
    private var isLanguageClickLoading = false

    override fun initViews(savedInstanceState: Bundle?) {
        enableConfigEdgeSystemBar(false)
        setupLanguageAdapter()
        setupApplyWindowInsetListener {
            viewBinding.toolbar.updateLayoutParams<MarginLayoutParams> {
                topMargin = it.statusBars().top
            }
        }
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds(block)
        // Ads disabled: no native language ads.
        viewBinding.nativeView.makeGone()
        viewBinding.blurView.setEnable(false, viewBinding.cstContainer)
        handleShowAnimTap()
    }

    private suspend fun onLoadNativeLanguage() {
        Log.d(TAG_KOG, "Starting loading native language")
        runCatching {
            val unitAdId = BuildConfig.planty_native_language_2ID
            Log.d(TAG_KOG, "Loading native language 2ID - $unitAdId")
            onLoadNativeAd(this, unitAdId)?.also { apNativeAd ->
                Log.d(TAG_KOG, "Native language 2ID loaded - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE] = apNativeAd
                return
            }
            Log.d(TAG_KOG, "Native language 2ID failed")
        }
        runCatching {
            val unitAdId = BuildConfig.planty_native_language
            Log.d(TAG_KOG, "Loading native language normal - $unitAdId")
            onLoadNativeAd(this, unitAdId)?.also { apNativeAd ->
                Log.d(TAG_KOG, "Native language normal loaded - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE] = apNativeAd
                return
            }
            Log.d(TAG_KOG, "Native language normal failed")
        }
    }

    private suspend fun onLoadNativeLanguageClick() {
        Log.d(TAG_KOG, "Starting loading native language click")
        val nativeCallback = if (enableAutoNextLanguage) {
            object : AdCallback() {
            override fun onAdClicked() {
                runOnUiThread {
                    Log.d(TAG_KOG, "onAdClicked -> pause timer")
                    pauseAutoApplyCountdown()
                }
            }
        }
        } else null

        runCatching {
            val unitAdId = BuildConfig.planty_native_language_click_2ID
            Log.d(TAG_KOG, "Loading native language click 2ID - $unitAdId")
            onLoadNativeAd(this, unitAdId, callback = nativeCallback)?.also { apNativeAd ->
                Log.d(TAG_KOG, "Native language click 2ID loaded - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE_CLICK] = apNativeAd
                Log.d(TAG_KOG, "Starting preloading native language click 2ID - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE_CLICK2] = runCatching {
                    Log.d(TAG_KOG, "Native language click 2ID preloaded - $unitAdId")
                    onLoadNativeAd(this, unitAdId, callback = nativeCallback)
                }.getOrNull()
                return
            }
            Log.d(TAG_KOG, "Native language click 2ID failed")
        }
        runCatching {
            val unitAdId = BuildConfig.planty_native_language_click
            Log.d(TAG_KOG, "Loading native language click normal - $unitAdId")
            onLoadNativeAd(this, unitAdId, callback = nativeCallback)?.also { nativeAd ->
                Log.d(TAG_KOG, "Native language click normal loaded - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE_CLICK] = nativeAd
                Log.d(TAG_KOG, "Starting preloading native language click normal - $unitAdId")
                mNativeLanguages[NATIVE_LANGUAGE_CLICK2] = runCatching {
                    Log.d(TAG_KOG, "Native language click normal preloaded - $unitAdId")
                    onLoadNativeAd(this, unitAdId, callback = nativeCallback)
                }.getOrNull()
                return
            }
            Log.d(TAG_KOG, "Native language click normal failed")
        }
    }

    private fun setupLoadingState() {
        if (enableAutoNextLanguage) {
            setupLoadingStateAuto()
        } else {
            setupLoadingStateDefault()
        }
    }

    private fun setupLoadingStateAuto() {
        viewBinding.blurView.setEnable(true, viewBinding.cstContainer)
        viewBinding.blurView.delayTimeTicker(
            lifecycleOwner = this@LanguageActivity,
            delayMillis = LOADING_TIMEOUT_MS,
            onTick = {
                mNativeLanguages[NATIVE_LANGUAGE]?.let { nativeAd ->
                    viewBinding.nativeView.onPopulatedListener = { handleShowAnimTap() }
                    viewBinding.nativeView.populateUnifiedView(
                        nativeAd, com.dqh.lmtech.ads.R.layout.ads_native_large_gray
                    )
                    return@delayTimeTicker true
                }
                false
            },
            onFinished = {
                mNativeLanguages[NATIVE_LANGUAGE]?.let {
                    viewBinding.nativeView.onPopulatedListener = { handleShowAnimTap() }
                    viewBinding.nativeView.populateUnifiedView(
                        it, com.dqh.lmtech.ads.R.layout.ads_native_large_gray
                    )
                } ?: handleShowAnimTap()
                viewBinding.blurView.setEnable(false, viewBinding.cstContainer)
            }
        )
    }

    private fun setupLoadingStateDefault() {
        viewBinding.blurView.beVisible()
        viewBinding.blurView.setEnable(true, viewBinding.cstContainer)
        viewBinding.blurView.delayTimeTicker(
            lifecycleOwner = this@LanguageActivity,
            delayMillis = LOADING_TIMEOUT_MS,
            onTick = {
                Log.d(TAG_KOG, "setupLoadingState: $it")
                if (it <= LOADING_LIMIT_MS) return@delayTimeTicker false
                mNativeLanguages[NATIVE_LANGUAGE]?.let { nativeAd ->
                    viewBinding.nativeView.onPopulatedListener = { handleShowAnimTap() }
                    viewBinding.nativeView.populateUnifiedView(
                        nativeAd, com.dqh.lmtech.ads.R.layout.ads_native_large_gray
                    )
                    return@delayTimeTicker true
                }
                return@delayTimeTicker false
            },
            onFinished = {
                mNativeLanguages[NATIVE_LANGUAGE]?.let {
                    viewBinding.nativeView.onPopulatedListener = { handleShowAnimTap() }
                    viewBinding.nativeView.populateUnifiedView(
                        it,
                        com.dqh.lmtech.ads.R.layout.ads_native_large_gray
                    )
                } ?: handleShowAnimTap()
                viewBinding.blurView.setEnable(false, viewBinding.cstContainer)
            }
        )
    }

    private fun setupLanguageAdapter() {
        viewBinding.ivBack.isVisible = shouldBackScreen
        with(viewBinding.rvLanguage) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = languageAdapter
        }
    }

    private fun onLanguageClicked() {
        wasShowAnimTap = true
        viewBinding.lottieTap.makeGone()
        if (!canLoadLanguageClickNative()) {
            showApplyButton()
            return
        }

        if (showDisplayedLanguageClickOnMain()) {
            showApplyButton()
            return
        }

        startLanguageClickLoad()
    }

    private fun canLoadLanguageClickNative(): Boolean {
        return !shouldBackScreen &&
                isNetwork() &&
                configManagement.enableNativeLanguage &&
                !AppPurchase.getInstance().isPurchased
    }

    private fun startLanguageClickLoad() {
        if (isLanguageClickLoading) return
        cancelLanguageClickJobs()
        languageClickDialog?.dismiss()
        isLanguageClickLoading = true
        displayedLanguageClickAd = null

        viewBinding.ivApply.makeGone()
        viewBinding.pbApplyLoading.beVisible()
        viewBinding.nativeView.beVisible()
        viewBinding.nativeView.onPopulatedListener = null
        viewBinding.nativeView.setNativeLayout(R.layout.ads_native_large)
        viewBinding.nativeView.setShimmerLayout(com.dqh.lmtech.ads.R.layout.ads_shimmer_large)
        requestLanguageClickNative()

        languageClickTimeoutJob = lifecycleScope.launch {
            delay(CLICK_AD_TIMEOUT_MS)
            if (!isLanguageClickLoading || isDestroyed || isFinishing) return@launch
            showApplyButton()
            showLanguageClickTimeoutDialog()
        }
    }

    private fun onLanguageClickLoadFinished() {
        if (isDestroyed || isFinishing) {
            isLanguageClickLoading = false
            languageClickTimeoutJob?.cancel()
            languageClickTimeoutJob = null
            return
        }
        isLanguageClickLoading = false
        languageClickTimeoutJob?.cancel()
        languageClickTimeoutJob = null
        showApplyButton()

        if (showLanguageClickAdIfPossible()) {
            return
        }

        if (isLanguageClickShown()) {
            Log.d(TAG_KOG, "Language click callback finished without UI consume; keep current state")
            return
        }

        viewBinding.nativeView.removeAllViews()
    }

    private fun showLanguageClickAdIfPossible(): Boolean {
        if (isDestroyed || isFinishing) {
            return false
        }
        val adToShow = introAdHelper.consumePendingLanguageClick()?.ad ?: return false
        if (languageClickDialog?.isShowing == true) {
            populateLanguageClickOnPopup(adToShow)
            return true
        }

        populateLanguageClickOnMain(adToShow)
        return true
    }

    private fun showApplyButton() {
        viewBinding.pbApplyLoading.makeGone()
        viewBinding.ivApply.beVisible()
        handleShowAnimApply()
    }

    private fun requestLanguageClickNative() {
        if (shouldBackScreen) return
        val callback = object : NativeCallback {
            override fun onLoaded(data: LammaAd<ApNativeAd>?) {
                introAdHelper.removePendingLanguageClickCallback(this)
                if (languageClickNativeCallback === this) {
                    languageClickNativeCallback = null
                }
                onLanguageClickLoadFinished()
            }

            override fun onFailed() {
                introAdHelper.removePendingLanguageClickCallback(this)
                if (languageClickNativeCallback === this) {
                    languageClickNativeCallback = null
                }
                onLanguageClickLoadFinished()
            }
        }
        languageClickNativeCallback = callback
        introAdHelper.loadNativeLanguageClickForSelection(this, callback)
    }

    private fun showLanguageClickTimeoutDialog() {
        if (languageClickDialog?.isShowing == true || isDestroyed || isFinishing) return
        val selectedLanguageName =
            languageAdapter.selectedLanguage?.getEnglishDisplayName() ?: return
        try {
            val dialog = LanguageClickAdDialog(
                context = this,
                selectedLanguageName = selectedLanguageName,
                onApplyClick = ::onHandleSetupLocale,
                onDismissed = { wasApplied ->
                    languageClickDialog = null
                    if (!wasApplied && !isDestroyed && !isFinishing) {
                        displayedLanguageClickAd?.let {
                            populateLanguageClickOnMain(it)
                        }
                    }
                },
            )
            languageClickDialog = dialog
            dialog.show()
            displayedLanguageClickAd?.let(dialog::populateNativeAd) ?: showLanguageClickAdIfPossible()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populateLanguageClickDialog(nativeAd: ApNativeAd?) {
        languageClickDialog?.populateNativeAd(nativeAd)
    }

    private fun populateLanguageClickOnPopup(nativeAd: ApNativeAd) {
        displayedLanguageClickAd = nativeAd
        populateLanguageClickDialog(nativeAd)
    }

    private fun populateLanguageClickOnMain(nativeAd: ApNativeAd) {
        try {
            displayedLanguageClickAd = nativeAd
            viewBinding.nativeView.beVisible()
            viewBinding.nativeView.onPopulatedListener = { handleShowAnimApply() }
            viewBinding.nativeView.populateUnifiedView(nativeAd, R.layout.ads_native_large)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDisplayedLanguageClickOnMain(): Boolean {
        val displayedAd = displayedLanguageClickAd ?: return false
        populateLanguageClickOnMain(displayedAd)
        return true
    }

    private fun isLanguageClickShown(): Boolean {
        return displayedLanguageClickAd != null ||
                (languageClickDialog?.takeIf { it.isShowing }?.isAdShown() == true)
    }

    private fun cancelLanguageClickJobs() {
        languageClickNativeCallback?.let {
            introAdHelper.removePendingLanguageClickCallback(it)
        }
        languageClickTimeoutJob?.cancel()
        languageClickNativeCallback = null
        languageClickTimeoutJob = null
        isLanguageClickLoading = false
    }

    private fun onEventLanguageClicked() {
        Log.d(TAG_KOG, "onEventLanguageClicked: autoNext=$enableAutoNextLanguage, shouldBackScreen=$shouldBackScreen")
        wasShowAnimTap = true
        viewBinding.lottieTap.makeGone()
        viewBinding.ivApply.beVisible()
        if (!shouldBackScreen && false) {
            showLanguageClickNative()
        }
        handleSetupAutoNextPage()
    }

    private fun handleSetupAutoNextPage() {
        Log.d(TAG_KOG, "handleSetupAutoNextPage: start 3000ms")
        cancelAutoApplyLanguage(clear = true)
        autoRemainingMs = 3000L
        isAutoWaiting = true
        startAutoApplyCountdown()
    }

    private fun startAutoApplyCountdown() {
        if (!isAutoWaiting) return
        autoStartAt = android.os.SystemClock.elapsedRealtime()
        autoApplyLanguageRunnable = Runnable {
            Log.d(TAG_KOG, "autoApply fired")
            isAutoWaiting = false
            autoRemainingMs = 3000L
            onHandleSetupLocale()
        }
        autoApplyLanguageHandler.postDelayed(autoApplyLanguageRunnable!!, autoRemainingMs)
    }

    private fun pauseAutoApplyCountdown() {
        if (!isAutoWaiting || autoApplyLanguageRunnable == null) return
        autoApplyLanguageHandler.removeCallbacks(autoApplyLanguageRunnable!!)
        val elapsed = android.os.SystemClock.elapsedRealtime() - autoStartAt
        autoRemainingMs = (autoRemainingMs - elapsed).coerceAtLeast(0L)
        Log.d(TAG_KOG, "pauseAutoApplyCountdown: remaining=$autoRemainingMs")
        autoApplyLanguageRunnable = null
    }

    private fun resumeAutoApplyCountdown() {
        if (!isAutoWaiting || autoRemainingMs <= 0L) return
        Log.d(TAG_KOG, "resumeAutoApplyCountdown: remaining=$autoRemainingMs")
        startAutoApplyCountdown()
    }

    private fun cancelAutoApplyLanguage(clear: Boolean = true) {
        Log.d(TAG_KOG, "cancelAutoApplyLanguage: runnable=${autoApplyLanguageRunnable != null}")
        autoApplyLanguageRunnable?.let {
            autoApplyLanguageHandler.removeCallbacks(it)
        }
        autoApplyLanguageRunnable = null
        if (clear) {
            isAutoWaiting = false
            autoRemainingMs = 3000L
        }
    }

    private fun showLanguageClickNative() {
        try {
            viewBinding.nativeView.onPopulatedListener = { handleShowAnimApply() }
            val primaryAd = mNativeLanguages[NATIVE_LANGUAGE_CLICK]
            if (primaryAd != null) {
                viewBinding.nativeView.populateUnifiedView(primaryAd, R.layout.ads_native_large)
                return
            }
            runDelayOnUiThread(SECONDARY_AD_DELAY_MS) {
                if (isDestroyed || isFinishing) return@runDelayOnUiThread
                mNativeLanguages[NATIVE_LANGUAGE_CLICK2]?.let {
                    viewBinding.nativeView.populateUnifiedView(it, R.layout.ads_native_large)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleShowAnimTap() {
        viewBinding.lottieTap.takeIf { !it.isVisible && !wasShowAnimTap }
            ?.postDelayed(2000L) {
                viewBinding.lottieTap.takeIf { !it.isVisible && !wasShowAnimTap }
                    ?.beVisible()
                    ?.also {
                        wasShowAnimTap = true
                    }
            }
    }

    private fun handleShowAnimApply() {
        viewBinding.lottieTapDone.takeIf { !it.isVisible && !wasShowAnimApply }
            ?.postDelayed(2000L) {
                viewBinding.lottieTapDone.takeIf { !it.isVisible && !wasShowAnimApply }
                    ?.beVisible()
                    ?.also {
                        wasShowAnimApply = true
                    }
            }
    }

    private fun populateBaseAdIfNeeded() {
        if (!viewBinding.nativeView.populated) {
            mNativeLanguages[NATIVE_LANGUAGE]?.let { nativeAd ->
                viewBinding.nativeView.onPopulatedListener = { handleShowAnimTap() }
                viewBinding.nativeView.populateUnifiedView(
                    nativeAd, com.dqh.lmtech.ads.R.layout.ads_native_large_gray
                )
            }
        }
    }

    private fun onHandleSetupLocale() {
        Log.d(TAG_KOG, "onHandleSetupLocale: fired")
        val language = languageAdapter.selectedLanguage
        if (language == null) {
            Log.d(TAG_KOG, "onHandleSetupLocale: no language selected")
            showToast(R.string.please_select_language)
            return
        }
        val currentLanguage = appConfigSettings.currentLanguage
        val isChanged = language.isoCountry != currentLanguage
        Log.d(
            TAG_KOG,
            "onHandleSetupLocale: selected=${language.isoCountry}, current=$currentLanguage, isChanged=$isChanged"
        )
        if (isChanged) {
            appConfigSettings.currentLanguage = language.isoCountry
            languagePrefetchService.schedulePrefetch(
                languageCode = language.isoCountry,
                forceRefresh = true,
            )
        }
        navigateNextScreen(isChanged)
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.languageList, Lifecycle.State.CREATED) {
            languageAdapter.submitData(it.orEmpty())
        }
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.onApplyLanguageClicked = onDebounceClick {
            if (enableAutoNextLanguage) {
                viewBinding.lottieTapDone.makeGone()
                wasShowAnimApply = true
            }
            onHandleSetupLocale()
        }
    }

    private fun navigateNextScreen(wasChanged: Boolean) {
        Log.d(TAG_KOG, "navigateNextScreen: wasChanged=$wasChanged, shouldBackScreen=$shouldBackScreen")
        cancelAutoApplyLanguage(clear = true)
        cancelLanguageClickJobs()
        languageClickDialog?.setOnDismissListener(null)
        languageClickDialog?.dismiss()
        languageClickDialog = null
        if (shouldBackScreen) {
            if (!wasChanged) {
                Log.d(TAG_KOG, "navigateNextScreen: returning back without language change")
                onBackPressedDispatcher.onBackPressed()
                return
            }
            Log.d(TAG_KOG, "navigateNextScreen: launching nextScreen=$nextScreen")
            Intent(this@LanguageActivity, nextScreen).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
                finish()
            }
            return
        }
        Log.d(TAG_KOG, "navigateNextScreen: going to home (skip onboarding/premium)")
        navigateToHomeAfterLanguage()
    }

    private fun navigateToHomeAfterLanguage() {
        viewModel.introCompleted = true
        startActivity<HomeActivity> {}
        finish()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_KOG, "onResume: autoNext=$enableAutoNextLanguage")
        if (enableAutoNextLanguage) {
            resumeAutoApplyCountdown()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG_KOG, "onPause: autoNext=$enableAutoNextLanguage")
        if (enableAutoNextLanguage) {
            pauseAutoApplyCountdown()
        }
    }

    override fun onDestroy() {
        Log.d(TAG_KOG, "onDestroy")
        if (enableAutoNextLanguage) {
            cancelAutoApplyLanguage()
        }
        cancelLanguageClickJobs()
        languageClickDialog?.dismiss()
        languageClickDialog = null
        displayedLanguageClickAd = null
        viewBinding.blurView.setEnable(false, viewBinding.cstContainer)
        mNativeLanguages.filterNotNull().forEach { it.admobNativeAd.destroy() }
        mNativeLanguages = arrayOf(null, null, null)
        super.onDestroy()
    }
}
