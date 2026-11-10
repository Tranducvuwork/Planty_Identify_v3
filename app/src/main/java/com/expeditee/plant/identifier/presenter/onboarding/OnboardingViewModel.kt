package com.expeditee.plant.identifier.presenter.onboarding

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.admob.intro.IntroAdHelper
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class OnboardingViewModel(
    private val configManagement: RemoteConfigManagement, application: Application,
) : BaseViewModel<OnboardingState>(application) {

    private val _onboardingPages = MutableStateFlow<List<OnboardingPage>>(emptyList())
    internal val onboardingPages = _onboardingPages.onStart { fetchPageRmt() }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList()
    )
    internal val onboardingPagesValue get() = onboardingPages.value

    private val _currentPage = MutableStateFlow(RecyclerView.NO_POSITION)
    internal val currentPage = _currentPage.asStateFlow()

    private fun fetchPageRmt() {
        val newUi = configManagement.enableNewOnboardingUi
        val onboardingPages = mutableListOf<OnboardingPage>(
            OnboardingPage.Content(
                placement = IntroAdHelper.NATIVE_ONBOARDING,
                onboarding = Onboarding(
                    img = if (newUi) R.drawable.bg_onboarding_1_new else R.drawable.bg_onboarding_1,
                    title = if (newUi) R.string.onboarding_1_title_new else R.string.onboarding_1_title,
                    subText = if (newUi) R.string.onboarding_1_desc_new else R.string.onboarding_1_desc,
                ),
                admobEnable = false,
            ),
            OnboardingPage.Content(
                placement = IntroAdHelper.NATIVE_ONBOARDING_2,
                onboarding = Onboarding(
                    img = if (newUi) R.drawable.bg_onboarding_2_new else R.drawable.bg_onboarding_2,
                    title = if (newUi) R.string.onboarding_2_title_new else R.string.onboarding_2_title,
                    subText = if (newUi) R.string.onboarding_2_desc_new else R.string.onboarding_2_desc,
                ),
                admobEnable = false,
            ),
            OnboardingPage.Content(
                placement = IntroAdHelper.NATIVE_ONBOARDING_LAST,
                onboarding = Onboarding(
                    img = if (newUi) R.drawable.bg_onboarding_3_new else R.drawable.bg_onboarding_4,
                    title = if (newUi) R.string.onboarding_3_title_new else R.string.onboarding_4_title,
                    subText = if (newUi) R.string.onboarding_3_desc_new else R.string.onboarding_4_desc,
                ),
                admobEnable = false,
            )
        )
        _onboardingPages.value = onboardingPages
    }

    fun setCurrentPage(index: Int) {
        _currentPage.value = index
    }

    override fun onState(state: OnboardingState) = Unit
}

sealed class OnboardingState : BaseViewModel.State
