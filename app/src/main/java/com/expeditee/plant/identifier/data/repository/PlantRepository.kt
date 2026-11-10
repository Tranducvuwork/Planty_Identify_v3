package com.expeditee.plant.identifier.data.repository


import androidx.paging.PagingData
import com.expeditee.plant.identifier.model.PlantUIModel
import com.expeditee.plant.identifier.model.PopularPlantModel
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun getPopularPlants(query: String? = null): Flow<PagingData<PopularPlantModel>>
    suspend fun preloadAllPlants()
    suspend fun prefetchForLanguage(languageCode: String, forceRefresh: Boolean = false): Boolean
    fun commitLanguageSwitch(languageCode: String)
    fun getAllPopularPlants(): Flow<List<PopularPlantModel>>
    fun searchPlants(query: String): Flow<List<PopularPlantModel>>
    suspend fun getPlantsByCategory(category: String): List<PopularPlantModel>
    suspend fun resolvePlantForIdentify(
        scientificName: String,
        commonNames: List<String> = emptyList(),
        language: String? = null,
    ): PopularPlantModel?
    suspend fun getPopularPlantById(id: String, language: String? = null): PopularPlantModel?
    suspend fun getPlantDetailFromJsonById(id: String, language: String? = null): PlantUIModel?
    suspend fun isDataAvailable(): Boolean
}
