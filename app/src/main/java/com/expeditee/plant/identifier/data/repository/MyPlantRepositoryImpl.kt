package com.expeditee.plant.identifier.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.expeditee.plant.identifier.data.AppDatabase
import com.expeditee.plant.identifier.data.mapper.MyPlantMapper
import com.expeditee.plant.identifier.model.GardenStatisticsDto
import com.expeditee.plant.identifier.model.HealthStatisticsDto
import com.expeditee.plant.identifier.model.MyPlantModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MyPlantRepositoryImpl(
    appDatabase: AppDatabase,
    private val mapper: MyPlantMapper
) : MyPlantRepository {

    private val dao = appDatabase.myPlantDao()

    override fun getMyPlants(type: String?, gardenId: String?): Flow<PagingData<MyPlantModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (type.isNullOrBlank()) {
                    dao.getAllMyPlantsPaged(gardenId)
                } else {
                    dao.getMyPlantsByTypePaged(type, gardenId)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { mapper.mapToDomain(it) }
        }
    }

    override suspend fun addPlant(plant: MyPlantModel) {
        dao.insert(mapper.mapToEntity(plant))
    }

    override suspend fun updatePlant(plant: MyPlantModel) {
        dao.update(mapper.mapToEntity(plant))
    }

    override suspend fun removePlant(plant: MyPlantModel) {
        dao.deletePlant(mapper.mapToEntity(plant))
    }

    override suspend fun getPlantDetail(id: Int): MyPlantModel? {
        return dao.getPlantById(id)?.let { mapper.mapToDomain(it) }
    }


    override fun getGardenStatistics(): Flow<GardenStatisticsDto> {
        return combine(
            dao.getPlantCountFlow(),
            dao.getFlowerCountFlow(),
            dao.getTotalCountFlow()
        ) { plants, flowers, total ->
            GardenStatisticsDto(plants, flowers, total)
        }
    }

    override fun getHealthStatistics(): Flow<HealthStatisticsDto> {
        return dao.getHealthyPlantCountFlow().combine(dao.getUnhealthyPlantCountFlow()) { healthy, unhealthy ->
            HealthStatisticsDto(healthy, unhealthy)
        }
    }

    override suspend fun exists(name: String, scientificName: String): Boolean {
        return dao.exists(name, scientificName)
    }
}