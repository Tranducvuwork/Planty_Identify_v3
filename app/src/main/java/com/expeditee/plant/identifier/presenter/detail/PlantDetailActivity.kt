package com.expeditee.plant.identifier.presenter.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityPlantDetailBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.CareSection
import com.expeditee.plant.identifier.model.ClassificationSection
import com.expeditee.plant.identifier.model.EnvironmentSection
import com.expeditee.plant.identifier.model.PhysicalSection
import com.expeditee.plant.identifier.model.PlantUIModel
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.detail.adapter.PlantPhotoLibListAdapter
import com.expeditee.plant.identifier.presenter.photo_library.PhotoLibraryActivity
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlantDetailActivity :
    BaseActivity<ActivityPlantDetailBinding, PlantDetailViewModel, PlantDetailState>() {

    companion object {
        private const val EXTRA_PLANT_ID = "extra_plant_id"
        private const val EXTRA_PLANT_NAME = "extra_plant_name"
        private const val EXTRA_SCIENTIFIC_NAME = "extra_scientific_name"
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_LOCAL_IMAGE_PATH = "extra_local_image_path"
        private const val EXTRA_PREVIEW_DESCRIPTION = "extra_preview_description"
        private const val EXTRA_ALLOW_RESOLVE_FALLBACK = "extra_allow_resolve_fallback"
        private const val EXTRA_GARDEN_ID = "extra_garden_id"

        fun start(
            context: Context,
            plantId: String,
            plantName: String?,
            scientificName: String?,
            imageUrl: String?,
            localImagePath: String? = null,
            previewDescription: String? = null,
            allowResolveFallback: Boolean = true,
            gardenId: String? = null,
        ) {
            val intent = Intent(context, PlantDetailActivity::class.java).apply {
                putExtra(EXTRA_PLANT_ID, plantId)
                putExtra(EXTRA_PLANT_NAME, plantName)
                putExtra(EXTRA_SCIENTIFIC_NAME, scientificName)
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_LOCAL_IMAGE_PATH, localImagePath)
                putExtra(EXTRA_PREVIEW_DESCRIPTION, previewDescription)
                putExtra(EXTRA_ALLOW_RESOLVE_FALLBACK, allowResolveFallback)
                putExtra(EXTRA_GARDEN_ID, gardenId)
            }
            context.startActivity(intent)
        }
    }

    private var localImagePath: String = ""
    private var selectedPhotoUrl: String? = null
    private var selectedPhotoIndex: Int = 0
    private val nameFormatter by lazy { PlantDetailNameFormatter() }
    private val overviewFormatter by lazy { PlantDetailOverviewFormatter() }
    private val conditionFormatter by lazy { PlantDetailConditionFormatter() }
    private val photoAdapter by lazy {
        PlantPhotoLibListAdapter { imageUrl, position ->
            selectedPhotoUrl = imageUrl
            selectedPhotoIndex = position
            openPhotoLibrary()
        }
    }

    override fun provideLayout() = R.layout.activity_plant_detail
    override fun getLazyViewModel() = viewModel<PlantDetailViewModel>()
    override fun isFitsSystemWindows() = false
    override fun isLightStatusBar() = true

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.llToolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }

        val plantId = intent.getStringExtra(EXTRA_PLANT_ID).orEmpty()
        localImagePath = intent.getStringExtra(EXTRA_LOCAL_IMAGE_PATH).orEmpty()
        initPhotoLibrary()

        viewModel.onState(
            PlantDetailState.LoadPlant(
                plantId = plantId,
                previewName = intent.getStringExtra(EXTRA_PLANT_NAME),
                previewScientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME),
                previewImageUrl = intent.getStringExtra(EXTRA_IMAGE_URL),
                previewDescription = intent.getStringExtra(EXTRA_PREVIEW_DESCRIPTION),
                allowResolveFallback = intent.getBooleanExtra(EXTRA_ALLOW_RESOLVE_FALLBACK, true),
            ),
        )
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.consumeNativeAd(this@PlantDetailActivity, viewBinding.nativeAdView)
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

    override fun initListeners() {
        super.initListeners()
        viewBinding.ivBackToolbar.onClickWithScale {
            finish()
        }
        viewBinding.flRetakeButton.onClickWithScale {
            Intent(this@PlantDetailActivity, ScanCameraActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
                finish()
            }
        }
        viewBinding.ivPlantMain.onClickWithScale {
            if (photoAdapter.list.isNotEmpty()) {
                openPhotoLibrary()
            }
        }
    }

    private fun openPhotoLibrary() {
        val photoUrls = photoAdapter.list.filter { it.isNotBlank() }
        if (photoUrls.isNotEmpty()) {
            PhotoLibraryActivity.start(this, photoUrls, selectedPhotoIndex)
        }
    }

    private fun renderState(state: PlantDetailState) {
        when (state) {
            PlantDetailState.Loading -> showLoading()
            is PlantDetailState.Content -> showContent(state)
            is PlantDetailState.Error -> showError(state.message)
            is PlantDetailState.LoadPlant -> Unit
        }
    }

    private fun showLoading() {
        viewBinding.pbLoading.visibility = View.VISIBLE
        viewBinding.tvErrorMessage.visibility = View.GONE
        setContentVisible(false)
    }

    private fun showContent(state: PlantDetailState.Content) {
        viewBinding.pbLoading.visibility = View.GONE
        viewBinding.tvErrorMessage.visibility = View.GONE
        setContentVisible(true)

        val plant = state.plant

        bindPlantBasicInfo(
            plantName = plant.basic.commonName,
            scientificName = plant.basic.scientificName,
            commonNames = plant.basic.commonNames,
        )

        setupPhotoLibrary(plant.images.gallery)
        setExpandableSectionsFromPlant(plant)

        val imageForDisplay = selectImageForDisplay(plant)
        val fallbackImage = getFallbackImage(imageForDisplay)
        loadImages(imageForDisplay, fallbackImage)

        bindOverviewText(
            plantName = plant.basic.commonName,
            scientificName = plant.basic.scientificName,
            detailDescription = plant.basic.description,
            isPreview = state.isPreview,
        )
        bindTaxonomyInfo(plant.classification, plant.basic.scientificName)
        bindConditionsInfo(plant.environment, plant.physical, plant.care)
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

    private fun initPhotoLibrary() {
        viewBinding.rvPhotoList.apply {
            layoutManager =
                LinearLayoutManager(this@PlantDetailActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoAdapter
        }
    }

    private fun setupPhotoLibrary(photoLibraryUrls: List<String>) {
        val photoUrls = photoLibraryUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        if (photoUrls.isEmpty()) {
            viewBinding.rvPhotoList.visibility = View.GONE
            return
        }

        viewBinding.rvPhotoList.visibility = View.VISIBLE

        if (selectedPhotoUrl.isNullOrBlank() || photoUrls.none { it == selectedPhotoUrl }) {
            selectedPhotoUrl = photoUrls.first()
        }
        photoAdapter.submitData(photoUrls)
    }

    private fun setExpandableSectionsFromPlant(plant: PlantUIModel) {
        val care = plant.care
        val environment = plant.environment

        val wateringCardDisplay =
            conditionFormatter.clean(care.watering) ?: conditionFormatter.clean(care.wateringGuide)
            ?: getString(R.string.unknown)
        setTextWithTranslation(viewBinding.includeCareSection.waterTextCard, wateringCardDisplay)

        val pruningCardDisplay = conditionFormatter.formatList(care.pruningMonths)
            ?: conditionFormatter.clean(care.pruningGuide) ?: getString(R.string.unknown)
        setTextWithTranslation(
            viewBinding.includeCareSection.pruningMonthsTextCard,
            pruningCardDisplay
        )

        val sunlightCardDisplay =
            conditionFormatter.formatList(environment.sunlightTypes) ?: conditionFormatter.clean(
                environment.sunlightGuide
            ) ?: conditionFormatter.clean(environment.lightNeeds) ?: getString(R.string.unknown)
        setTextWithTranslation(
            viewBinding.includeCareSection.sunlightTypesTextCard,
            sunlightCardDisplay
        )

        val soilCardDisplay =
            conditionFormatter.formatList(environment.soil) ?: getString(R.string.unknown)
        setTextWithTranslation(viewBinding.includeCareSection.soilTextCard, soilCardDisplay)

        val wateringGuide = conditionFormatter.clean(care.wateringGuide)
            ?: getString(R.string.plant_detail_watering_guide_fallback)
        setTextWithTranslation(viewBinding.includeCareSection.wateringGuideText, wateringGuide)
        viewBinding.includeCareSection.wateringContentLayout.visibility = View.VISIBLE

        val pruningGuide = conditionFormatter.clean(care.pruningGuide)
            ?: getString(R.string.plant_detail_pruning_guide_fallback)
        setTextWithTranslation(viewBinding.includeCareSection.pruningGuideText, pruningGuide)
        viewBinding.includeCareSection.wateringContentLayout.visibility = View.VISIBLE

        val sunlightGuide = conditionFormatter.clean(environment.sunlightGuide)
            ?: getString(R.string.plant_detail_sunlight_guide_fallback)
        setTextWithTranslation(viewBinding.includeCareSection.sunlightGuideText, sunlightGuide)
        viewBinding.includeCareSection.sunlightContentLayout.visibility = View.VISIBLE
    }

    private fun bindPlantBasicInfo(
        plantName: String,
        scientificName: String,
        commonNames: List<String>?,
    ) {
        val resolvedPlantName = nameFormatter.clean(plantName) ?: getString(R.string.unknown)
        val resolvedScientificName = nameFormatter.clean(scientificName)
        val resolvedOtherName = commonNames.orEmpty().mapNotNull { nameFormatter.clean(it) }
            .filterNot { it.equals(resolvedPlantName, ignoreCase = true) }.distinct().firstOrNull()
            ?: nameFormatter.extractOtherName(scientificName)

        viewBinding.tvPlantName.text = resolvedPlantName
        setTextWithTranslation(viewBinding.tvPlantName, resolvedPlantName)
        viewBinding.tvPlantName.visibility = View.VISIBLE
        val scientificNameDisplay = conditionFormatter.formatLabeled(
            label = getString(R.string.scientific_name_label),
            value = resolvedScientificName ?: getString(R.string.unknown),
        ).orEmpty()
        setTextWithTranslation(viewBinding.tvScientificName, scientificNameDisplay)

        val otherNameDisplay = conditionFormatter.formatLabeled(
            label = getString(R.string.other_name_label),
            value = resolvedOtherName ?: getString(R.string.unknown),
        ).orEmpty()
        setTextWithTranslation(viewBinding.tvOtherName, otherNameDisplay)
    }

    private fun bindOverviewText(
        plantName: String,
        scientificName: String,
        detailDescription: String?,
        isPreview: Boolean,
    ) {
        val resolvedPlantName = nameFormatter.clean(plantName) ?: getString(R.string.unknown)
        val resolvedScientificName =
            nameFormatter.clean(scientificName) ?: getString(R.string.unknown)
        val scientificNameLine = conditionFormatter.formatLabeled(
            label = getString(R.string.scientific_name_label),
            value = resolvedScientificName,
        ).orEmpty()
        val previewNotice = if (isPreview) {
            getString(R.string.plant_detail_preview_loading_notice)
        } else {
            null
        }

        val overviewText = overviewFormatter.formatOverview(
            detailDescription = detailDescription,
            catalogEntryLine = getString(
                R.string.plant_detail_catalog_entry_sentence, resolvedPlantName
            ),
            scientificNameLine = scientificNameLine,
            previewNotice = previewNotice,
        ) ?: getString(R.string.plant_detail_description_placeholder)

        setTextWithTranslation(viewBinding.includeInfoSection.plantDescText, overviewText)
    }

    private fun bindTaxonomyInfo(classification: ClassificationSection, scientificName: String) {
        val hasFamily = !classification.family.isNullOrBlank()
        val genusText =
            conditionFormatter.clean(classification.genus) ?: nameFormatter.extractGenus(
                scientificName
            )
        val hasGenus = !genusText.isNullOrBlank()
        val speciesText = conditionFormatter.clean(classification.species)
        val hasSpecies = !speciesText.isNullOrBlank()
        val typeText = conditionFormatter.clean(classification.plantType)
        val hasType = !typeText.isNullOrBlank()
        val originText = conditionFormatter.formatList(classification.origin)
        val hasOrigin = !originText.isNullOrBlank()

        if (!hasFamily && !hasGenus && !hasSpecies && !hasType && !hasOrigin) {
            viewBinding.includeInfoSection.taxonomyRoot.visibility = View.GONE
            return
        }

        viewBinding.includeInfoSection.taxonomyRoot.visibility = View.VISIBLE

        if (hasFamily) {
            viewBinding.includeInfoSection.familyView.visibility = View.VISIBLE
            setTextWithTranslation(viewBinding.includeInfoSection.familyText, classification.family)
        } else {
            viewBinding.includeInfoSection.familyView.visibility = View.GONE
        }

        if (hasGenus) {
            viewBinding.includeInfoSection.genusView.visibility = View.VISIBLE
            setTextWithTranslation(viewBinding.includeInfoSection.genusText, genusText.orEmpty())
        } else {
            viewBinding.includeInfoSection.genusView.visibility = View.GONE
        }

        if (hasSpecies) {
            viewBinding.includeInfoSection.speciesView.visibility = View.VISIBLE
            setTextWithTranslation(
                viewBinding.includeInfoSection.speciesText,
                speciesText.orEmpty()
            )
            viewBinding.includeInfoSection.speciesDivider.visibility = View.VISIBLE
        } else {
            viewBinding.includeInfoSection.speciesView.visibility = View.GONE
            viewBinding.includeInfoSection.speciesDivider.visibility = View.GONE
        }

        if (hasType) {
            viewBinding.includeInfoSection.typeView.visibility = View.VISIBLE
            setTextWithTranslation(viewBinding.includeInfoSection.typeText, typeText.orEmpty())
            viewBinding.includeInfoSection.typeDivider.visibility = View.VISIBLE
        } else {
            viewBinding.includeInfoSection.typeView.visibility = View.GONE
            viewBinding.includeInfoSection.typeDivider.visibility = View.GONE
        }

        if (hasOrigin) {
            viewBinding.includeInfoSection.originView.visibility = View.VISIBLE
            setTextWithTranslation(viewBinding.includeInfoSection.originText, originText.orEmpty())
            viewBinding.includeInfoSection.originDivider.visibility = View.VISIBLE
        } else {
            viewBinding.includeInfoSection.originView.visibility = View.GONE
            viewBinding.includeInfoSection.originDivider.visibility = View.GONE
        }
    }

    private fun bindConditionsInfo(
        environment: EnvironmentSection,
        physical: PhysicalSection,
        care: CareSection,
    ) {

        val dimensionsLine = conditionFormatter.formatLabeled(
            getString(R.string.plant_detail_dimensions_label),
            buildDimensionsText(physical),
        )
        val soilLine = conditionFormatter.formatLabeled(
            getString(R.string.soil_planted),
            conditionFormatter.formatList(environment.soil),
        )
        val hardinessLine = conditionFormatter.formatLabeled(
            getString(R.string.plant_detail_hardiness_label),
            conditionFormatter.clean(care.hardiness),
        )
        val tolerantLine = conditionFormatter.formatLabeled(
            getString(R.string.plant_detail_tolerant_label),
            conditionFormatter.formatList(environment.getTolerances()),
        )
        val detailLines = conditionFormatter.joinLines(
            main = conditionFormatter.formatLabeled(
                getString(R.string.attract),
                conditionFormatter.formatList(environment.attracts),
            ),
            extras = listOfNotNull(
                conditionFormatter.formatLabeled(
                    getString(R.string.propagation),
                    conditionFormatter.formatList(care.propagation),
                ),
                conditionFormatter.formatLabeled(
                    getString(R.string.plant_detail_growth_rate_label),
                    conditionFormatter.clean(care.growthRate),
                ),
                conditionFormatter.formatLabeled(
                    getString(R.string.plant_detail_cycle_label),
                    conditionFormatter.clean(care.cycle),
                ),
                conditionFormatter.formatLabeled(
                    getString(R.string.maintenance),
                    conditionFormatter.clean(care.maintenance),
                ),
                conditionFormatter.formatLabeled(
                    getString(R.string.temperature),
                    conditionFormatter.clean(environment.temperature),
                ),
                conditionFormatter.formatLabeled(
                    getString(R.string.humidity),
                    conditionFormatter.clean(environment.humidity),
                ),
            ),
        )

        setTextWithTranslation(viewBinding.includeCareSection.soilText, dimensionsLine)
        setTextWithTranslation(viewBinding.includeCareSection.sunlightText, soilLine)
        setTextWithTranslation(viewBinding.includeCareSection.tempText, hardinessLine)
        setTextWithTranslation(viewBinding.includeCareSection.humidText, tolerantLine)
        setTextWithTranslation(viewBinding.includeCareSection.attractText, detailLines)
    }

    private fun buildDimensionsText(physical: PhysicalSection): String? {
        val heightText = physical.getHeightDisplay()
        val description = conditionFormatter.clean(physical.dimensionsDescription)
        return conditionFormatter.joinLines(
            main = heightText,
            extras = listOfNotNull(description),
        )
    }

    private fun setTextWithTranslation(textView: TextView, text: String?) {
        textView.text = text
    }

    private fun setTextOrHide(textView: TextView, value: String?) {
        val content = conditionFormatter.clean(value)
        if (content.isNullOrBlank()) {
            textView.visibility = View.GONE
            textView.text = ""
            return
        }
        textView.visibility = View.VISIBLE
        textView.text = content
    }


    private fun selectImageForDisplay(plant: PlantUIModel): String? {
        return selectedPhotoUrl?.takeIf { it.isNotBlank() }
            ?: plant.images.mainImage?.takeIf { it.isNotBlank() }
            ?: plant.images.gallery.firstOrNull { it.isNotBlank() }
            ?: localImagePath.takeIf { it.isNotBlank() }
    }

    private fun getFallbackImage(imageForDisplay: String?): String? {
        return localImagePath.takeIf {
            it.isNotBlank() && it != imageForDisplay
        }
    }

    private fun loadImages(imageUrl: String?, fallbackUrl: String?) {
        loadImageViewWithFallback(
            viewBinding.ivPlantMain,
            imageUrl,
            fallbackUrl,
        )
    }

    private fun loadImageViewWithFallback(
        imageView: android.widget.ImageView,
        imageUrl: String?,
        fallbackUrl: String?,
    ) {
        imageView.loadImageWithGithub(
            imageUrl,
            onFailed = {
                fallbackUrl?.let { fallback ->
                    imageView.loadImageWithGithub(fallback)
                }
            },
        )
    }
}
