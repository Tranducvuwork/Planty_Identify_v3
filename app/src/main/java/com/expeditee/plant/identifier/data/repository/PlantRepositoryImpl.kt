package com.expeditee.plant.identifier.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.expeditee.plant.identifier.data.AppDatabase
import com.expeditee.plant.identifier.data.RemoteDataManager
import com.expeditee.plant.identifier.data.mapper.PlantMapper
import com.expeditee.plant.identifier.domain.github.GithubApi
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.model.BasicSection
import com.expeditee.plant.identifier.model.CareSection
import com.expeditee.plant.identifier.model.ClassificationSection
import com.expeditee.plant.identifier.model.EnvironmentSection
import com.expeditee.plant.identifier.model.ImagesSection
import com.expeditee.plant.identifier.model.PhysicalSection
import com.expeditee.plant.identifier.model.PlantUIModel
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.reponse.PlantDetailResponse
import com.expeditee.plant.identifier.model.reponse.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.util.Log
import com.expeditee.plant.identifier.data.entity.PopularPlantFtsEntity
import com.expeditee.plant.identifier.data.entity.PopularPlantEntity
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.expeditee.plant.identifier.model.PlantCategory

class PlantRepositoryImpl(
    appDatabase: AppDatabase,
    private val githubApi: GithubApi,
    private val mapper: PlantMapper,
    private val remoteDataManager: RemoteDataManager,
    private val remoteConfig: RemoteConfigManagement,
    private val appConfigSettings: AppConfigSettings,
) : PlantRepository {
    companion object {
        private const val PLANT_IMAGE_FOLDER = "PlantImage"
        private const val TAG = "PlantRepositoryImpl"
    }

    private val dao = appDatabase.popularPlantDao()
    private val gson = Gson()
    private val detailCache = mutableMapOf<String, PlantUIModel>()
    private val categorySearchIndex = mutableMapOf<Int, String>()
    private val cachedLanguageCatalogs = mutableMapOf<String, List<PlantResponse>>()
    private val cachedLanguageCatalogIndex = mutableMapOf<String, Map<Int, PlantResponse>>()
    private val cachedLanguageDetailCatalogs = mutableMapOf<String, List<PlantDetailResponse>>()
    private val cachedLanguageDetailCatalogIndex = mutableMapOf<String, Map<Int, PlantDetailResponse>>()
    private val preloadMutex = Mutex()

    // Fetch popular plants with pagination (optionally by query)
    override fun getPopularPlants(query: String?): Flow<PagingData<PopularPlantModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                initialLoadSize = 20,
                maxSize = 20,
                enablePlaceholders = false
            ), pagingSourceFactory = {
                if (query.isNullOrBlank()) {
                    dao.getAllPlantsPaged()
                } else {
                    dao.getPlantsPagedByQuery(query)
                }
            }).flow.map { pagingData ->
            pagingData.map { entity ->
                mapper.mapToDomain(entity)
            }
        }.catch { exception ->
            Log.e(TAG, "Error in getPopularPlants paging", exception)
        }
    }

    // Fetch all plants from API and save to DB if not available
    override suspend fun preloadAllPlants() {
        preloadMutex.withLock {
            if (remoteConfig.baseURLInvalid){
                return
            }
            val existingCount = dao.getCount()
            if (existingCount > 0) {
                Log.d(TAG, "DB success $existingCount plants, skip preload")
                return
            }
            try {
                val response = githubApi.getDetailedPlants()
                if (response.isSuccessful) {
                    val plantList = response.body() ?: emptyList()
                    if (plantList.isNotEmpty()) {
                        val entities = mapper.mapResponseListToEntityList(plantList).distinctBy { it.id }

                        val ftsEntities = entities.map { entity ->
                            PopularPlantFtsEntity(
                                rowid = entity.id,
                                commonNames = entity.commonNames.joinToString(" "),
                                scientificName = entity.scientificName
                            )
                        }

                        dao.clearAll()
                        dao.clearAllFts()
                        dao.insertAll(entities)
                        dao.insertAllFts(ftsEntities)

                        Log.d(TAG, "Preloaded ${entities.size} plants successfully")
                    }
                } else {
                    Log.w(TAG, "Failed to fetch plants: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during preloadAllPlants", e)
            }
        }
    }

    override suspend fun prefetchForLanguage(languageCode: String, forceRefresh: Boolean): Boolean {
        preloadAllPlants()
        if (languageCode == "en") return isDataAvailable()

        val detailLoaded = loadLanguageDetailCatalog(languageCode, forceRefresh)
        val lightweightLoaded = loadLightweightLanguageCatalog(languageCode, forceRefresh)
        return isDataAvailable() && (detailLoaded.isNotEmpty() || lightweightLoaded.isNotEmpty())
    }

    override fun commitLanguageSwitch(languageCode: String) {
        val keepLanguage = languageCode.takeIf { it != "en" }

        detailCache.keys.removeAll { key ->
            if (keepLanguage == null) {
                key.contains("_")
            } else {
                key.contains("_") && !key.endsWith("_$keepLanguage")
            }
        }

        cachedLanguageCatalogs.keys
            .filter { keepLanguage == null || it != keepLanguage }
            .forEach { cachedLanguageCatalogs.remove(it) }
        cachedLanguageCatalogIndex.keys
            .filter { keepLanguage == null || it != keepLanguage }
            .forEach { cachedLanguageCatalogIndex.remove(it) }
        cachedLanguageDetailCatalogs.keys
            .filter { keepLanguage == null || it != keepLanguage }
            .forEach { cachedLanguageDetailCatalogs.remove(it) }
        cachedLanguageDetailCatalogIndex.keys
            .filter { keepLanguage == null || it != keepLanguage }
            .forEach { cachedLanguageDetailCatalogIndex.remove(it) }
    }

    // Get all popular plants from DB (no pagination)
    override fun getAllPopularPlants(): Flow<List<PopularPlantModel>> {
        return dao.getAllPlants().map { entities ->
                Log.d(TAG, "getAllPopularPlants loaded ${entities.size} items into memory")
                entities.map { mapper.mapToDomain(it) }
            }.catch { exception ->
                Log.e(TAG, "Error loading all plants", exception)
                emit(emptyList())
            }
    }


    // Search plants by name (query)
    override fun searchPlants(query: String): Flow<List<PopularPlantModel>> = flow {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            val popular = dao.getPopularPlantsLimited(10).first().map { mapper.mapToDomain(it) }
            Log.d(TAG, "searchPlants returning ${popular.size} popular plants (empty query)")
            emit(popular)
            return@flow
        }

        val ftsQuery = sanitizeFtsQuery(trimmedQuery)
        val fts = if (ftsQuery.isBlank()) {
            emptyList()
        } else {
            runCatching { dao.searchPlants(ftsQuery).first() }.getOrDefault(emptyList())
        }
        var mapped = fts.map { mapper.mapToDomain(it) }
        if (mapped.isEmpty()) {
            mapped = dao.searchPlantsLikeOnce(trimmedQuery).map { mapper.mapToDomain(it) }
        }
        if (mapped.isEmpty()) {
            mapped = searchLocalizedNames(trimmedQuery)
        }
        Log.d(TAG, "searchPlants found ${mapped.size} results for query='$trimmedQuery'")
        emit(mapped)
    }.catch { exception ->
        Log.e(TAG, "Error searching plants with query='$query'", exception)
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    override suspend fun getPlantsByCategory(category: String): List<PopularPlantModel> =
        withContext(Dispatchers.IO) {
            val resolved = PlantCategory.fromQuery(category)
            val normalizedCategory = normalizeKeyword(resolved?.name ?: category)
            if (normalizedCategory.isBlank()) return@withContext emptyList()

            try {
                val plants = dao.getAllPlantsNoLimitOnce()
                val categoryTokens = buildCategoryTokens(resolved, normalizedCategory)
                val matched = mutableListOf<PopularPlantModel>()

                plants.forEach { entity ->
                    val searchable = buildSearchableText(entity)
                    val isMatched = categoryTokens.any { token ->
                        searchable.contains(token)
                    }
                    if (isMatched) {
                        matched += mapper.mapToDomain(entity)
                    }
                }
                matched
            } catch (e: Exception) {
                Log.e(TAG, "getPlantsByCategory failed for '$category'", e)
                emptyList()
            }
        }

    // Find plant by scientific or common name (for identification)
    override suspend fun resolvePlantForIdentify(
        scientificName: String,
        commonNames: List<String>,
        language: String?,
    ): PopularPlantModel? {
        val lang = language?.takeIf { it.isNotBlank() }
            ?: appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }

        val normalizedScientificName = normalizeKeyword(scientificName)
        var matchedId: Int? = null

        if (normalizedScientificName.isNotBlank()) {
            val exact = dao.findByScientificNameExact(normalizedScientificName)
            if (exact != null) {
                matchedId = exact.id
            } else {
                val genus = normalizedScientificName.substringBefore(" ").trim()
                if (genus.isNotBlank()) {
                    val byGenus = dao.findByGenusPrefix(genus)
                    if (byGenus != null) matchedId = byGenus.id
                }
            }
        }

        if (matchedId == null) {
            for (name in commonNames.asSequence().map(::normalizeKeyword).filter { it.isNotBlank() }) {
                val byCommon = dao.findByCommonNameKeyword(name)
                if (byCommon != null) {
                    matchedId = byCommon.id
                    break
                }
            }
        }

        val id = matchedId ?: return null
        val base = dao.findById(id)?.let { mapper.mapToDomain(it) } ?: return null

        if (lang.isNullOrBlank() || lang == "en") return base

        return tryLoadLocalized(base, id, lang)
    }

    private suspend fun tryLoadLocalized(
        base: PopularPlantModel,
        plantId: Int,
        lang: String,
    ): PopularPlantModel {
        val lightweightCatalog = loadLightweightLanguageCatalog(lang)
        lightweightCatalog[plantId]?.commonNames?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { localizedName ->
            return base.copy(name = localizedName)
        }

        loadLanguageDetailCatalog(lang)
        val cached = cachedLanguageDetailCatalogIndex[lang]?.get(plantId)
        if (cached != null) {
            val localizedName = cached.commonNames?.firstOrNull()?.takeIf { it.isNotBlank() }
            if (localizedName != null) return base.copy(name = localizedName)
            return base
        }

        val jsonString = remoteDataManager.loadJsonData("json-$lang/$plantId.json")
        if (jsonString != null) {
            val detail = runCatching { gson.fromJson(jsonString, PlantDetailResponse::class.java) }.getOrNull()
            if (detail != null) {
                val localizedName = detail.commonNames?.firstOrNull()?.takeIf { it.isNotBlank() }
                if (localizedName != null) return base.copy(name = localizedName)
            }
        }

        return base
    }

    // Get popular plant by id
    override suspend fun getPopularPlantById(id: String, language: String?): PopularPlantModel? {
        val parsedId = id.toIntOrNull() ?: return null
        val lang = language?.takeIf { it.isNotBlank() }
            ?: appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }
        val base = dao.findById(parsedId)?.let { mapper.mapToDomain(it) } ?: return null
        if (lang.isNullOrBlank() || lang == "en") return base
        return tryLoadLocalized(base, parsedId, lang)
    }

    // Get plant detail from JSON file by id
    override suspend fun getPlantDetailFromJsonById(id: String, language: String?): PlantUIModel? {
        val parsedId = id.toIntOrNull() ?: return null
        val lang = language?.takeIf { it.isNotBlank() }
            ?: appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }
        val cacheKey = if (lang != null && lang != "en") "${parsedId}_$lang" else parsedId.toString()
        detailCache["detail_$cacheKey"]?.let {
            return it
        }

        val detail = if (lang != null && lang != "en") {
            loadLanguageDetailCatalog(lang)
            cachedLanguageDetailCatalogIndex[lang]?.get(parsedId)
                ?: loadPlantDetailFromFile(parsedId, lang)
        } else {
            loadPlantDetailFromFile(parsedId)
        } ?: return null

        val mapped = mapDetailToUiModel(parsedId, detail)
        detailCache["detail_$cacheKey"] = mapped
        return mapped
    }

    private suspend fun loadPlantDetailFromFile(plantId: Int, lang: String? = null): PlantDetailResponse? {
        val fileName = if (lang != null && lang != "en") "json-$lang/$plantId.json" else "json/$plantId.json"
        val jsonString = remoteDataManager.loadJsonData(fileName)
            ?: if (lang != null && lang != "en") remoteDataManager.loadJsonData("json/$plantId.json") else null
            ?: return null
        return runCatching { gson.fromJson(jsonString, PlantDetailResponse::class.java) }.getOrNull()
    }

    // Check if DB has enough plant data
    override suspend fun isDataAvailable(): Boolean {
        return dao.getCount() > 500
    }

    private fun mapDetailToUiModel(
        id: Int,
        detail: PlantDetailResponse,
    ): PlantUIModel {
        val details = detail.details
        val cultivation = details?.growthCultivation
        val description = details?.description ?: detail.description
        val photoLibraryUrls = buildPhotoLibraryUrls(detail)
        val imageUrl = normalizeImageUrl(detail.mainImage?.url)
            ?: normalizeImageUrl(detail.mainImage?.urlSmall) ?: photoLibraryUrls.firstOrNull()

        // Hardiness zone formatting
        val hardinessInfo = details?.hardiness ?: detail.hardiness
        val hardyZone = formatHardinessZone(hardinessInfo?.min, hardinessInfo?.max)

        // Dimension parsing
        val dimInfo = details?.dimensions
        val heightMax = dimInfo?.max ?: (details?.height?.toDoubleOrNull())
        val heightMin = dimInfo?.min ?: heightMax

        // Sunlight types and guide
        val sunlight = cultivation?.sunlight
        val sunlightTypes =
            sunlight?.type ?: (cultivation?.lightRequirements?.split(",")?.map { it.trim() })

        // Pruning months (distinct to remove duplicates)
        val pruningMonthsUnique = cultivation?.pruning?.months?.distinct()

        return PlantUIModel(
            basic = BasicSection(
                id = id.toString(),
                commonName = detail.commonNames?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: "Unknown",
                scientificName = detail.scientificName?.takeIf { it.isNotBlank() } ?: "Unknown",
                description = description,
                commonNames = detail.commonNames,
            ),
            classification = ClassificationSection(
                family = details?.family,
                genus = details?.genus,
                species = details?.species,
                plantType = details?.type,
                origin = details?.origin,
                isTropical = cultivation?.isTropical ?: false,
                isIndoor = cultivation?.isIndoor ?: false,
            ),
            physical = PhysicalSection(
                height = details?.height,
                heightMin = heightMin,
                heightMax = heightMax,
                heightUnit = dimInfo?.unit,
                dimensionsDescription = dimInfo?.description,
                spread = details?.spread,
                bloomTime = details?.bloomTime,
            ),
            care = CareSection(
                careLevel = cultivation?.careLevel,
                growthRate = cultivation?.growthRate,
                cycle = cultivation?.cycle,
                maintenance = cultivation?.maintenance,
                propagation = cultivation?.propagation,
                watering = cultivation?.watering?.frequency,
                wateringGuide = cultivation?.watering?.guide,
                pruningMonths = pruningMonthsUnique,
                pruningGuide = cultivation?.pruning?.guide,
                hardiness = hardyZone,
                hardinessMin = hardinessInfo?.min,
                hardinessMax = hardinessInfo?.max,
            ),
            environment = EnvironmentSection(
                lightNeeds = cultivation?.lightRequirements,
                sunlightTypes = sunlightTypes,
                sunlightGuide = sunlight?.guide,
                temperature = cultivation?.temperatureRange,
                humidity = formatHumidity(cultivation?.humidity?.min, cultivation?.humidity?.max),
                humidityMin = cultivation?.humidity?.min,
                humidityMax = cultivation?.humidity?.max,
                soil = cultivation?.soil,
                attracts = cultivation?.attracts,
                droughtTolerant = cultivation?.tolerant?.drought ?: false,
                saltTolerant = cultivation?.tolerant?.salt ?: false,
            ),
            images = ImagesSection(
                mainImage = imageUrl,
                gallery = photoLibraryUrls,
            ),
        )
    }

    private fun buildPhotoLibraryUrls(detail: PlantDetailResponse): List<String> {
        val urls = mutableListOf<String>()

        normalizeImageUrl(detail.mainImage?.url)?.let { urls.add(it) }
        normalizeImageUrl(detail.mainImage?.urlSmall)?.let { urls.add(it) }

        detail.similarImages.orEmpty().forEach { image ->
            normalizeImageUrl(image.url)?.let { urls.add(it) }
            normalizeImageUrl(image.urlSmall)?.let { urls.add(it) }
        }

        return urls.distinct()
    }

    private fun formatHumidity(min: Int?, max: Int?): String? {
        return when {
            min != null && max != null -> "$min-$max%"
            min != null -> "Min $min%"
            max != null -> "Max $max%"
            else -> null
        }
    }

    private fun formatHardinessZone(min: Int?, max: Int?): String? {
        return when {
            min != null && max != null && min != max -> "Zone $min-$max"
            min != null -> "Zone $min"
            max != null -> "Zone $max"
            else -> null
        }
    }

    private fun normalizeImageUrl(rawUrl: String?): String? {
        val cleaned = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else {
            val normalizedRelativePath = when {
                cleaned.startsWith("$PLANT_IMAGE_FOLDER/") -> cleaned
                cleaned.contains("/") -> cleaned
                else -> "$PLANT_IMAGE_FOLDER/$cleaned"
            }
            "${remoteConfig.baseUrlData.trimEnd('/')}/${normalizedRelativePath.trimStart('/')}"
        }
    }

    private suspend fun loadLanguageDetailCatalog(
        lang: String,
        forceRefresh: Boolean = false,
    ): List<PlantDetailResponse> {
        if (lang == "en") return emptyList()
        if (forceRefresh) {
            cachedLanguageDetailCatalogs.remove(lang)
            cachedLanguageDetailCatalogIndex.remove(lang)
        } else {
            cachedLanguageDetailCatalogs[lang]?.let { return it }
        }
        val jsonString = loadFirstAvailableJson(
            "detailed_plants_$lang.json",
            useCache = !forceRefresh,
        )
        if (jsonString != null) {
            runCatching {
                val type = object : TypeToken<List<PlantDetailResponse>>() {}.type
                val plants = gson.fromJson<List<PlantDetailResponse>>(jsonString, type).orEmpty()
                if (plants.isNotEmpty()) {
                    cachedLanguageDetailCatalogs[lang] = plants
                    cachedLanguageDetailCatalogIndex[lang] = plants.mapNotNull { it.id?.let { id -> id to it } }.toMap()
                }
            }
        }
        return cachedLanguageDetailCatalogs[lang].orEmpty()
    }

    private suspend fun loadLightweightLanguageCatalog(
        lang: String,
        forceRefresh: Boolean = false,
    ): Map<Int, PlantResponse> {
        if (lang == "en") return emptyMap()
        if (forceRefresh) {
            cachedLanguageCatalogs.remove(lang)
            cachedLanguageCatalogIndex.remove(lang)
        } else {
            cachedLanguageCatalogIndex[lang]?.let { return it }
        }
        val jsonString = loadFirstAvailableJson(
            "detailed_plants_light_$lang.json",
            "detailed_plants_$lang.json",
            useCache = !forceRefresh,
        )
        if (jsonString != null) {
            runCatching {
                val type = object : TypeToken<List<PlantResponse>>() {}.type
                val plants = gson.fromJson<List<PlantResponse>>(jsonString, type).orEmpty()
                if (plants.isNotEmpty()) {
                    cachedLanguageCatalogs[lang] = plants
                    cachedLanguageCatalogIndex[lang] = plants.associateBy { it.id }
                }
            }
        }
        return cachedLanguageCatalogIndex[lang].orEmpty()
    }

    private suspend fun loadFirstAvailableJson(vararg fileNames: String, useCache: Boolean = true): String? {
        fileNames.forEach { fileName ->
            remoteDataManager.loadJsonData(fileName, useCache = useCache)?.let { return it }
        }
        return null
    }

    private fun normalizeKeyword(keyword: String): String {
        return keyword.trim().replace(Regex("\\s+"), " ").lowercase()
    }

    private fun buildCategoryTokens(
        category: PlantCategory?,
        normalizedCategory: String,
    ): Set<String> {
        val key = when (category) {
            PlantCategory.FLOWERS -> "flower"
            PlantCategory.FRUIT -> "fruit"
            PlantCategory.VEGETABLES -> "vegetable"
            PlantCategory.CREEPERS -> "creeper"
            PlantCategory.TREES -> "tree"
            PlantCategory.WEEDS -> "weed"
            PlantCategory.TOXIC -> "toxic"
            PlantCategory.SHRUBS -> "shrub"
            PlantCategory.HERBS -> "herb"
            null -> singularize(normalizedCategory)
        }
        val synonyms = when (key) {
            "flower" -> setOf("blossom", "bloom", "floral", "flowering")
            "fruit" -> setOf("fruits", "berry", "berries", "citrus", "apple", "orange")
            "vegetable" -> setOf("vegetables", "veggie", "veggies", "eggplant", "aubergine")
            "creeper" -> setOf("creepers", "vine", "ivy", "climber", "climbing")
            "tree" -> setOf("trees", "woody", "oak", "maple", "pine")
            "weed" -> setOf("weeds", "dandelion")
            "toxic" -> setOf("poison", "poisonous", "oleander", "dieffenbachia")
            "shrub" -> setOf("shrubs", "bush", "bushes")
            "herb" -> setOf("herbs", "mint", "basil", "rosemary")
            else -> emptySet()
        }
        return setOf(normalizedCategory, key, "${key}s") + synonyms
    }

    private fun sanitizeFtsQuery(query: String): String {
        return query.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private suspend fun searchLocalizedNames(query: String): List<PopularPlantModel> {
        val lang = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() && it != "en" }
            ?: return emptyList()
        val needle = PlantCategory.fold(query)
        if (needle.length < 2) return emptyList()

        val ids = linkedSetOf<Int>()
        loadLightweightLanguageCatalog(lang).values.forEach { plant ->
            val names = plant.commonNames.orEmpty() + listOfNotNull(plant.scientificName)
            if (names.any { PlantCategory.fold(it).contains(needle) }) {
                ids += plant.id
            }
        }
        if (ids.isEmpty()) {
            loadLanguageDetailCatalog(lang).forEach { plant ->
                val id = plant.id ?: return@forEach
                val names = plant.commonNames.orEmpty() + listOfNotNull(plant.scientificName)
                if (names.any { PlantCategory.fold(it).contains(needle) }) {
                    ids += id
                }
            }
        }
        return ids.mapNotNull { id ->
            val base = dao.findById(id)?.let { mapper.mapToDomain(it) } ?: return@mapNotNull null
            tryLoadLocalized(base, id, lang)
        }
    }

    private fun buildSearchableText(entity: PopularPlantEntity): String {
        val cached = categorySearchIndex[entity.id]
        if (cached != null) return cached

        val combined = listOf(
            entity.commonNames.joinToString(" "),
            entity.scientificName.orEmpty(),
            entity.mainImageUrl.orEmpty().substringAfterLast("/").substringBeforeLast(".")
                .replace("-", " ").replace("_", " "),
            entity.mainImageUrl.orEmpty()
        ).joinToString(" ").replace(Regex("\\s+"), " ").lowercase()

        categorySearchIndex[entity.id] = combined
        return combined
    }

    private fun singularize(value: String): String {
        return when {
            value.length > 4 && value.endsWith("ies") -> value.dropLast(3) + "y"
            value.length > 4 && value.endsWith("es") -> value.dropLast(2)
            value.length > 3 && value.endsWith("s") -> value.dropLast(1)
            else -> value
        }
    }
}
