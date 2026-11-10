package com.expeditee.plant.identifier.services

import com.expeditee.plant.identifier.data.RemoteDataManager
import com.expeditee.plant.identifier.data.repository.DiseaseRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LanguageDataPrefetchService(
    private val plantRepository: PlantRepository,
    private val githubRepository: GithubRepository,
    private val diseaseRepository: DiseaseRepository,
    private val remoteDataManager: RemoteDataManager,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefetchMutex = Mutex()

    fun schedulePrefetch(languageCode: String, forceRefresh: Boolean = true) {
        scope.launch {
            runCatching { prefetchAndCommitLanguage(languageCode, forceRefresh) }
        }
    }

    fun scheduleStartupPrefetch(languageCode: String) {
        schedulePrefetch(languageCode, forceRefresh = false)
    }

    suspend fun prefetchAndCommitLanguage(
        languageCode: String,
        forceRefresh: Boolean,
    ): Boolean = prefetchMutex.withLock {
        if (forceRefresh) {
            invalidateLanguageDiskCache(languageCode)
        }

        val languageOverride = languageCode.takeIf { it != "en" }
        val plantReady = plantRepository.prefetchForLanguage(languageCode, forceRefresh)
        if (!plantReady) return@withLock false

        val careGuide = githubRepository.getCareGuide(languageOverride)
        val faqList = githubRepository.getFaqList(languageOverride)
        val diseaseCatalog = githubRepository.getDiseaseList(languageOverride)
        val diseaseDetails = diseaseRepository.loadDiseases(languageOverride)

        val isReady = careGuide != null &&
            faqList.isNotEmpty() &&
            diseaseCatalog.isNotEmpty() &&
            diseaseDetails.isNotEmpty()
        if (!isReady) return@withLock false

        plantRepository.commitLanguageSwitch(languageCode)
        githubRepository.commitLanguageSwitch(languageCode)
        diseaseRepository.commitLanguageSwitch(languageCode)
        remoteDataManager.clearMultilingualCacheExcept(languageCode)
        true
    }

    private fun invalidateLanguageDiskCache(languageCode: String) {
        if (languageCode == "en") return

        remoteDataManager.invalidateCacheFile("detailed_plants_$languageCode.json")
        remoteDataManager.invalidateCacheFile("detailed_plants_light_$languageCode.json")
        remoteDataManager.invalidateCacheFile("pest_disease_list_$languageCode.json")
        remoteDataManager.invalidateCacheFile("faq_data_$languageCode.json")
        remoteDataManager.invalidateCacheFile("care_guides_$languageCode.json")
    }
}
