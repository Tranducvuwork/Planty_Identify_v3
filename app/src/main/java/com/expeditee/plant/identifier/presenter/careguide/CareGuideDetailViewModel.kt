package com.expeditee.plant.identifier.presenter.careguide

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import kotlinx.coroutines.launch

class CareGuideDetailViewModel(
    application: Application,
    private val githubRepository: GithubRepository
) : BaseViewModel<CareGuideDetailState>(application) {

    private val _plantData = MutableLiveData<SuggestedPlant?>()
    val plantData: LiveData<SuggestedPlant?> = _plantData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    override fun onState(state: CareGuideDetailState) {
        when (state) {
            is CareGuideDetailState.LoadPlantDetails -> loadPlantByScientificName(state.scientificName)
        }
    }

    private fun loadPlantByScientificName(scientificName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _plantData.value = githubRepository.getSuggestedPlant(scientificName, appConfigSettings.currentLanguage)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

sealed class CareGuideDetailState : BaseViewModel.State {
    data class LoadPlantDetails(val scientificName: String) : CareGuideDetailState()
}
