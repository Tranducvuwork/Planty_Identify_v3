package com.expeditee.plant.identifier.domain.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.expeditee.plant.identifier.domain.github.GithubApi
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.expeditee.plant.identifier.model.Faq
import com.expeditee.plant.identifier.model.PestDisease
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.reponse.CareGuidesResponse
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import com.expeditee.plant.identifier.model.reponse.toFaq
import com.expeditee.plant.identifier.model.reponse.toModel
import kotlinx.coroutines.flow.Flow

class GithubRepositoryImpl(
    private val remoteConfigManagement: RemoteConfigManagement,
    private val githubApi: GithubApi,
) : GithubRepository {

    private var cachedPlants: List<PopularPlantModel>? = null
    private val cachedCareGuides = mutableMapOf<String, CareGuidesResponse>()
    private val cachedFaq = mutableMapOf<String, List<Faq>>()
    private val cachedDisease = mutableMapOf<String, List<PestDisease>>()

    override fun getPopularPlants(pageSize: Int): Flow<PagingData<PopularPlantModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                initialLoadSize = 20,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                GithubPopularPagingSource(
                    remoteConfigManagement,
                    githubApi,
                    ::getCachedPlants,
                    ::setCachedPlants
                )
            }
        ).flow
    }

    override fun commitLanguageSwitch(languageCode: String) {
        val keepKeys = buildSet {
            add("en")
            languageCode.takeIf { it != "en" }?.let(::add)
        }

        cachedCareGuides.keys.filter { it !in keepKeys }.forEach { cachedCareGuides.remove(it) }
        cachedFaq.keys.filter { it !in keepKeys }.forEach { cachedFaq.remove(it) }
        cachedDisease.keys.filter { it !in keepKeys }.forEach { cachedDisease.remove(it) }
    }

    override suspend fun getCareGuide(language: String?): CareGuidesResponse? {
        val fileName = buildCareGuideFileName(language)
        val fallbackFileName = buildCareGuideFileName("en")
        return loadLocalizedResource(
            requestedFileName = fileName,
            fallbackFileName = fallbackFileName,
            cache = cachedCareGuides,
        ) { requestFileName ->
            githubApi.getCareGuide(requestFileName).body()
        }
    }

    override suspend fun getSuggestedPlant(scientificName: String, language: String?): SuggestedPlant? {
        return getCareGuide(language)?.careGuides?.find {
            it.scientificName.equals(scientificName, ignoreCase = true)
        }
    }

    private fun buildCareGuideFileName(language: String?): String {
        val normalized = language.orEmpty().trim().lowercase()
        return if (normalized.isBlank() || normalized == "en") {
            "care_guides.json"
        } else {
            "care_guides_${normalized}.json"
        }
    }

    override suspend fun getFaqList(languageCode: String?): List<Faq> {
        val fileName = buildFaqFileName(languageCode)
        val fallbackFileName = buildFaqFileName("en")
        return loadLocalizedResource(
            requestedFileName = fileName,
            fallbackFileName = fallbackFileName,
            cache = cachedFaq,
        ) { requestFileName ->
            githubApi.getFAQ(requestFileName)
                .takeIf { it.isSuccessful }
                ?.body()
                .orEmpty()
                .map { it.toFaq() }
        }.orEmpty()
    }

    private fun buildFaqFileName(languageCode: String?): String {
        val normalized = languageCode.orEmpty().trim().lowercase()
        return if (normalized.isBlank() || normalized == "en") {
            "faq_data.json"
        } else {
            "faq_data_${normalized}.json"
        }
    }

    override suspend fun searchFaqByKeyword(keyword: String, languageCode: String?): List<Faq> {
        val normalized = keyword.trim().lowercase()
        if (normalized.isBlank()) return getFaqList(languageCode)
        return getFaqList(languageCode).filter {
            it.question.lowercase().contains(normalized) ||
                    it.answer.lowercase().contains(normalized)
        }
    }

    override suspend fun searchFaqByTag(tag: String, languageCode: String?): List<Faq> {
        val normalized = tag.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return getFaqList(languageCode).filter { faq ->
            faq.tags.any { it.lowercase().contains(normalized) }
        }
    }

    override suspend fun getDiseaseList(languageCode: String?): List<PestDisease> {
        val fileName = buildDiseaseFileName(languageCode)
        val fallbackFileName = buildDiseaseFileName("en")
        return loadLocalizedResource(
            requestedFileName = fileName,
            fallbackFileName = fallbackFileName,
            cache = cachedDisease,
        ) { requestFileName ->
            githubApi.getDiseaseList(requestFileName)
                .takeIf { it.isSuccessful }
                ?.body()
                ?.data
                .orEmpty()
                .map { it.toModel() }
        }.orEmpty()
    }

    private fun buildDiseaseFileName(languageCode: String?): String {
        val normalized = languageCode.orEmpty().trim().lowercase()
        return if (normalized.isBlank() || normalized == "en") {
            "pest_disease_list.json"
        } else {
            "pest_disease_list_${normalized}.json"
        }
    }

    private fun getCachedPlants() = cachedPlants
    private fun setCachedPlants(list: List<PopularPlantModel>) {
        cachedPlants = list
    }

    private suspend fun <T> loadLocalizedResource(
        requestedFileName: String,
        fallbackFileName: String,
        cache: MutableMap<String, T>,
        loader: suspend (String) -> T?,
    ): T? {
        cache[requestedFileName]?.let { return it }
        if (requestedFileName != fallbackFileName) {
            cache[fallbackFileName]?.let { return it }
        }

        return try {
            loader(requestedFileName)?.also { loaded ->
                cache[requestedFileName] = loaded
            } ?: if (requestedFileName != fallbackFileName) {
                loader(fallbackFileName)?.also { fallback ->
                    cache[fallbackFileName] = fallback
                }
            } else {
                null
            }
        } catch (_: Exception) {
            if (requestedFileName == fallbackFileName) {
                null
            } else {
                cache[fallbackFileName] ?: runCatching { loader(fallbackFileName) }.getOrNull()?.also { fallback ->
                    cache[fallbackFileName] = fallback
                }
            }
        }
    }
}
