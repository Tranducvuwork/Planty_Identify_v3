package com.expeditee.plant.identifier.presenter.disease_detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityDiseaseDetailBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.Solution
import com.expeditee.plant.identifier.presenter.disease_detail.adapter.OtherDiseasesAdapter
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.expeditee.plant.identifier.thesis.ThesisDiseaseCopy
import com.expeditee.plant.identifier.thesis.ToxicAlertPlayer

class DiseaseDetailActivity :
    BaseActivity<ActivityDiseaseDetailBinding, DiseaseDetailViewModel, DiseaseDetailUiState>() {
    private var alertPlayed = false
    private var selectedTabId = "overview"
    private var hasSymptoms = false
    private var hasCauses = false
    private var hasPrevention = false
    private var hasVisualInfo = false
    private var hasCompareInfo = false
    private var hasTreatment = false
    private var hasSolution = false
    private var hasOtherDiseases = false
    private var hasVideos = false

    private val otherDiseasesAdapter by lazy {
        OtherDiseasesAdapter { item, _ ->
            DiseaseDetailActivity.start(this, item.id, null, 0, null)
        }
    }
    private val diseaseInfoBinding
        get() = viewBinding.diseaseInfo

    companion object {
        private const val EXTRA_DISEASE_ID = "extra_disease_id"
        private const val EXTRA_IMAGE_PATH = "extra_image_path"
        private const val EXTRA_PROBABILITY = "extra_probability"
        private const val EXTRA_SOLUTION = "extra_solution"
        private const val EXTRA_FALLBACK_DISEASE_NAME = "extra_fallback_disease_name"
        private const val EXTRA_FALLBACK_DISEASE_DESCRIPTION = "extra_fallback_disease_description"

        fun start(
            context: Context,
            diseaseId: Int,
            imagePath: String?,
            probability: Int,
            solution: Solution? = null,
            fallbackDiseaseName: String? = null,
            fallbackDiseaseDescription: String? = null,
        ) {
            val intent = Intent(context, DiseaseDetailActivity::class.java).apply {
                putExtra(EXTRA_DISEASE_ID, diseaseId)
                putExtra(EXTRA_IMAGE_PATH, imagePath)
                putExtra(EXTRA_PROBABILITY, probability)
                solution?.let { putExtra(EXTRA_SOLUTION, it) }
                putExtra(EXTRA_FALLBACK_DISEASE_NAME, fallbackDiseaseName)
                putExtra(EXTRA_FALLBACK_DISEASE_DESCRIPTION, fallbackDiseaseDescription)
            }
            context.startActivity(intent)
        }
    }

    override fun getLazyViewModel(): Lazy<DiseaseDetailViewModel> = viewModel()

    override fun provideLayout(): Int = R.layout.activity_disease_detail
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.llToolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }

        val diseaseId = intent.getIntExtra(EXTRA_DISEASE_ID, -1)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val probability = intent.getIntExtra(EXTRA_PROBABILITY, 0)
        val solution = intent.getSerializableExtra(EXTRA_SOLUTION) as? Solution
        val fallbackDiseaseName = intent.getStringExtra(EXTRA_FALLBACK_DISEASE_NAME)
        val fallbackDiseaseDescription = intent.getStringExtra(EXTRA_FALLBACK_DISEASE_DESCRIPTION)

        setupToolbar()
        setupButtons()
        setupTabs()
        viewModel.onState(
            DiseaseDetailUiState.LoadDisease(
                diseaseId = diseaseId,
                imagePath = imagePath,
                probability = probability,
                solution = solution,
                fallbackDiseaseName = fallbackDiseaseName,
                fallbackDiseaseDescription = fallbackDiseaseDescription,
            ),
        )
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.consumeNativeAd(this@DiseaseDetailActivity, viewBinding.nativeAdView)
        }
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
        if (!hasPurchase) return
        viewBinding.nativeAdView.removeAllViews()
        viewBinding.nativeAdView.beGone()
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.state) { state ->
            renderState(state)
        }
    }

    private fun setupToolbar() {
        viewBinding.ivBackToolbar.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }


    private fun setupButtons() {
        viewBinding.flRetakeButton.setOnClickListener {
            startActivity(Intent(this, ScanCameraActivity::class.java).apply {
                putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.DIAGNOSE)
            })
            finish()
        }
        diseaseInfoBinding.similarImagesRecycler.apply {
            layoutManager = LinearLayoutManager(
                this@DiseaseDetailActivity, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = otherDiseasesAdapter
        }
        diseaseInfoBinding.btnFindPesticideStore.setOnClickListener {
            openPesticideStoreMap()
        }
    }

    private fun setupTabs() {
        viewBinding.tabOverview.setOnClickListener {
            selectTab("overview")
        }
        viewBinding.tabSymptoms.setOnClickListener {
            selectTab("symptoms")
        }
        viewBinding.tabImages.setOnClickListener {
            selectTab("images")
        }
        viewBinding.tabTreatment.setOnClickListener {
            selectTab("treatment")
        }
        viewBinding.tabVideos.setOnClickListener {
            selectTab("videos")
        }
        renderTabs()
    }

    private fun renderState(state: DiseaseDetailUiState) {
        when (state) {
            is DiseaseDetailUiState.LoadDisease -> Unit
            DiseaseDetailUiState.Loading -> showLoading()
            is DiseaseDetailUiState.Content -> {
                showContent()
                setupAccuracyBadge(state.uiModel.probability)
                setupDiseaseInfo(state.uiModel)
                showDiseaseAlert(state.uiModel.basicInfo.displayName)
                loadImages(state.uiModel.similarImages, state.uiModel.imagePath)
                setupSmallImageFrame(state.uiModel.imagePath)
                selectTab("overview", scrollToContent = false)
            }

            is DiseaseDetailUiState.Error -> showError(state.message)
        }
    }

    private fun showLoading() {
        viewBinding.pbLoading.visibility = View.VISIBLE
        viewBinding.tvErrorMessage.visibility = View.GONE
        setContentVisible(false)
    }

    private fun showContent() {
        viewBinding.pbLoading.visibility = View.GONE
        viewBinding.tvErrorMessage.visibility = View.GONE
        setContentVisible(true)
    }

    private fun showError(message: String) {
        viewBinding.pbLoading.visibility = View.GONE
        viewBinding.tvErrorMessage.visibility = View.VISIBLE
        viewBinding.tvErrorMessage.text = message
        setContentVisible(false)
    }

    private fun setContentVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        viewBinding.nsvContent.visibility = visibility
        viewBinding.llBottomButtonArea.visibility = visibility
    }

    private fun setTextWithTranslation(textView: TextView, text: String?) {
        textView.text = text
    }

    private fun showDiseaseAlert(rawName: String) {
        val name = ThesisDiseaseCopy.vietnameseName(rawName)
        viewBinding.layoutDiseaseAlert.visibility = View.VISIBLE
        viewBinding.tvDiseaseAlertMessage.text = getString(R.string.thesis_disease_alert_message, name)
        viewBinding.btnSuggestCure.setOnClickListener {
            selectTab("treatment")
        }
        if (!alertPlayed) {
            alertPlayed = true
            ToxicAlertPlayer.playQuiet(this)
        }
    }

    override fun onDestroy() {
        ToxicAlertPlayer.stop()
        super.onDestroy()
    }

    private fun setupDiseaseInfo(uiModel: DiseaseDetailUiModel) {
        setupBasicInfo(uiModel.basicInfo)
        setupDescription(uiModel.overview)
        setupSymptoms(uiModel.symptoms)
        setupCauses(uiModel.causes)
        setupPrevention(uiModel.prevention)
        setupVisualInfo(uiModel)
        setupTreatmentInfo(uiModel.treatment)
        setupSolutionInfo(uiModel.solution)
        setupSimilarImagesInfo(uiModel.otherDiseases)
        setupVideoInfo(uiModel.videos)
    }

    private fun setupDescription(overview: DiseaseOverviewUiModel) {
        setTextWithTranslation(diseaseInfoBinding.plantDescText, overview.description)
    }

    private fun setupBasicInfo(basicInfo: DiseaseBasicInfoUiModel) {
        setTextWithTranslation(viewBinding.diseaseName, basicInfo.displayName)
        setTextWithTranslation(viewBinding.diseaseNameToolbar, basicInfo.displayName)
        setTextWithTranslation(viewBinding.scientificNameText, basicInfo.scientificName)
        setTextWithTranslation(viewBinding.otherNameText, basicInfo.otherNames)
    }

    private fun setupTreatmentInfo(treatment: DiseaseTreatmentUiModel) {
        val section = diseaseInfoBinding

        val biological = treatment.biological
        val chemical = treatment.chemical
        val fallback = treatment.fallback

        section.bioTreatmentArea.visibility = View.GONE
        section.chemTreatmentArea.visibility = View.GONE

        hasTreatment = true
        section.diseaseTreatment.isVisible = hasTreatment
        section.organicSpiceTreatmentArea.visibility = View.VISIBLE
        section.btnFindPesticideStore.visibility = View.VISIBLE

        if (fallback.isNotEmpty()) {
            setTextWithTranslation(section.bioTreatmentText, fallback.joinToString("\n") { it.trim() })
            section.bioTreatmentArea.visibility = View.VISIBLE
            return
        }

        if (biological.isNotEmpty()) {
            setTextWithTranslation(section.bioTreatmentText, biological.joinToString("\n") { it.trim() })
            section.bioTreatmentArea.visibility = View.VISIBLE
        }

        if (chemical.isNotEmpty()) {
            setTextWithTranslation(section.chemTreatmentText, chemical.joinToString("\n") { it.trim() })
            section.chemTreatmentArea.visibility = View.VISIBLE
        }
    }

    private fun setupSymptoms(symptoms: List<String>) {
        bindBullets(
            area = diseaseInfoBinding.diseaseSymptomsArea,
            textView = diseaseInfoBinding.symptomsText,
            items = symptoms,
        )
    }

    private fun setupCauses(causes: List<String>) {
        bindBullets(
            area = diseaseInfoBinding.diseaseCauseArea,
            textView = diseaseInfoBinding.causesText,
            items = causes,
        )
    }

    private fun setupPrevention(prevention: List<String>) {
        bindBullets(
            area = diseaseInfoBinding.diseasePreventionArea,
            textView = diseaseInfoBinding.preventionText,
            items = prevention,
        )
    }

    private fun bindBullets(area: View, textView: TextView, items: List<String>) {
        val clean = items.map { it.trim() }.filter { it.isNotBlank() }
        area.isVisible = clean.isNotEmpty()
        textView.text = clean.joinToString("\n") { "• $it" }
        when (area.id) {
            R.id.diseaseSymptomsArea -> hasSymptoms = clean.isNotEmpty()
            R.id.diseaseCauseArea -> hasCauses = clean.isNotEmpty()
            R.id.diseasePreventionArea -> hasPrevention = clean.isNotEmpty()
        }
    }

    private fun setupVisualInfo(uiModel: DiseaseDetailUiModel) {
        val images = uiModel.similarImages.mapNotNull { it.url ?: it.urlSmall }
        val pestImage = uiModel.visuals.pestImageUrl ?: images.getOrNull(0)
        val symptomImage = uiModel.visuals.symptomImageUrl ?: images.getOrNull(1) ?: images.firstOrNull()
        val healthyImage = uiModel.visuals.healthyImageUrl
        val compareImage = uiModel.visuals.diseasedImageUrl ?: images.getOrNull(2) ?: symptomImage

        hasVisualInfo = !pestImage.isNullOrBlank() || !symptomImage.isNullOrBlank()
        hasCompareInfo = !healthyImage.isNullOrBlank() || !compareImage.isNullOrBlank()
        diseaseInfoBinding.diseaseVisualArea.isVisible = hasVisualInfo
        diseaseInfoBinding.diseaseCompareArea.isVisible = hasCompareInfo
        loadVisualImage(diseaseInfoBinding.pestImage, pestImage, R.drawable.ic_toxic)
        loadVisualImage(diseaseInfoBinding.symptomImage, symptomImage, R.drawable.ic_camera_home)
        loadVisualImage(diseaseInfoBinding.healthyImage, healthyImage, R.drawable.ic_plant_success)
        loadVisualImage(diseaseInfoBinding.diseaseCompareImage, compareImage, R.drawable.ic_camera_home)
    }

    private fun loadVisualImage(imageView: ImageView, value: String?, fallback: Int) {
        if (value.isNullOrBlank()) {
            imageView.setImageResource(fallback)
        } else {
            imageView.loadImageWithGithub(value, onFailed = { imageView.setImageResource(fallback) })
        }
    }

    private fun setupSimilarImagesInfo(otherDiseases: List<OtherDiseaseUiModel>) {
        val sectionBinding = diseaseInfoBinding
        hasOtherDiseases = otherDiseases.isNotEmpty()
        if (!hasOtherDiseases) {
            sectionBinding.diseaseSimilarImages.visibility = View.GONE
            return
        }
        sectionBinding.diseaseSimilarImages.visibility = View.VISIBLE
        otherDiseasesAdapter.submitData(otherDiseases)
    }

    private fun setupSolutionInfo(solution: Solution?) {
        val binding = diseaseInfoBinding
        val resolved = solution ?: ThesisDiseaseCopy.buildSolution(viewBinding.diseaseName.text?.toString())
        hasSolution = true
        binding.solutionCard.visibility = View.VISIBLE
        setTextWithTranslation(binding.solutionTitle, resolved.title ?: getString(R.string.solution))
        setTextWithTranslation(binding.solutionSummary, resolved.summary)
        val stepsText = resolved.steps?.filterNotNull()?.mapIndexed { i, s ->
            getString(R.string.solution_step_format, i + 1, s)
        }?.joinToString("\n").orEmpty()
        setTextWithTranslation(binding.solutionSteps, stepsText)

        val precautionsText = resolved.precautions?.filterNotNull()
            ?.joinToString("\n") { getString(R.string.precaution_bullet_format, it) }.orEmpty()
        setTextWithTranslation(binding.solutionPrecautions, precautionsText)
    }

    private fun setupVideoInfo(videos: List<DiseaseVideoUiModel>) {
        val container = diseaseInfoBinding.videoList
        container.removeAllViews()
        hasVideos = videos.isNotEmpty()
        diseaseInfoBinding.diseaseVideoArea.isVisible = hasVideos
        videos.forEach { video ->
            container.addView(videoRow(video))
        }
    }

    private fun videoRow(video: DiseaseVideoUiModel): View {
        return TextView(this).apply {
            text = "Video: ${video.title}"
            textSize = 13f
            typeface = ResourcesCompat.getFont(this@DiseaseDetailActivity, R.font.poppins_semibold)
            setTextColor(ContextCompat.getColor(this@DiseaseDetailActivity, R.color.primary_green))
            setPadding(24, 18, 24, 18)
            background = ContextCompat.getDrawable(this@DiseaseDetailActivity, R.drawable.plant_info_card_shape)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
            }
        }
    }

    private fun setupAccuracyBadge(probability: Int) {
        viewBinding.tvProbabilityText.text = probability.toString()
    }

    private fun selectTab(id: String, scrollToContent: Boolean = true) {
        selectedTabId = id
        renderTabs()
        renderTabContent()
        if (scrollToContent) {
            viewBinding.nsvContent.post {
                val y = viewBinding.diseaseInfo.root.top - 12
                viewBinding.nsvContent.smoothScrollTo(0, y.coerceAtLeast(0))
            }
        }
    }

    private fun renderTabContent() {
        val showOverview = selectedTabId == "overview"
        val showSymptoms = selectedTabId == "symptoms"
        val showImages = selectedTabId == "images"
        val showTreatment = selectedTabId == "treatment"
        val showVideos = selectedTabId == "videos"

        diseaseInfoBinding.diseaseDescArea.isVisible =
            showOverview && diseaseInfoBinding.plantDescText.text.isNotBlank()

        diseaseInfoBinding.diseaseSymptomsArea.isVisible = showSymptoms && hasSymptoms
        diseaseInfoBinding.diseaseCauseArea.isVisible = showSymptoms && hasCauses

        diseaseInfoBinding.diseaseVisualArea.isVisible = showImages && hasVisualInfo
        diseaseInfoBinding.diseaseCompareArea.isVisible = showImages && hasCompareInfo

        diseaseInfoBinding.diseaseTreatment.isVisible = showTreatment && hasTreatment
        diseaseInfoBinding.organicSpiceTreatmentArea.isVisible = showTreatment
        diseaseInfoBinding.btnFindPesticideStore.isVisible = showTreatment
        diseaseInfoBinding.diseasePreventionArea.isVisible = showTreatment && hasPrevention
        diseaseInfoBinding.solutionCard.isVisible = showTreatment && hasSolution
        diseaseInfoBinding.diseaseSimilarImages.isVisible = showTreatment && hasOtherDiseases

        diseaseInfoBinding.diseaseVideoArea.isVisible = showVideos && hasVideos
    }

    private fun renderTabs() {
        styleTab(viewBinding.tabOverview, selectedTabId == "overview")
        styleTab(viewBinding.tabSymptoms, selectedTabId == "symptoms")
        styleTab(viewBinding.tabImages, selectedTabId == "images")
        styleTab(viewBinding.tabTreatment, selectedTabId == "treatment")
        styleTab(viewBinding.tabVideos, selectedTabId == "videos")
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        val bgColor = if (selected) {
            ContextCompat.getColor(this, R.color.primary_green)
        } else {
            Color.WHITE
        }
        val strokeColor = ContextCompat.getColor(this, R.color.primary_green)
        tab.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke((1 * resources.displayMetrics.density).toInt().coerceAtLeast(1), strokeColor)
        }
        tab.setTextColor(if (selected) Color.WHITE else strokeColor)
    }

    private fun openPesticideStoreMap() {
        val diseaseName = viewBinding.diseaseName.text?.toString().orEmpty()
        val query = if (diseaseName.isBlank()) {
            "cửa hàng thuốc bảo vệ thực vật thuốc trừ sâu sinh học gần đây"
        } else {
            "cửa hàng thuốc bảo vệ thực vật thuốc trừ sâu sinh học $diseaseName gần đây"
        }
        val encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8.name())
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapsIntent)
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"),
                ),
            )
        }
    }


    private fun setupSmallImageFrame(imagePath: String?) {
        imagePath?.takeIf { it.isNotBlank() }?.let { path ->
            viewBinding.smallSizeImageView.loadImageWithGithub(path)
        }
    }

    private fun loadImages(similarImages: List<SimilarImageUiModel>, fallbackImagePath: String?) {
        if (!fallbackImagePath.isNullOrBlank()) {
            loadFallbackImage(fallbackImagePath)
            return
        }
        val imageUrl = similarImages.firstOrNull()?.let { it.url ?: it.urlSmall }
        when {
            !imageUrl.isNullOrBlank() -> {
                viewBinding.bigSizeImageView.loadImageWithGithub(
                    imageUrl, onFailed = { loadFallbackImage(fallbackImagePath) })
            }
        }
    }

    private fun loadFallbackImage(imagePath: String?) {
        imagePath?.takeIf { it.isNotBlank() }?.let { path ->
            viewBinding.bigSizeImageView.loadImageWithGithub(path)
        }
    }
}
