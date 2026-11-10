package com.expeditee.plant.identifier.presenter.onboarding.page

import android.content.Context
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.admob.NativeAdView
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentAdPageBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.parcelable
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingPage
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingState
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AdPageFragment :
    BaseFragment<FragmentAdPageBinding, OnboardingViewModel, OnboardingState>(
        inflate = { layoutInflater, viewGroup, attachToParent ->
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.fragment_ad_page,
                viewGroup,
                attachToParent
            )
        }
    ) {

    companion object {
        private const val EXTRA_POSITION = ".EXTRA_POSITION"
        private const val EXTRA_DATA = ".EXTRA_DATA"

        fun newInstance(position: Int, page: OnboardingPage.FullscreenAd): AdPageFragment {
            return AdPageFragment().apply {
                arguments = bundleOf(
                    EXTRA_POSITION to position,
                    EXTRA_DATA to page
                )
            }
        }
    }

    override fun getLazyViewModel() = activityViewModel<OnboardingViewModel>()

    private val pagePosition: Int by lazy {
        arguments?.getInt(EXTRA_POSITION) ?: RecyclerView.NO_POSITION
    }
    private val adPage: OnboardingPage.FullscreenAd? by lazy { arguments?.parcelable(EXTRA_DATA) }
    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        }
    }

    override fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { view, insets ->
            view.setPadding(0, insets.statusBars(true).top, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        refreshConsumableAd()
    }

    override fun initObservers() {
        super.initObservers()
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.currentPage) { index ->
            if (index == RecyclerView.NO_POSITION) return@collectLatestRepeatOnLifecycle
            if (index != pagePosition) {
                return@collectLatestRepeatOnLifecycle
            }
            refreshConsumableAd()
        }
    }

    private fun refreshConsumableAd() {
        listener?.onLoadNativeFull(viewBinding.nativeAdView, pagePosition)
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface Listener {
        fun onLoadNativeFull(nativeAdView: NativeAdView, pagePosition: Int)
    }
}
