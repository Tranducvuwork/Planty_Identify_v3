package com.expeditee.plant.identifier.presenter.home

import android.app.Application
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers

class HomeViewModel(
    application: Application,
    private val myPlantRepository: MyPlantRepository,
    private val plantRepository: PlantRepository,
) : BaseViewModel<HomeState>(application) {

    override fun onState(state: HomeState) {
        when (state) {
            is HomeState.PreloadPlants -> preloadPlants()
        }
    }

    private fun preloadPlants() {
        launchBlock(Dispatchers.IO) {
            plantRepository.preloadAllPlants()
        }
    }
}

sealed class HomeState : BaseViewModel.State {
    data object PreloadPlants : HomeState()
}