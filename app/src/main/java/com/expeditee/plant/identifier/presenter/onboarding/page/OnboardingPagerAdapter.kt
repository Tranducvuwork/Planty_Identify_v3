package com.expeditee.plant.identifier.presenter.onboarding.page

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingPage

class OnboardingPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val pages: List<OnboardingPage>
) : FragmentStateAdapter(fragmentActivity) {

    val lastPageIndex: Int
        get() = pages.lastIndex

    override fun createFragment(position: Int): Fragment {
        return when (val onboardingPage = pages[position]) {
            is OnboardingPage.Content -> DefaultPageFragment.newInstance(onboardingPage)
            is OnboardingPage.FullscreenAd -> AdPageFragment.newInstance(position, onboardingPage)
        }
    }

    override fun getItemCount() = pages.size

    fun page(index: Int): OnboardingPage {
        return pages[index]
    }
}
