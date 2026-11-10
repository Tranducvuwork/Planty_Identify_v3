package com.expeditee.plant.identifier.presenter.disease_list

import android.app.Application
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.PestDisease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiseaseListViewModel(
    application: Application,
    private val githubRepository: GithubRepository
) : BaseViewModel<DiseaseListState>(application) {

    private val _state = MutableStateFlow<DiseaseListUiState>(DiseaseListUiState.Loading)
    val state: StateFlow<DiseaseListUiState> = _state.asStateFlow()

    private var allDiseases: List<PestDisease> = emptyList()

    override fun onState(state: DiseaseListState) {
        when (state) {
            is DiseaseListState.LoadDiseases -> loadDiseases()
            is DiseaseListState.SearchDiseases -> filterDiseases(state.query)
        }
    }

    private fun loadDiseases() {
        emitUiState(DiseaseListUiState.Loading)
        launchBlock {
            try {
                val language = appConfigSettings.currentLanguage
                allDiseases = githubRepository.getDiseaseList(language)
                emitUiState(DiseaseListUiState.Content(allDiseases))
            } catch (exception: Exception) {
                allDiseases = emptyList()
                emitUiState(
                    DiseaseListUiState.Error(
                        message = exception.message.orEmpty(),
                        diseases = emptyList(),
                    ),
                )
            }
        }
    }

    private fun filterDiseases(query: String) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            emitUiState(DiseaseListUiState.Content(allDiseases))
            return
        }
        emitUiState(
            DiseaseListUiState.Content(
                allDiseases.filter { disease ->
                    disease.scientificName.lowercase().contains(normalized) ||
                        disease.commonName.lowercase().contains(normalized) ||
                        disease.otherName.any { it.lowercase().contains(normalized) }
                },
            ),
        )
    }

    private fun emitUiState(state: DiseaseListUiState) {
        _state.value = state
    }
}

sealed class DiseaseListState : BaseViewModel.State {
    data object LoadDiseases : DiseaseListState()
    data class SearchDiseases(val query: String) : DiseaseListState()
}

sealed class DiseaseListUiState {
    data object Loading : DiseaseListUiState()
    data class Content(val diseases: List<PestDisease>) : DiseaseListUiState()
    data class Error(val message: String, val diseases: List<PestDisease>) : DiseaseListUiState()
}
