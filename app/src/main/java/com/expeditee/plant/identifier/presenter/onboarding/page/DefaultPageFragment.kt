package com.expeditee.plant.identifier.presenter.onboarding.page

import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentDefaultPageBinding
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingPage
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingState

class DefaultPageFragment : BaseFragment<FragmentDefaultPageBinding, OnboardingViewModel, OnboardingState>(
    FragmentDefaultPageBinding::inflate
) {

    private val onboarding: OnboardingPage.Content? by lazy { arguments?.getParcelable(EXTRA_DATA) }

    override fun getLazyViewModel() = activityViewModel<OnboardingViewModel>()

    override fun initViews() {
        onboarding?.let { item ->
            Glide.with(this)
                .load(item.onboarding.img)
                .override(800)
                .into(viewBinding.ivThumbnail)

            viewBinding.tvTitle.text = getString(item.onboarding.title)
            viewBinding.tvSubText.text = getString(item.onboarding.subText)
        }
    }

    companion object {
        private const val EXTRA_DATA = ".EXTRA_DATA"
        fun newInstance(onboarding: OnboardingPage.Content): DefaultPageFragment {
            return DefaultPageFragment().apply {
                arguments = bundleOf(
                    EXTRA_DATA to onboarding
                )
            }
        }
    }
}
