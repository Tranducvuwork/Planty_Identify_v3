package com.expeditee.plant.identifier.presenter.home.explore

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.model.PopularPlantModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import com.expeditee.plant.identifier.R

class ExploreTabViewModel(
    application: Application,
    private val plantRepository: PlantRepository,
    private val myPlantRepository: MyPlantRepository,
) : BaseViewModel<ExploreTabState>(application) {

    private val _addPlantSuccess = MutableStateFlow(false)
    val addPlantSuccess: StateFlow<Boolean> = _addPlantSuccess.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val debouncedSearchQuery: SharedFlow<String> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1,
        )

    val popularPlants: StateFlow<List<PopularPlantModel>> = debouncedSearchQuery
        .flatMapLatest { query ->
            plantRepository.getAllPopularPlants()
                .onStart { _isLoading.value = true }
                .map { plants ->
                    val filtered = filterPlants(plants, query)
                    _isLoading.value = false
                    filtered
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    override fun onState(state: ExploreTabState) {
        when (state) {
            is ExploreTabState.Search -> searchPopularPlants(state.query)
            is ExploreTabState.ClearSearch -> clearSearch()
            is ExploreTabState.AddPlant -> addPlant(state.plant)
            ExploreTabState.ResetAddPlantSuccess -> resetAddPlantSuccess()
        }
    }

    private fun searchPopularPlants(query: String) {
        _searchQuery.value = query.trim()
    }

    private fun resetAddPlantSuccess() {
        _addPlantSuccess.value = false
    }

    private fun clearSearch() {
        _searchQuery.value = ""
    }

    private fun addPlant(plant: PopularPlantModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true

                if (myPlantRepository.exists(plant.name, plant.scientificName)) {
                    _isLoading.value = false
                    _errorEvent.emit(
                        applicationContext.getString(
                            R.string.plant_already_exists,
                            plant.name
                        )
                    )
                    return@launch
                }

                _addPlantSuccess.value = false

                val myPlant = MyPlantModel(
                    id = 0,
                    uuid = UUID.randomUUID().toString(),
                    name = plant.name,
                    scientificName = plant.scientificName,
                    type = plant.type,
                    status = "healthy",
                    timestamp = System.currentTimeMillis(),
                    imageFile = plant.imageUrl,
                    metadata = buildMetadataJson(plant),
                    gardenId = com.expeditee.plant.identifier.helper.SiteManager.DEFAULT_SITE_ID,
                    isActive = true
                )

                myPlantRepository.addPlant(myPlant)

                _addPlantSuccess.value = true
                _isLoading.value = false

            } catch (e: Exception) {
                _addPlantSuccess.value = false
                _isLoading.value = false
                e.printStackTrace()
            }
        }
    }

    private fun filterPlants(
        plants: List<PopularPlantModel>,
        query: String,
    ): List<PopularPlantModel> {
        return if (query.isBlank()) {
            plants.take(20)
        } else {
            plants.filter { plant ->
                plant.name.contains(query, ignoreCase = true) ||
                        plant.scientificName.contains(query, ignoreCase = true)
            }
        }
    }

    private fun buildMetadataJson(plant: PopularPlantModel): String {
        return """{"common_names":["${plant.name}"],"main_image":{"url":"${plant.imageUrl}"},"scientific_name":"${plant.scientificName}"}"""
    }
}

sealed class ExploreTabState : BaseViewModel.State {
    data class Search(val query: String) : ExploreTabState()
    data object ClearSearch : ExploreTabState()
    data class AddPlant(val plant: PopularPlantModel) : ExploreTabState()

    object ResetAddPlantSuccess : ExploreTabState()
}
