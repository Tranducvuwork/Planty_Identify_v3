package com.expeditee.plant.identifier.data.repository

import com.expeditee.plant.identifier.model.CommonDisease

interface DiseaseRepository {
    fun commitLanguageSwitch(languageCode: String)
    suspend fun loadDiseases(languageOverride: String? = null): List<CommonDisease>
    suspend fun getDiseaseById(diseaseId: Int, languageOverride: String? = null): CommonDisease?
    suspend fun getDiseaseByScientificName(scientificName: String, languageOverride: String? = null): CommonDisease?
    suspend fun getDiseaseByCommonName(commonName: String, languageOverride: String? = null): CommonDisease?
    suspend fun getOtherDiseases(currentDiseaseId: Int, limit: Int = 10, languageOverride: String? = null): List<CommonDisease>
}
