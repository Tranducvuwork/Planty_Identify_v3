package com.expeditee.plant.identifier.presenter.processing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityScanProcessingBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.Solution
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.Serializable
import androidx.core.net.toUri
import com.expeditee.plant.identifier.presenter.error_process.ScanProcessingErrorActivity
import com.expeditee.plant.identifier.presenter.detail.PlantDetailActivity
import com.expeditee.plant.identifier.presenter.disease_detail.DiseaseDetailActivity
import com.expeditee.plant.identifier.thesis.HistoryImages
import com.expeditee.plant.identifier.thesis.PlantGroup
import com.expeditee.plant.identifier.thesis.PlantScanSession
import com.expeditee.plant.identifier.thesis.ScanHistoryItem
import com.expeditee.plant.identifier.thesis.ThesisLocalStore
import com.expeditee.plant.identifier.thesis.ThesisPlantCatalog
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ScanProcessingActivity :
    BaseActivity<ActivityScanProcessingBinding, ScanProcessingViewModel, ScanProcessingState>() {

    companion object {
        private const val TAG = "TAG123123"
        private const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val EXTRA_CAMERA_MODE = "extra_camera_mode"
        private const val EXTRA_IMAGE_SOURCE = "extra_image_source"
        private const val EXTRA_GARDEN_ID = "extra_garden_id"

        fun start(
            context: Context,
            imageUri: String,
            cameraMode: CameraMode,
            imageSource: String,
            gardenId: String? = null
        ) {
            val intent = Intent(context, ScanProcessingActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, imageUri)
                putExtra(EXTRA_CAMERA_MODE, cameraMode as Serializable)
                putExtra(EXTRA_IMAGE_SOURCE, imageSource)
                putExtra(EXTRA_GARDEN_ID, gardenId.orEmpty())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }

    private val imageUri: String by lazy {
        intent.getStringExtra(EXTRA_IMAGE_URI).orEmpty()
    }
    private val cameraMode: CameraMode by lazy {
        (intent.getSerializableExtra(EXTRA_CAMERA_MODE) as? CameraMode) ?: CameraMode.IDENTIFY
    }
    private val imageSource: String by lazy {
        intent.getStringExtra(EXTRA_IMAGE_SOURCE).orEmpty()
    }
    private val gardenId: String by lazy {
        intent.getStringExtra(EXTRA_GARDEN_ID).orEmpty().ifBlank {
            com.expeditee.plant.identifier.helper.SiteManager.DEFAULT_SITE_ID
        }
    }

    override fun isFitsSystemWindows() = false
    override fun getLazyViewModel() = viewModel<ScanProcessingViewModel>()

    override fun provideLayout() = R.layout.activity_scan_processing

    private val historyStore: ThesisLocalStore by inject()
    private var processingStepJob: Job? = null
    private var hasNavigatedAway = false

    override fun initViews(savedInstanceState: Bundle?) {
        setupInsets()
        setupStaticContent()
        startProcessingStepLoop()
        Log.d(TAG, "initViews imageUri='$imageUri' cameraMode=${cameraMode.typeMode} imageSource='$imageSource'")
        if (imageUri.isBlank()) {
            Log.e(TAG, "imageUri is blank, navigating to error")
            navigateToError(R.string.scan_processing_failed)
            finish()
            return
        }
        viewModel.startProcessing(
            imageUri = imageUri,
            cameraMode = cameraMode,
            imageSource = imageSource,
        )
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.state) { state ->
            renderState(state)
        }
    }

    private fun setupInsets() {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
    }

    private fun setupStaticContent() {
        bindScanPreview(imageUri)
    }

    private fun bindScanPreview(path: String) {
        if (path.isBlank()) return
        val uri = path.toUri()
        if (isTransientPhotoPickerUri(uri)) {
            Log.w(TAG, "skip picker content uri preview path=$path")
            return
        }
        runCatching {
            viewBinding.layoutScanArea.ivPlantImage.setImageURI(uri)
        }.onFailure {
            Log.w(TAG, "bindScanPreview failed path=$path error=${it.message}")
        }
    }

    private fun isTransientPhotoPickerUri(uri: android.net.Uri): Boolean {
        if (uri.scheme != "content") return false
        val authority = uri.authority.orEmpty()
        val path = uri.path.orEmpty()
        return authority.contains("photopicker", ignoreCase = true) ||
            path.contains("/picker/", ignoreCase = true)
    }

    private fun renderState(state: ScanProcessingState) {
        Log.d(TAG, "renderState state=${state::class.simpleName}")
        when (state) {
            ScanProcessingState.Loading -> startProcessingStepLoop()

            is ScanProcessingState.Completed -> {
                stopProcessingStepLoop()
                Log.d(
                    TAG,
                    "completed diagnose=${state.diagnosedDisease != null} healthy=${state.diagnosedDisease?.isHealthy} isPlant=${state.diagnosedDisease?.isPlant} disease=${state.diagnosedDisease?.disease?.scientificName}"
                )
                navigateToResultOrError(state)
                finish()
            }

            is ScanProcessingState.Failed -> {
                if (hasNavigatedAway) return
                hasNavigatedAway = true
                stopProcessingStepLoop()
                Log.w(
                    TAG,
                    "failed messageRes=${state.messageRes} subtitleRes=${state.subtitleRes} subtitleText=${state.subtitleText.orEmpty().take(120)} isDiagnose=${state.isDiagnose} isHealthy=${state.isHealthy}"
                )
                navigateToError(
                    messageRes = state.messageRes,
                    subtitleRes = state.subtitleRes,
                    subtitleText = state.subtitleText,
                    isDiagnose = state.isDiagnose,
                    isHealthy = state.isHealthy,
                )
                finish()
            }
        }
    }

    private fun startProcessingStepLoop() {
        if (processingStepJob?.isActive == true) return
        processingStepJob = lifecycleScope.launch {
            val steps = listOf(
                viewBinding.layoutScanDone.rowEnhance,
                viewBinding.layoutScanDone.rowAnalyze,
                viewBinding.layoutScanDone.rowIdentify,
                viewBinding.layoutScanDone.rowResearch,
                viewBinding.layoutScanDone.rowPrepare,
            )
            while (isActive) {
                steps.forEachIndexed { index, _ ->
                    renderProcessingStep(index)
                    delay(3_800L)
                }
            }
        }
    }

    private fun stopProcessingStepLoop() {
        processingStepJob?.cancel()
        processingStepJob = null
        val animations = listOf(
            viewBinding.layoutScanDone.animEnhance,
            viewBinding.layoutScanDone.animAnalyze,
            viewBinding.layoutScanDone.animIdentify,
            viewBinding.layoutScanDone.animResearch,
            viewBinding.layoutScanDone.animPrepare,
        )
        animations.forEach { anim ->
            anim.cancelAnimation()
            anim.progress = 0f
        }
    }

    private fun renderProcessingStep(activeIndex: Int) {
        val rows = listOf(
            viewBinding.layoutScanDone.rowEnhance,
            viewBinding.layoutScanDone.rowAnalyze,
            viewBinding.layoutScanDone.rowIdentify,
            viewBinding.layoutScanDone.rowResearch,
            viewBinding.layoutScanDone.rowPrepare,
        )
        val animations = listOf(
            viewBinding.layoutScanDone.animEnhance,
            viewBinding.layoutScanDone.animAnalyze,
            viewBinding.layoutScanDone.animIdentify,
            viewBinding.layoutScanDone.animResearch,
            viewBinding.layoutScanDone.animPrepare,
        )
        val texts = listOf(
            viewBinding.layoutScanDone.tvEnhance,
            viewBinding.layoutScanDone.tvAnalyze,
            viewBinding.layoutScanDone.tvIdentify,
            viewBinding.layoutScanDone.tvResearch,
            viewBinding.layoutScanDone.tvPrepare,
        )
        rows.forEachIndexed { index, row ->
            val active = index == activeIndex
            val anim = animations[index]
            row.animate().cancel()
            row.animate()
                .alpha(if (active) 1f else 0.42f)
                .scaleX(if (active) 1.04f else 1f)
                .scaleY(if (active) 1.04f else 1f)
                .setDuration(220L)
                .start()
            texts[index].setTextColor(if (active) Color.WHITE else Color.argb(150, 255, 255, 255))
            if (active) {
                if (!anim.isAnimating) {
                    anim.progress = 0f
                    anim.playAnimation()
                }
            } else {
                anim.cancelAnimation()
                anim.progress = 0f
            }
        }
    }

    private fun navigateToResultOrError(state: ScanProcessingState.Completed) {
        val isDiagnoseMode = cameraMode.typeMode == CameraMode.TYPE_DIAGNOSE

        val canOpenDiagnoseDetail =
            isDiagnoseMode &&
                state.diagnosedDisease != null &&
                !state.diagnosedDisease.isHealthy &&
                state.diagnosedDisease.disease != null

        if (canOpenDiagnoseDetail) {
            val disease = state.diagnosedDisease?.disease
            val diseaseId = disease?.id ?: -1
            Log.d(TAG, "navigate diagnose detail diseaseId=$diseaseId name=${disease?.scientificName}")
            DiseaseDetailActivity.start(
                context = this,
                diseaseId = diseaseId,
                imagePath = imageUri,
                probability = (state.diagnosedDisease.probability * 100).toInt(),
                solution = state.diagnosedDisease?.solution,
                fallbackDiseaseName = disease?.scientificName ?: disease?.commonNames?.firstOrNull(),
                fallbackDiseaseDescription = disease?.description,
            )
            return
        }

        val canShowDiagnoseNotPlant =
            isDiagnoseMode &&
                state.diagnosedDisease != null &&
                !state.diagnosedDisease.isPlant

        if (canShowDiagnoseNotPlant) {
            Log.d(TAG, "navigate diagnose not-plant")
            navigateToError(
                messageRes = R.string.diagnose_not_detected_title,
                subtitleRes = R.string.diagnose_not_detected_message,
                isDiagnose = true,
                isHealthy = false,
            )
            return
        }

        val canShowDiagnoseHealthy =
            isDiagnoseMode &&
                state.diagnosedDisease != null &&
                state.diagnosedDisease.isHealthy

        if (canShowDiagnoseHealthy) {
            Log.d(TAG, "navigate diagnose healthy")
            navigateToError(R.string.diagnose_healthy_title, isDiagnose = true, isHealthy = true)
            return
        }

        val canShowDiagnoseNotDetected =
            isDiagnoseMode &&
                (state.diagnosedDisease == null || state.diagnosedDisease.disease == null)

        if (canShowDiagnoseNotDetected) {
            Log.d(TAG, "navigate diagnose not-detected")
            navigateToError(R.string.diagnose_not_detected_title, isDiagnose = true, isHealthy = false)
            return
        }

        val plantId = state.resolvedPlantId.orEmpty()
        val scientificName = state.resolvedScientificName.orEmpty().ifBlank { state.highlightName.orEmpty() }
        val allowResolveFallback =
            plantId.isBlank() &&
                (!state.resolutionAttemptedAtIdentify || !state.catalogReadyAtIdentify)
        val canOpenIdentifyDetail =
            cameraMode.typeMode == CameraMode.TYPE_IDENTIFY &&
                (plantId.isNotBlank() || scientificName.isNotBlank())

        if (canOpenIdentifyDetail) {
            Log.d(TAG, "navigate identify detail plantId='$plantId' scientificName='$scientificName'")
            val thesis = PlantScanSession.lastResult
            if (thesis != null) {
                val enriched = ThesisPlantCatalog.enrich(this, thesis)
                recordIdentifyHistory(
                    commonName = enriched.commonNames.firstOrNull().orEmpty().ifBlank { enriched.scientificName },
                    scientificName = enriched.scientificName,
                    imagePath = imageUri.ifBlank { enriched.imageUrl.orEmpty() },
                    plantGroup = PlantGroup.from(enriched).key,
                )
                PlantScanSession.store(enriched, imageUri)
                ThesisPlantDetailActivity.start(this, imageUri)
            } else {
                recordIdentifyHistory(
                    commonName = state.resolvedPlantName ?: state.highlightName ?: scientificName,
                    scientificName = scientificName,
                    imagePath = imageUri.ifBlank { state.resolvedImageUrl.orEmpty() },
                    plantGroup = "",
                )
                PlantDetailActivity.start(
                    context = this,
                    plantId = plantId,
                    plantName = state.resolvedPlantName ?: state.highlightName,
                    scientificName = scientificName,
                    imageUrl = state.resolvedImageUrl,
                    localImagePath = imageUri,
                    previewDescription = state.resolvedPreviewDescription,
                    allowResolveFallback = allowResolveFallback,
                    gardenId = gardenId
                )
            }
            return
        }

        Log.e(TAG, "no valid navigation branch, fallback to generic error")
        navigateToError(R.string.scan_processing_failed)
    }

    private fun recordIdentifyHistory(
        commonName: String,
        scientificName: String,
        imagePath: String,
        plantGroup: String,
    ) {
        if (commonName.isBlank() && scientificName.isBlank()) return
        val storedImage = if (imagePath.startsWith("http")) {
            imagePath
        } else {
            HistoryImages.persist(this, imagePath)
        }
        historyStore.addHistory(
            ScanHistoryItem(
                commonName = commonName.ifBlank { scientificName },
                scientificName = scientificName,
                imagePath = storedImage,
                plantGroup = plantGroup,
            ),
        )
    }

    override fun onDestroy() {
        stopProcessingStepLoop()
        super.onDestroy()
    }

    private fun navigateToError(
        messageRes: Int,
        subtitleRes: Int = messageRes,
        subtitleText: String? = null,
        isDiagnose: Boolean = false,
        isHealthy: Boolean = false,
    ) {
        Log.d(
            TAG,
            "navigateToError messageRes=$messageRes subtitleRes=$subtitleRes subtitleText=${subtitleText.orEmpty().take(120)} isDiagnose=$isDiagnose isHealthy=$isHealthy"
        )
        ScanProcessingErrorActivity.start(
            context = this,
            messageRes = messageRes,
            subtitleRes = subtitleRes,
            subtitleText = subtitleText,
            isDiagnose = isDiagnose,
            isHealthy = isHealthy,
            localImagePath = imageUri,
        )
    }
}
