package com.expeditee.plant.identifier.presenter.careguide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityCareGuideDetailBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.model.reponse.CareGuide
import com.expeditee.plant.identifier.model.reponse.GrowthCultivation
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class CareGuideDetailActivity :
    BaseActivity<ActivityCareGuideDetailBinding, CareGuideDetailViewModel, CareGuideDetailState>() {

    companion object {
        private const val EXTRA_SCIENTIFIC_NAME = "extra_scientific_name"

        fun start(context: Context, scientificName: String) {
            val intent = Intent(context, CareGuideDetailActivity::class.java).apply {
                putExtra(EXTRA_SCIENTIFIC_NAME, scientificName)
            }
            context.startActivity(intent)
        }
    }

    override fun provideLayout() = R.layout.activity_care_guide_detail
    override fun getLazyViewModel() = viewModel<CareGuideDetailViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        setupToolbar()
        applyInset()
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME).orEmpty()
        if (scientificName.isBlank()) {
            finish()
            return
        }
        viewModel.onState(CareGuideDetailState.LoadPlantDetails(scientificName))
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
             adsManager.consumeNativeAd(this@CareGuideDetailActivity, viewBinding.nativeAdView)
        }
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
        if (!hasPurchase) return
        viewBinding.nativeAdView.removeAllViews()
        viewBinding.nativeAdView.beGone()
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun initObservers() {
        super.initObservers()
        viewModel.isLoading.observe(this) {
            viewBinding.loadingState.root.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.plantData.observe(this) { plant ->
            if (plant == null) return@observe
            bindPlant(plant)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.care_guides)
        }
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    private fun bindPlant(plant: SuggestedPlant) {
        supportActionBar?.title = plant.commonNames?.firstOrNull() ?: plant.scientificName
        val imageUrl = plant.mainImage?.urlSmall ?: plant.mainImage?.url
        if (imageUrl.isNullOrBlank()) {
            viewBinding.ivPlantImage.setImageResource(R.drawable.ic_placeholder_loading)
        } else {
            viewBinding.ivPlantImage.loadImageWithGithub(imageUrl)
        }
        bindLightAndEnvironment(plant.growthCultivation)
        bindWateringAndSoil(plant.growthCultivation, plant.careGuide)
        bindCareAndMaintenance(plant.careGuide)
    }

    private fun bindLightAndEnvironment(cultivation: GrowthCultivation?) {
        if (cultivation == null) return
        val sunlightText = cultivation.sunlight?.guide
            ?: cultivation.sunlight?.type?.joinToString(", ")
            ?: "-"
        setTranslatedText(viewBinding.sunlightContent, sunlightText)
        setTranslatedText(
            viewBinding.lightRequirementsContent,
            cultivation.lightRequirements ?: "-"
        )
        setTranslatedText(viewBinding.temperatureContent, cultivation.temperatureRange ?: "-")
        val humidityText = when {
            cultivation.humidity?.min != null && cultivation.humidity.max != null ->
                "${cultivation.humidity.min}-${cultivation.humidity.max}%"

            else -> "-"
        }
        setTranslatedText(viewBinding.humidityContent, humidityText)
    }

    private fun bindWateringAndSoil(cultivation: GrowthCultivation?, careGuide: CareGuide?) {
        val wateringText = cultivation?.watering?.guide ?: cultivation?.watering?.frequency ?: "-"
        setTranslatedText(viewBinding.wateringContent, wateringText)
        setTranslatedText(viewBinding.wateringScheduleContent, careGuide?.wateringSchedule ?: "-")
        setTranslatedText(viewBinding.soilTypeContent, cultivation?.soilType ?: "-")
    }

    private fun bindCareAndMaintenance(careGuide: CareGuide?) {
        setTranslatedText(viewBinding.pruningScheduleContent, careGuide?.pruning ?: "-")
        setTranslatedText(viewBinding.fertilizingContent, careGuide?.fertilizing ?: "-")
        setTranslatedText(viewBinding.repottingContent, careGuide?.repotting ?: "-")
        setTranslatedText(viewBinding.pestControlContent, careGuide?.pestControl ?: "-")
        setTranslatedText(viewBinding.seasonalCareContent, careGuide?.seasonalCare ?: "-")
    }

    private fun setTranslatedText(textView: android.widget.TextView, value: String) {
        textView.text = value
    }

    // Native callbacks are managed/removed inside LammaAdManager.loadNativeByN110Config(...)
}
