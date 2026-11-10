package com.expeditee.plant.identifier.presenter.home.search

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.helper.SearchHistoryManager
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.model.PlantCategory
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.SearchHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SearchViewModel(
    application: Application,
    private val plantRepository: PlantRepository,
    private val myPlantRepository: MyPlantRepository,
) : BaseViewModel<SearchState>(application) {

    private val searchHistoryManager = SearchHistoryManager(application)

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _addPlantSuccess = MutableStateFlow(false)
    val addPlantSuccess: StateFlow<Boolean> = _addPlantSuccess.asStateFlow()
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()
    val currentQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryModel>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryModel>> = _searchHistory.asStateFlow()

    val searchResults: StateFlow<List<PopularPlantModel>> =
        _searchQuery.onStart { emit("") }.flatMapLatest { query ->
            _isLoading.value = true

            val result = if (query.isBlank()) {
                loadSearchHistory()

                plantRepository.getAllPopularPlants().map { plants -> plants.take(10) }
                    .onEach { _ -> _isLoading.value = false }.catch { exception ->
                        _isLoading.value = false
                        emit(emptyList())
                    }
            } else {
                // Hide history when searching
                clearSearchHistory()

                val category = PlantCategory.fromQuery(query)
                val resultFlow = if (category != null) {
                    flow {
                        val categoryResults = plantRepository.getPlantsByCategory(category.name)
                        emit(categoryResults.take(10))
                        if (categoryResults.size > 10) emit(categoryResults)
                    }
                } else {
                    plantRepository.searchPlants(query)
                        .flatMapLatest { fastResults ->
                            flow {
                                if (fastResults.isNotEmpty()) {
                                    emit(fastResults.take(10))
                                    if (fastResults.size > 10) emit(fastResults)
                                } else {
                                    val categoryResults = plantRepository.getPlantsByCategory(query)
                                    emit(categoryResults.take(10))
                                    if (categoryResults.size > 10) emit(categoryResults)
                                }
                            }
                        }
                }
                resultFlow
                    .distinctUntilChanged()
                    .onEach { _isLoading.value = false }
                    .catch {
                        _isLoading.value = false
                        emit(emptyList())
                    }
            }

            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    override fun onState(state: SearchState) {
        when (state) {
            is SearchState.Search -> {
                val query = state.query.trim()
                _searchQuery.value = query
                // Save to history
                if (query.isNotEmpty()) {
                    searchHistoryManager.addSearchToHistory(query)
                }
            }

            is SearchState.ClearSearch -> {
                _searchQuery.value = ""
                loadSearchHistory()
            }

            is SearchState.AddPlant -> addPlant(state.plant, state.gardenId)
            is SearchState.DeleteHistory -> deleteHistory(state.historyModel)
            is SearchState.ClearAllHistory -> clearAllHistory()
        }
    }


    private fun addPlant(plant: PopularPlantModel, gardenId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                if (myPlantRepository.exists(plant.name, plant.scientificName)) {
                    _isLoading.value = false
                    _errorEvent.emit(
                        applicationContext.getString(
                            com.expeditee.plant.identifier.R.string.plant_already_exists,
                            plant.name
                        )
                    )
                    return@launch
                }
                _addPlantSuccess.value = false

                myPlantRepository.addPlant(
                    MyPlantModel(
                        id = 0,
                        uuid = UUID.randomUUID().toString(),
                        name = plant.name,
                        scientificName = plant.scientificName,
                        type = plant.type,
                        status = "healthy",
                        timestamp = System.currentTimeMillis(),
                        imageFile = plant.imageUrl,
                        metadata = buildMetadataJson(plant),
                        gardenId = gardenId,
                        isActive = true
                    )
                )
                _addPlantSuccess.value = true
                _isLoading.value = false

            } catch (e: Exception) {
                e.printStackTrace()

                _addPlantSuccess.value = false
                _isLoading.value = false
            }
        }
    }

    private fun loadSearchHistory() {
        launchBlock(Dispatchers.IO) {
            val history = searchHistoryManager.getSearchHistory()
            _searchHistory.emit(history.take(4))
        }
    }

    private fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    private fun deleteHistory(historyModel: SearchHistoryModel) {
        launchBlock(Dispatchers.IO) {
            searchHistoryManager.deleteHistory(historyModel)
            val updatedHistory = searchHistoryManager.getSearchHistory()
            _searchHistory.emit(updatedHistory.take(4))
        }
    }

    private fun clearAllHistory() {
        launchBlock(Dispatchers.IO) {
            searchHistoryManager.clearAllHistory()
            _searchHistory.emit(emptyList())
        }
    }

    private fun buildMetadataJson(plant: PopularPlantModel): String {
        return """{"common_names":["${plant.name}"],"main_image":{"url":"${plant.imageUrl}"},"scientific_name":"${plant.scientificName}"}"""
    }
}

sealed class SearchState : BaseViewModel.State {
    data class Search(val query: String) : SearchState()
    data object ClearSearch : SearchState()
    data class AddPlant(val plant: PopularPlantModel, val gardenId: String) : SearchState()
    data class DeleteHistory(val historyModel: SearchHistoryModel) : SearchState()
    data object ClearAllHistory : SearchState()
}
