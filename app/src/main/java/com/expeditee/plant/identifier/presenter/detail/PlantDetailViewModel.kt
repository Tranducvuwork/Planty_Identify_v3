package com.expeditee.plant.identifier.presenter.detail

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.helper.SiteManager
import com.expeditee.plant.identifier.model.BasicSection
import com.expeditee.plant.identifier.model.CareSection
import com.expeditee.plant.identifier.model.ClassificationSection
import com.expeditee.plant.identifier.model.EnvironmentSection
import com.expeditee.plant.identifier.model.ImagesSection
import com.expeditee.plant.identifier.model.PhysicalSection
import com.expeditee.plant.identifier.model.PlantUIModel
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.model.PopularPlantModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PlantDetailViewModel(
    application: Application,
    private val plantRepository: PlantRepository,
    private val myPlantRepository: MyPlantRepository,
) : BaseViewModel<PlantDetailState>(application) {

    private val _state = MutableStateFlow<PlantDetailState>(PlantDetailState.Loading)
    val state: StateFlow<PlantDetailState> = _state.asStateFlow()

    private val _addToGardenSuccess = MutableSharedFlow<Boolean>()
    val addToGardenSuccess: SharedFlow<Boolean> = _addToGardenSuccess.asSharedFlow()

    private var currentPlant: PlantUIModel? = null

    private data class PlantResolutionInput(
        val scientificName: String,
        val commonName: String,
    )

    override fun onState(state: PlantDetailState) {
        when (state) {
            is PlantDetailState.LoadPlant -> loadPlant(
                plantId = state.plantId,
                previewName = state.previewName,
                previewScientificName = state.previewScientificName,
                previewImageUrl = state.previewImageUrl,
                previewDescription = state.previewDescription,
                allowResolveFallback = state.allowResolveFallback,
            )
            else -> emitUiState(state)
        }
    }

    private fun loadPlant(
        plantId: String,
        previewName: String?,
        previewScientificName: String?,
        previewImageUrl: String?,
        previewDescription: String?,
        allowResolveFallback: Boolean,
    ) {
        emitUiState(PlantDetailState.Loading)

        val resolutionInput = PlantResolutionInput(
            scientificName = previewScientificName.orEmpty(),
            commonName = previewName.orEmpty(),
        )
        val initialPlantId = plantId.takeIf { it.isNotBlank() }

        if (
            previewName?.isNotBlank() == true ||
            previewScientificName?.isNotBlank() == true ||
            previewImageUrl?.isNotBlank() == true
        ) {
            val previewPlant = createPreviewPlant(
                id = initialPlantId.orEmpty(),
                previewName = previewName,
                previewScientificName = previewScientificName,
                previewImageUrl = previewImageUrl,
                previewDescription = previewDescription,
            )
            currentPlant = previewPlant
            emitUiState(
                toContentState(
                    plant = previewPlant,
                    isPreview = true,
                ),
            )
        }

        launchBlock(Dispatchers.IO) {
            val resolvedPlantId = when {
                initialPlantId != null -> initialPlantId
                allowResolveFallback -> resolvePlantId(resolutionInput)
                else -> null
            }

            if (resolvedPlantId == null) {
                if (currentPlant == null) {
                    emitUiState(PlantDetailState.Error(getString(R.string.plant_detail_not_found)))
                }
                return@launchBlock
            }

            if (currentPlant != null && currentPlant?.basic?.id != resolvedPlantId) {
                val existing = currentPlant
                if (existing != null) {
                    currentPlant = existing.copy(
                        basic = existing.basic.copy(id = resolvedPlantId),
                    )
                }
            }

            val language = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }
            val jsonPlant = plantRepository.getPlantDetailFromJsonById(resolvedPlantId, language)
            val mergedPlant = mergePlant(currentPlant, jsonPlant)
            if (mergedPlant == null) {
                if (currentPlant == null) {
                    emitUiState(PlantDetailState.Error(getString(R.string.plant_detail_not_found)))
                }
                return@launchBlock
            }

            currentPlant = mergedPlant
            emitUiState(
                toContentState(
                    plant = mergedPlant,
                    isPreview = false,
                ),
            )
        }
    }

    private fun createPreviewPlant(
        id: String,
        previewName: String?,
        previewScientificName: String?,
        previewImageUrl: String?,
        previewDescription: String?,
    ): PlantUIModel {
        return PlantUIModel(
            basic = BasicSection(
                id = id,
                commonName = previewName?.takeIf { it.isNotBlank() } ?: getString(R.string.unknown),
                scientificName = previewScientificName?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.unknown),
                description = previewDescription,
                commonNames = listOfNotNull(previewName?.takeIf { it.isNotBlank() }),
            ),
            classification = ClassificationSection(
                family = null,
                genus = null,
                species = null,
                plantType = null,
                origin = null,
                isTropical = false,
                isIndoor = false,
            ),
            physical = PhysicalSection(
                height = null,
                heightMin = null,
                heightMax = null,
                heightUnit = null,
                spread = null,
                bloomTime = null,
            ),
            care = CareSection(
                careLevel = null,
                growthRate = null,
                cycle = null,
                maintenance = null,
                propagation = null,
                watering = null,
                wateringGuide = null,
                pruningMonths = null,
                pruningGuide = null,
                hardiness = null,
                hardinessMin = null,
                hardinessMax = null,
            ),
            environment = EnvironmentSection(
                lightNeeds = null,
                sunlightTypes = null,
                sunlightGuide = null,
                temperature = null,
                humidity = null,
                humidityMin = null,
                humidityMax = null,
                soil = null,
                attracts = null,
                droughtTolerant = false,
                saltTolerant = false,
            ),
            images = ImagesSection(
                mainImage = previewImageUrl,
                gallery = listOfNotNull(previewImageUrl?.takeIf { it.isNotBlank() }),
            ),
        )
    }

    private fun toContentState(
        plant: PlantUIModel,
        isPreview: Boolean,
    ): PlantDetailState.Content {
        return PlantDetailState.Content(
            plant = plant,
            isPreview = isPreview,
        )
    }

    private suspend fun resolvePlantId(input: PlantResolutionInput): String? {
        if (input.scientificName.isBlank() && input.commonName.isBlank()) return null
        val language = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() }

        return plantRepository.resolvePlantForIdentify(
            scientificName = input.scientificName,
            commonNames = listOf(input.commonName).filter { it.isNotBlank() },
            language = language,
        )?.id
    }

    private fun mergePlant(
        previewPlant: PlantUIModel?,
        jsonPlant: PlantUIModel?,
    ): PlantUIModel? {
        val base = jsonPlant ?: previewPlant ?: return null
        val fallback = previewPlant

        return PlantUIModel(
            basic = base.basic.copy(
                id = base.basic.id.ifBlank { fallback?.basic?.id.orEmpty() },
                commonName = base.basic.commonName
                    .ifBlank { fallback?.basic?.commonName.orEmpty() }
                    .ifBlank { getString(R.string.unknown) },
                scientificName = base.basic.scientificName
                    .ifBlank { fallback?.basic?.scientificName.orEmpty() }
                    .ifBlank { getString(R.string.unknown) },
                description = base.basic.description ?: fallback?.basic?.description,
                commonNames = pickList(base.basic.commonNames, fallback?.basic?.commonNames),
            ),
            classification = base.classification.copy(
                family = base.classification.family ?: fallback?.classification?.family,
                genus = base.classification.genus ?: fallback?.classification?.genus,
                species = base.classification.species ?: fallback?.classification?.species,
                plantType = base.classification.plantType ?: fallback?.classification?.plantType,
                origin = pickList(base.classification.origin, fallback?.classification?.origin),
                isTropical = base.classification.isTropical || (fallback?.classification?.isTropical == true),
                isIndoor = base.classification.isIndoor || (fallback?.classification?.isIndoor == true),
            ),
            physical = base.physical.copy(
                height = base.physical.height ?: fallback?.physical?.height,
                heightMin = base.physical.heightMin ?: fallback?.physical?.heightMin,
                heightMax = base.physical.heightMax ?: fallback?.physical?.heightMax,
                heightUnit = base.physical.heightUnit ?: fallback?.physical?.heightUnit,
                dimensionsDescription = base.physical.dimensionsDescription ?: fallback?.physical?.dimensionsDescription,
                spread = base.physical.spread ?: fallback?.physical?.spread,
                bloomTime = base.physical.bloomTime ?: fallback?.physical?.bloomTime,
            ),
            care = base.care.copy(
                careLevel = base.care.careLevel ?: fallback?.care?.careLevel,
                growthRate = base.care.growthRate ?: fallback?.care?.growthRate,
                cycle = base.care.cycle ?: fallback?.care?.cycle,
                maintenance = base.care.maintenance ?: fallback?.care?.maintenance,
                propagation = pickList(base.care.propagation, fallback?.care?.propagation),
                watering = base.care.watering ?: fallback?.care?.watering,
                wateringGuide = base.care.wateringGuide ?: fallback?.care?.wateringGuide,
                pruningMonths = pickList(base.care.pruningMonths, fallback?.care?.pruningMonths),
                pruningGuide = base.care.pruningGuide ?: fallback?.care?.pruningGuide,
                hardiness = base.care.hardiness ?: fallback?.care?.hardiness,
                hardinessMin = base.care.hardinessMin ?: fallback?.care?.hardinessMin,
                hardinessMax = base.care.hardinessMax ?: fallback?.care?.hardinessMax,
            ),
            environment = base.environment.copy(
                lightNeeds = base.environment.lightNeeds ?: fallback?.environment?.lightNeeds,
                sunlightTypes = pickList(base.environment.sunlightTypes, fallback?.environment?.sunlightTypes),
                sunlightGuide = base.environment.sunlightGuide ?: fallback?.environment?.sunlightGuide,
                temperature = base.environment.temperature ?: fallback?.environment?.temperature,
                humidity = base.environment.humidity ?: fallback?.environment?.humidity,
                humidityMin = base.environment.humidityMin ?: fallback?.environment?.humidityMin,
                humidityMax = base.environment.humidityMax ?: fallback?.environment?.humidityMax,
                soil = pickList(base.environment.soil, fallback?.environment?.soil),
                attracts = pickList(base.environment.attracts, fallback?.environment?.attracts),
                droughtTolerant = base.environment.droughtTolerant || (fallback?.environment?.droughtTolerant == true),
                saltTolerant = base.environment.saltTolerant || (fallback?.environment?.saltTolerant == true),
            ),
            images = base.images.copy(
                mainImage = base.images.mainImage ?: fallback?.images?.mainImage,
                gallery = when {
                    base.images.gallery.isNotEmpty() -> base.images.gallery
                    fallback?.images?.gallery?.isNotEmpty() == true -> fallback.images.gallery
                    else -> emptyList()
                },
            ),
        )
    }

    private fun <T> pickList(primary: List<T>?, fallback: List<T>?): List<T>? {
        return when {
            !primary.isNullOrEmpty() -> primary
            !fallback.isNullOrEmpty() -> fallback
            else -> null
        }
    }

    fun addCurrentPlantToGarden(gardenId: String = SiteManager.DEFAULT_SITE_ID) {
        val plant = currentPlant ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                myPlantRepository.addPlant(
                    MyPlantModel(
                        id = 0,
                        uuid = UUID.randomUUID().toString(),
                        name = plant.basic.commonName,
                        scientificName = plant.basic.scientificName,
                        type = PopularPlantModel.TYPE_PLANT,
                        status = "healthy",
                        timestamp = System.currentTimeMillis(),
                        imageFile = plant.images.mainImage,
                        metadata = buildMetadataJson(plant),
                        gardenId = gardenId,
                        isActive = true
                    )
                )
                _addToGardenSuccess.emit(true)
            } catch (e: Exception) {
                _addToGardenSuccess.emit(false)
            }
        }
    }

    private fun buildMetadataJson(plant: PlantUIModel): String {
        return """{"common_names":${buildCommonNamesJson(plant.basic.commonNames)},"scientific_name":"${plant.basic.scientificName}"}"""
    }

    private fun buildCommonNamesJson(commonNames: List<String>?): String {
        if (commonNames.isNullOrEmpty()) return "[]"
        return commonNames.joinToString(",", "[", "]") { "\"$it\"" }
    }

    private fun emitUiState(state: PlantDetailState) {
        _state.value = state
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

sealed class PlantDetailState : BaseViewModel.State {
    data class LoadPlant(val plantId: String, val previewName: String?, val previewScientificName: String?, val previewImageUrl: String?, val previewDescription: String?, val allowResolveFallback: Boolean, ) : PlantDetailState()
    data object Loading : PlantDetailState()
    data class Content(val plant: PlantUIModel, val isPreview: Boolean) : PlantDetailState()
    data class Error(val message: String) : PlantDetailState()
}