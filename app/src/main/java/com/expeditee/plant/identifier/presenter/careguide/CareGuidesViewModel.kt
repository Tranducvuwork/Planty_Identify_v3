package com.expeditee.plant.identifier.presenter.careguide

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

class CareGuidesViewModel(
    application: Application,
    private val remotePlantRepository: GithubRepository
) : BaseViewModel<CareGuidesState>(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val careGuides: Flow<PagingData<SuggestedPlant>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            getCareGuidesPagingFlow(query)
        }
        .cachedIn(viewModelScope)

    override fun onState(state: CareGuidesState) {
        when (state) {
            is CareGuidesState.LoadGuides -> {
                _searchQuery.value = ""
            }
            is CareGuidesState.SearchGuides -> {
                _searchQuery.value = state.query
            }
        }
    }

    private fun getCareGuidesPagingFlow(query: String): Flow<PagingData<SuggestedPlant>> {
        val language = appConfigSettings.currentLanguage
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                CareGuidesPagingSource(
                    repository = remotePlantRepository,
                    query = query,
                    language = language,
                )
            }
        ).flow
    }

    fun searchCareGuides(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}

sealed class CareGuidesState : BaseViewModel.State {
    object LoadGuides : CareGuidesState()
    data class SearchGuides(val query: String) : CareGuidesState()
}
