package com.expeditee.plant.identifier.thesis

import android.app.Application
import android.util.Log
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.network.model.CitationData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ThesisPlantDetailViewModel(
    application: Application,
    private val store: ThesisLocalStore,
    private val youtubeCareVideoRepository: YoutubeCareVideoRepository,
    private val morphologyImageRepository: MorphologyImageRepository,
) : BaseViewModel<ThesisPlantDetailState>(application) {

    private val _state = MutableStateFlow<ThesisPlantDetailState>(ThesisPlantDetailState.Empty)
    val uiState: StateFlow<ThesisPlantDetailState> = _state.asStateFlow()

    private val _morphologyImages = MutableStateFlow(MorphologyPartImages())
    val morphologyImages: StateFlow<MorphologyPartImages> = _morphologyImages.asStateFlow()

    private val _openLink = MutableSharedFlow<CareLink>(extraBufferCapacity = 1)
    val openLink = _openLink.asSharedFlow()

    @Volatile
    private var lastTapAt = 0L
    private var morphologyJob: Job? = null

    override fun onState(state: ThesisPlantDetailState) {
        _state.value = state
    }

    fun load(result: PlantScanResult?, imagePath: String?) {
        if (result == null) {
            morphologyJob?.cancel()
            _morphologyImages.value = MorphologyPartImages()
            onState(ThesisPlantDetailState.Empty)
            return
        }
        val enriched = store.applyOverride(ThesisPlantCatalog.enrich(getApplication(), result))
        val image = store.findOverride(
            enriched.scientificName,
            enriched.commonNames.firstOrNull().orEmpty(),
        )?.imagePath?.takeIf { it.isNotBlank() } ?: imagePath
        onState(ThesisPlantDetailState.Content(enriched, image))
        loadMorphology(enriched.scientificName)
    }

    private fun loadMorphology(scientificName: String) {
        morphologyJob?.cancel()
        _morphologyImages.value = MorphologyPartImages()
        morphologyJob = launchBlock(Dispatchers.IO) {
            val images = morphologyImageRepository.load(scientificName)
            _morphologyImages.value = images
            Log.i(TAG, "morphology state updated for '$scientificName'")
        }
    }

    fun openCitation(cite: CitationData, plantName: String, scientific: String) {
        val now = System.currentTimeMillis()
        if (now - lastTapAt < 800L) {
            Log.w(TAG, "ignore double tap")
            return
        }
        lastTapAt = now
        Log.i(
            TAG,
            "tap name='$plantName' scientific='$scientific' type=${cite.type} " +
                "citeUrl=${cite.url} citeVideoId=${cite.videoId}",
        )
        if (!CareVideoLookup.isYoutube(cite)) {
            val url = cite.url?.takeIf { it.isNotBlank() } ?: Constants.DUOC_DIEN_URL
            Log.i(TAG, "non-youtube emit url=$url")
            _openLink.tryEmit(CareLink.Web(url))
            return
        }
        val catalogId = CareVideoLookup.parseVideoId(cite.videoId)
            ?: CareVideoLookup.parseVideoId(cite.url)
        if (catalogId != null) {
            Log.i(TAG, "use catalog videoId=$catalogId watch=${CareVideoLookup.watchUrl(catalogId)}")
            _openLink.tryEmit(CareLink.Watch(catalogId))
            return
        }
        launchBlock(Dispatchers.IO) {
            val resolved = youtubeCareVideoRepository.lookupVideoId(plantName, scientific)
            val link = if (resolved != null) {
                CareLink.Watch(resolved)
            } else {
                CareLink.Search(CareVideoLookup.searchQuery(plantName, scientific))
            }
            Log.i(TAG, "emit $link")
            _openLink.emit(link)
        }
    }

    companion object {
        private const val TAG = CareVideoLookup.LOG_TAG
    }
}

sealed class CareLink {
    data class Watch(val videoId: String) : CareLink()
    data class Search(val query: String) : CareLink()
    data class Web(val url: String) : CareLink()
}

sealed class ThesisPlantDetailState : BaseViewModel.State {
    data object Empty : ThesisPlantDetailState()
    data class Content(
        val result: PlantScanResult,
        val localImagePath: String?,
    ) : ThesisPlantDetailState()
}
