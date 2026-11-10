package com.expeditee.plant.identifier.presenter.faq.tag

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.Faq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaqTagViewModel(
    application: Application,
    private val githubRepository: GithubRepository
) : BaseViewModel<FaqTagState>(application) {

    private val _faqsState = MutableStateFlow<FaqTagState>(FaqTagState.Idle)
    val faqsState = _faqsState.asStateFlow()

    fun searchFaqsByTag(tag: String) {
        viewModelScope.launch {
            _faqsState.value = FaqTagState.Loading
            val list = githubRepository.searchFaqByTag(tag, appConfigSettings.currentLanguage)
            _faqsState.value = FaqTagState.Success(list)
        }
    }

    override fun onState(state: FaqTagState) = Unit
}

sealed class FaqTagState : BaseViewModel.State {
    data object Idle : FaqTagState()
    data object Loading : FaqTagState()
    data class Success(val list: List<Faq>) : FaqTagState()
}
