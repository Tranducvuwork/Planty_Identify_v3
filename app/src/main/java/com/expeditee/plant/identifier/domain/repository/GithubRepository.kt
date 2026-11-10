package com.expeditee.plant.identifier.domain.repository

import androidx.paging.PagingData
import com.expeditee.plant.identifier.model.Faq
import com.expeditee.plant.identifier.model.PestDisease
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.reponse.CareGuidesResponse
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import kotlinx.coroutines.flow.Flow

interface GithubRepository {
    fun getPopularPlants(pageSize: Int = 20): Flow<PagingData<PopularPlantModel>>
    fun commitLanguageSwitch(languageCode: String)
    suspend fun getCareGuide(language: String? = null): CareGuidesResponse?
    suspend fun getSuggestedPlant(scientificName: String, language: String? = null): SuggestedPlant?
    suspend fun getFaqList(languageCode: String? = null): List<Faq>
    suspend fun searchFaqByKeyword(keyword: String, languageCode: String? = null): List<Faq>
    suspend fun searchFaqByTag(tag: String, languageCode: String? = null): List<Faq>
    suspend fun getDiseaseList(languageCode: String?): List<PestDisease>
}
