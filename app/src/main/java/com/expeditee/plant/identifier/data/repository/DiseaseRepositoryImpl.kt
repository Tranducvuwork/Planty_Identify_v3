package com.expeditee.plant.identifier.data.repository

import android.util.Log
import com.expeditee.plant.identifier.data.RemoteDataManager
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.model.CommonDisease
import com.expeditee.plant.identifier.model.License
import com.expeditee.plant.identifier.model.SimilarImage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiseaseRepositoryImpl(
    private val remoteDataManager: RemoteDataManager,
    private val appConfigSettings: AppConfigSettings,
) : DiseaseRepository {

    private val cachedDiseases = mutableMapOf<String, List<CommonDisease>>()
    private val cachedDiseaseById = mutableMapOf<String, Map<Int, CommonDisease>>()

    companion object {
        private const val TAG = "DiseaseRepository"
        private const val DISEASE_JSON_FILE = "pest_disease_list.json"
    }

    override fun commitLanguageSwitch(languageCode: String) {
        val keepKeys = buildSet {
            add("en")
            languageCode.takeIf { it != "en" }?.let(::add)
        }
        cachedDiseases.keys.filter { it !in keepKeys }.forEach { cachedDiseases.remove(it) }
        cachedDiseaseById.keys.filter { it !in keepKeys }.forEach { cachedDiseaseById.remove(it) }
    }

    override suspend fun loadDiseases(languageOverride: String?): List<CommonDisease> = withContext(Dispatchers.IO) {
        val cacheKey = resolveCacheKey(languageOverride)
        cachedDiseases[cacheKey]?.let {
            Log.d(TAG, "Using cached diseases: ${it.size} items for language: $cacheKey")
            return@withContext it
        }

        try {
            val languageCode = resolveLanguageCode(languageOverride)
            val fileName = if (languageCode != null) {
                "pest_disease_list_$languageCode.json"
            } else {
                DISEASE_JSON_FILE
            }
            val jsonString = remoteDataManager.loadJsonData(fileName, useCache = true)
            if (jsonString != null) {
                val diseases = parseDiseaseJson(jsonString)
                if (diseases.isNotEmpty()) {
                    cacheCatalog(cacheKey, diseases)
                    Log.d(TAG, "Loaded ${diseases.size} diseases from remote JSON for language: $cacheKey")
                    return@withContext diseases
                }
            } else {
                Log.w(TAG, "No data from remote for ${fileName}")
            }

            if (languageCode != null) {
                Log.w(TAG, "No JSON data for language $languageCode, falling back to English")
                return@withContext loadDiseases("en")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load diseases: ${e.message}", e)
        }

        emptyList()
    }

    override suspend fun getDiseaseByScientificName(scientificName: String, languageOverride: String?): CommonDisease? {
        return loadDiseases(languageOverride).find { disease ->
            disease.scientificName.equals(scientificName, ignoreCase = true)
        }
    }

    override suspend fun getDiseaseById(diseaseId: Int, languageOverride: String?): CommonDisease? {
        if (diseaseId <= 0) return null

        val cacheKey = resolveCacheKey(languageOverride)
        cachedDiseaseById[cacheKey]?.get(diseaseId)?.let { return it }

        loadDiseases(languageOverride)
        return cachedDiseaseById[cacheKey]?.get(diseaseId)
    }

    override suspend fun getDiseaseByCommonName(commonName: String, languageOverride: String?): CommonDisease? {
        return loadDiseases(languageOverride).find { disease ->
            disease.commonNames?.any { it.equals(commonName, ignoreCase = true) } == true
        }
    }

    override suspend fun getOtherDiseases(currentDiseaseId: Int, limit: Int, languageOverride: String?): List<CommonDisease> {
        val allDiseases = loadDiseases(languageOverride)
        return allDiseases
            .filter { it.id != currentDiseaseId }
            .shuffled()
            .take(limit)
    }

    private fun cacheCatalog(cacheKey: String, diseases: List<CommonDisease>) {
        cachedDiseases[cacheKey] = diseases
        cachedDiseaseById[cacheKey] = diseases.mapNotNull { disease ->
            disease.id?.let { id -> id to disease }
        }.toMap()
    }

    private fun resolveCacheKey(languageOverride: String?): String {
        val effectiveLanguage = languageOverride ?: (appConfigSettings.currentLanguage ?: "en")
        return if (effectiveLanguage == "en") "en" else effectiveLanguage
    }

    private fun resolveLanguageCode(languageOverride: String?): String? {
        val effectiveLanguage = languageOverride ?: (appConfigSettings.currentLanguage ?: "en")
        return if (effectiveLanguage == "en") null else effectiveLanguage
    }

    private fun parseDiseaseJson(jsonString: String): List<CommonDisease> {
        return try {
            val gson = Gson()
            val wrapper = gson.fromJson(jsonString, DiseaseWrapper::class.java)
            wrapper.data.map { it.toCommonDisease() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse disease JSON: ${e.message}", e)
            emptyList()
        }
    }
}

private fun DiseaseJson.toCommonDisease(): CommonDisease {
    val normalizedOtherNames = when (val rawOtherName = otherName) {
        is String -> listOf(rawOtherName)
        is List<*> -> rawOtherName.filterIsInstance<String>().ifEmpty { null }
        else -> null
    }
    val allCommonNames = buildList {
        commonName.takeIf { it.isNotBlank() }?.let(::add)
        normalizedOtherNames.orEmpty().forEach(::add)
    }.ifEmpty { null }

    return CommonDisease(
        id = id,
        similarImages = images?.map { image ->
            SimilarImage(
                url = image.mediumUrl,
                urlSmall = image.smallUrl,
                license = License(
                    url = image.licenseUrl,
                    name = image.licenseName,
                ),
            )
        },
        commonNames = allCommonNames,
        scientificName = scientificName,
        otherNames = normalizedOtherNames,
        family = family,
        description = description?.joinToString("\n\n") { section ->
            "${section.subtitle}\n${section.description}"
        },
        symptoms = null,
        causes = null,
        treatment = solution?.map { it.description },
        prevention = null,
        severity = null,
        affectedPlants = host,
    )
}

data class DiseaseWrapper(val data: List<DiseaseJson>)

data class DiseaseJson(
    val id: Int,
    @SerializedName("common_name") val commonName: String,
    @SerializedName("scientific_name") val scientificName: String,
    @SerializedName("other_name") val otherName: Any?,
    val family: String?,
    val description: List<DiseaseDescription>?,
    val solution: List<DiseaseSolution>?,
    val host: List<String>?,
    val images: List<DiseaseImage>?
)

data class DiseaseDescription(val subtitle: String, val description: String)

data class DiseaseSolution(val subtitle: String, val description: String)

data class DiseaseImage(
    val license: Int,
    @SerializedName("license_name") val licenseName: String,
    @SerializedName("license_url") val licenseUrl: String,
    @SerializedName("original_url") val originalUrl: String,
    @SerializedName("regular_url") val regularUrl: String,
    @SerializedName("medium_url") val mediumUrl: String,
    @SerializedName("small_url") val smallUrl: String,
    val thumbnail: String
)
