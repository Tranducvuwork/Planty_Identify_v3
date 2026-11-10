package com.expeditee.plant.identifier.common

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.expeditee.plant.identifier.admob.LammaAdManager
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

typealias OnPerformBackPressed = () -> Unit

abstract class BaseActivity<VB, VM, State> : AppCompatActivity()
        where VB : ViewDataBinding, VM : BaseViewModel<State>, State : BaseViewModel.State {

    protected val appConfigSettings by inject<AppConfigSettings>()
    protected val configManagement by inject<RemoteConfigManagement>()
    protected val adsManager by inject<LammaAdManager>()

    protected val viewModel: VM by this.getLazyViewModel()
    abstract fun getLazyViewModel(): Lazy<VM>

    protected val viewBinding: VB by lazy {
        DataBindingUtil.setContentView<VB>(this@BaseActivity, provideLayout())
    }

    abstract fun provideLayout(): Int

    private var onPerformBackPressed: OnPerformBackPressed? = null
    private var onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            onPerformBackPressed?.invoke()
        }
    }


    private val connectivityManager: ConnectivityManager? by lazy {
        ContextCompat.getSystemService(this@BaseActivity, ConnectivityManager::class.java)
    }

    protected fun setupApplyWindowInsetListener(block: (insets: WindowInsetsCompat) -> Unit) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insetCompact ->
            block(insetCompact)
            WindowInsetsCompat.CONSUMED
        }
    }


    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread {
                viewModel.setNetworkState(true)
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread {
                viewModel.setNetworkState(false)
            }
        }
    }

    protected val TAG: String
        get() = this::class.java.simpleName

    override fun attachBaseContext(newBase: Context) {
        var isoLanguage = appConfigSettings.currentLanguage
        if (isoLanguage.isNullOrEmpty()) {
            isoLanguage = Resources.getSystem().configuration.locales[0].language
        }
        val newLocale = Locale(isoLanguage.toString())
        Locale.setDefault(newLocale)
        val configuration = newBase.resources.configuration
        configuration.setLocale(newLocale)
        val newContext = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(ContextWrapper(newContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupInit()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewBinding.lifecycleOwner = this@BaseActivity
        makeDarkStatusBars()
        registerBackPressedDispatcher()

        initAds()
        initViews(savedInstanceState)
        initObservers()
        initListeners()

        observerNetworkState()
        observerHasPurchase()
    }

    protected open fun isLightStatusBar(): Boolean = true
    private fun makeDarkStatusBars() {
        WindowCompat.setDecorFitsSystemWindows(window, isFitsSystemWindows())
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = isLightStatusBar()
            controller.isAppearanceLightNavigationBars = isLightStatusBar()
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    protected open fun isFitsSystemWindows(): Boolean = true
    protected open fun isHideSystemBars(): Boolean = false

    protected open fun enableConfigEdgeSystemBar(
        isFitsSystemWindows: Boolean = true,
        isLight: Boolean = true
    ) {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = isLight

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }


    protected fun applyInsetToView(
        targetView: View = viewBinding.root,
        block: (view: View, insets: WindowInsetsCompat) -> Unit
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(targetView) { view, insetCompact ->
            block(view, insetCompact)
            return@setOnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
        }
    }

    private fun registerBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this@BaseActivity, onBackPressedCallback)
        onBackPressedCallback.isEnabled = onHandleBackPressed()
    }

    @CallSuper
    protected open fun onHandleBackPressed(onBackPressed: OnPerformBackPressed? = null): Boolean {
        this.onPerformBackPressed = onBackPressed
        return onBackPressed != null
    }

    protected open fun setupInit() = Unit

    @CallSuper
    protected open fun initAds(block: (suspend CoroutineScope.() -> Unit)? = null) {
        block?.let {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    it.invoke(this)
                }
            }
        }
    }

    abstract fun initViews(savedInstanceState: Bundle?)

    @CallSuper
    protected open fun initObservers() {
        viewModel
    }

    protected open fun initListeners() = Unit

    private fun observerNetworkState() {
        collectLatestRepeatOnLifecycle(viewModel.networkState) {
            onNetworkState(it)
        }
    }

    protected open fun onNetworkState(connected: Boolean) = Unit

    private fun observerHasPurchase() {
        // Premium removed: always report unlocked so paywall UI stays hidden.
        collectLatestRepeatOnLifecycle(viewModel.purchaseState) {
            onBillingPurchaseListener(true)
        }
    }

    protected open fun onBillingPurchaseListener(hasPurchase: Boolean) = Unit

    protected fun registerNetworkCallback() {
        connectivityManager?.runCatching {
            try {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build()
                requestNetwork(request, networkCallback)
            } catch (e: Exception) {
                registerDefaultNetworkCallback(networkCallback)
            }
        }
    }

    protected fun unregisterNetworkCallback() {
        connectivityManager?.runCatching {
            unregisterNetworkCallback(networkCallback)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        var isoLanguage = appConfigSettings.currentLanguage
        if (isoLanguage.isNullOrEmpty()) {
            isoLanguage = Resources.getSystem().configuration.locales[0].language
        }
        val newLocale = Locale(isoLanguage.toString())
        Locale.setDefault(newLocale)
        newConfig.setLocale(newLocale)
    }

    protected fun withViewModels(block: VM.() -> Unit) {
        with(viewModel, block)
    }

    protected fun withViewBindings(block: VB.() -> Unit) {
        with(viewBinding, block)
    }

    protected fun showInterAdBeforeDoAction(actionRunnable: Runnable) {
        actionRunnable.run()
    }

    @CallSuper
    override fun onStart() {
        Handler(Looper.getMainLooper()).postDelayed({
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                if (isHideSystemBars()) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }
        }, 50L)
        super.onStart()
    }

    override fun onDestroy() {
        unregisterNetworkCallback()
        super.onDestroy()
    }
}
