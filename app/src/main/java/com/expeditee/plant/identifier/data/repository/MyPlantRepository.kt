package com.expeditee.plant.identifier.data.repository

import androidx.paging.PagingData
import com.expeditee.plant.identifier.model.GardenStatisticsDto
import com.expeditee.plant.identifier.model.HealthStatisticsDto
import com.expeditee.plant.identifier.model.MyPlantModel
import kotlinx.coroutines.flow.Flow

interface MyPlantRepository {

    fun getMyPlants(type: String? = null, gardenId: String? = null): Flow<PagingData<MyPlantModel>>

    suspend fun addPlant(plant: MyPlantModel)

    suspend fun updatePlant(plant: MyPlantModel)

    suspend fun removePlant(plant: MyPlantModel)

    suspend fun getPlantDetail(id: Int): MyPlantModel?

    fun getGardenStatistics(): Flow<GardenStatisticsDto>

    fun getHealthStatistics(): Flow<HealthStatisticsDto>

    suspend fun exists(name: String, scientificName: String): Boolean
}

