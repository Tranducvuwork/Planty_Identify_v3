package com.expeditee.plant.identifier.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.expeditee.plant.identifier.admob.LammaAdManager
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

typealias Inflater<VB> = (LayoutInflater, ViewGroup?, Boolean) -> VB

abstract class BaseFragment<VB, VM, State>(private val inflate: Inflater<VB>) : Fragment()
        where VB : ViewDataBinding, VM : BaseViewModel<State>, State : BaseViewModel.State {

    protected val appConfigSettings by inject<AppConfigSettings>()

    protected val configManagement by inject<RemoteConfigManagement>()
    protected val adsManager: LammaAdManager by inject()

    private var _viewBinding: VB? = null
    protected val viewBinding: VB
        get() = _viewBinding!!

    abstract fun getLazyViewModel(): Lazy<VM>
    protected val viewModel: VM by this.getLazyViewModel()

    var isCreated: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupInit()
    }

    protected open fun setupInit() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflate(inflater, container, false).also { _viewBinding = it }.root.let {
        if (it.parent != null) {
            ((it.parent) as ViewGroup).removeView(it)
        }
        return@let it
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCreated = true

        initViews()
        initAds()
        initObservers()
        initListeners()

        observerDeviceConnection()
        observerHasPurchase()
    }

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

    abstract fun initViews()
    protected open fun initObservers() = Unit
    protected open fun initListeners() = Unit

    private fun observerDeviceConnection() {
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.networkState) {
            onDeviceConnectionChanged(it)
        }
    }

    protected open fun onDeviceConnectionChanged(connected: Boolean) = Unit

    private fun observerHasPurchase() {
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.purchaseState) {
            onBillingPurchaseListener(true)
        }
    }

    protected open fun onBillingPurchaseListener(hasPurchase: Boolean) = Unit

    protected fun withViewModels(block: VM.() -> Unit) {
        with(viewModel, block)
    }

    protected fun withViewBindings(block: VB.() -> Unit) {
        with(viewBinding, block)
    }

    protected fun showInterAdBeforeDoAction(actionRunnable: Runnable) {
        actionRunnable.run()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isCreated = false
        _viewBinding = null
    }
}
