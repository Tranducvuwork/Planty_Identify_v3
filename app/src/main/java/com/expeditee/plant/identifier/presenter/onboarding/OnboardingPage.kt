package com.expeditee.plant.identifier.presenter.onboarding

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class OnboardingPage(open val placement: String) : Parcelable {

    @Parcelize
    data class Content(
        override val placement: String,
        val onboarding: Onboarding,
        val admobEnable: Boolean
    ) : OnboardingPage(placement)

    @Parcelize
    data class FullscreenAd(override val placement: String) : OnboardingPage(placement)
}
