package com.expeditee.plant.identifier.presenter.faq

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.Faq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaqListViewModel(
    application: Application,
    private val githubRepository: GithubRepository
) : BaseViewModel<FaqListState>(application) {

    private val _faqState = MutableStateFlow<FaqListState>(FaqListState.Idle)
    val faqState = _faqState.asStateFlow()

    init {
        requestFaq("")
    }

    fun requestFaq(keyword: String) {
        viewModelScope.launch {
            _faqState.value = FaqListState.Loading
            val language = appConfigSettings.currentLanguage
            val list = if (keyword.isBlank()) {
                githubRepository.getFaqList(language)
            } else {
                githubRepository.searchFaqByKeyword(keyword, language)
            }
            _faqState.value = if (keyword.isBlank()) {
                FaqListState.FaqLoaded(list)
            } else {
                FaqListState.FaqSearched(keyword, list)
            }
        }
    }

    override fun onState(state: FaqListState) = Unit
}

sealed class FaqListState : BaseViewModel.State {
    data object Idle : FaqListState()
    data object Loading : FaqListState()
    data class FaqLoaded(val list: List<Faq>) : FaqListState()
    data class FaqSearched(
        val keyword: String,
        val list: List<Faq>
    ) : FaqListState()
}
