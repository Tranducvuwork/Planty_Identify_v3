package com.expeditee.plant.identifier.presenter.home.mygardentab

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.model.GardenStats
import com.expeditee.plant.identifier.model.HealthStats
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.model.PopularPlantModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MyGardenTabViewModel(
    application: Application,
    private val myPlantRepository: MyPlantRepository
) : BaseViewModel<MyGardenTabState>(application) {

    private val _selectedType = MutableStateFlow<String>(PopularPlantModel.TYPE_PLANT)
    private val _selectedGardenId = MutableStateFlow<String?>(null) // null = All

    val selectedGardenId: StateFlow<String?> = _selectedGardenId

    val myPlants: Flow<PagingData<MyPlantModel>> =
        kotlinx.coroutines.flow.combine(_selectedType, _selectedGardenId) { type, gardenId ->
            type to gardenId
        }.flatMapLatest { (type, gardenId) ->
            myPlantRepository.getMyPlants(type, gardenId).cachedIn(viewModelScope)
        }

    val gardenStats: StateFlow<GardenStats> = myPlantRepository.getGardenStatistics()
        .map { stats ->
            GardenStats(
                totalPlants = stats.plantCount,
                totalFlowers = stats.flowerCount,
                totalCount = stats.totalCount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GardenStats(0, 0, 0)
        )

    val healthStats: StateFlow<HealthStats> = myPlantRepository.getHealthStatistics()
        .map { stats ->
            HealthStats(
                healthyCount = stats.healthyCount,
                unhealthyCount = stats.unhealthyCount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HealthStats(0, 0)
        )

    private fun deletePlant(plant: MyPlantModel) {
        launchBlock(Dispatchers.IO) {
            myPlantRepository.removePlant(plant)
        }
    }

    private fun movePlant(plant: MyPlantModel) {
        launchBlock(Dispatchers.IO) {
            val newType = when (plant.gardenId.lowercase()) {
                "flower", PopularPlantModel.TYPE_FLOWER.lowercase() -> PopularPlantModel.TYPE_FLOWER
                "plant", PopularPlantModel.TYPE_PLANT.lowercase() -> PopularPlantModel.TYPE_PLANT
                else -> plant.type
            }
            myPlantRepository.updatePlant(plant.copy(type = newType))
        }
    }

    override fun onState(state: MyGardenTabState) {
        when (state) {
            is MyGardenTabState.DeletePlant -> deletePlant(state.plant)
            is MyGardenTabState.MovePlant -> movePlant(state.plant)
            is MyGardenTabState.SelectType -> _selectedType.value = state.type
            is MyGardenTabState.SelectGarden -> _selectedGardenId.value = state.gardenId
        }
    }
}

sealed class MyGardenTabState : BaseViewModel.State {
    data class DeletePlant(val plant: MyPlantModel) : MyGardenTabState()
    data class MovePlant(val plant: MyPlantModel) : MyGardenTabState()
    data class SelectType(val type: String) : MyGardenTabState()
    data class SelectGarden(val gardenId: String?) : MyGardenTabState()
}