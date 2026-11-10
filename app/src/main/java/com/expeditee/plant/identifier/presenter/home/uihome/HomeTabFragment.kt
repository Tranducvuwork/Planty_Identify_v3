package com.expeditee.plant.identifier.presenter.home.uihome

import android.Manifest
import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentHomeTabBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.hideKeyboard
import com.expeditee.plant.identifier.extension.loadImageWithGlide
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.WeatherInfo
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.chat.PlantAssistantChatActivity
import com.expeditee.plant.identifier.presenter.home.search.SearchActivity
import com.expeditee.plant.identifier.thesis.FolkRemediesActivity
import com.expeditee.plant.identifier.thesis.MedicinalCatalogActivity
import com.expeditee.plant.identifier.thesis.PlantImages
import com.expeditee.plant.identifier.thesis.PlantDiaryListActivity
import com.expeditee.plant.identifier.thesis.PlantGroup
import com.expeditee.plant.identifier.thesis.SymptomRemedyActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeTabFragment :
    BaseFragment<FragmentHomeTabBinding, HomeTabViewModel, HomeTabViewModel.HomeState>(
        { inflater, container, attachToParent ->
            FragmentHomeTabBinding.inflate(inflater, container, attachToParent)
        }
    ) {

    override fun getLazyViewModel() = viewModel<HomeTabViewModel>()


    private val useNewUi = configManagement.enableNewHomeUi

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.entries.any { it.value }
        if (isGranted) {
            startWeatherLoad()
        } else {
            setupPermissionRequiredUI()
        }
    }

    override fun initViews() {
        applyHomeUiVariant()
        setupDefaultWeatherUI()
        viewModel.initLocationClient(requireContext())
        checkPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkUnitChanged(requireContext())
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        viewBinding.layoutPremiumOld.root.isVisible = false
        viewBinding.layoutPremiumNew.root.isVisible = useNewUi
        viewBinding.weatherHomeArea.ivPremiumBadge.show(false)
    }

    override fun initObservers() {
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.homeState) { state ->
            when (state) {
                is HomeTabViewModel.HomeState.Loading -> {}
                is HomeTabViewModel.HomeState.Success -> {}
                is HomeTabViewModel.HomeState.Error -> {}
            }
        }

        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.weatherState) { state ->
            when (state) {
                is HomeTabViewModel.WeatherState.Initial -> setupDefaultWeatherUI()
                is HomeTabViewModel.WeatherState.Loading -> handleWeatherLoading()
                is HomeTabViewModel.WeatherState.PermissionRequired -> setupPermissionRequiredUI()
                is HomeTabViewModel.WeatherState.Success -> updateWeatherUI(state.data)
                is HomeTabViewModel.WeatherState.Error -> {
                    if (!state.locationName.isNullOrBlank()) {
                        showLocationOnly(state.locationName)
                    } else {
                        setupDefaultWeatherUI()
                    }
                }
            }
        }
    }


    override fun initListeners() {
        with(viewBinding) {
            weatherHomeArea.root.onClickWithScale {
                checkPermissionAndLoad()
            }

            flChat.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    requireContext().startActivity<PlantAssistantChatActivity>()
                })
            }

            layoutPremiumNew.clPremium.onClickWithScale {
                val safeContext = context ?: requireContext()
                safeContext.startActivity<ScanCameraActivity>()
            }
            layoutPremiumOld.clPremium.onClickWithScale {
                val safeContext = context ?: requireContext()
                safeContext.startActivity<ScanCameraActivity>()
            }
            weatherHomeArea.ivPremiumBadge.show(false)
            layoutFeaturesNew.cardIdentify.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    startActivity(Intent(requireContext(), ScanCameraActivity::class.java).apply {
                        putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.IDENTIFY)
                    })
                })
            }
            layoutFeaturesOld.cardIdentify.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    startActivity(Intent(requireContext(), ScanCameraActivity::class.java).apply {
                        putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.IDENTIFY)
                    })
                })
            }
            layoutFeaturesNew.cardDiagnose.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    startActivity(Intent(requireContext(), ScanCameraActivity::class.java).apply {
                        putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.DIAGNOSE)
                    })
                })
            }
            layoutFeaturesOld.cardDiagnose.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    startActivity(Intent(requireContext(), ScanCameraActivity::class.java).apply {
                        putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.DIAGNOSE)
                    })
                })
            }
            layoutThesisExtras.btnDiary.onClickWithScale {
                PlantDiaryListActivity.start(requireContext())
            }
            layoutThesisExtras.cardEducation.onClickWithScale {
                MedicinalCatalogActivity.start(requireContext(), PlantGroup.EDUCATION.key)
            }
            layoutThesisExtras.cardMedicine.onClickWithScale {
                MedicinalCatalogActivity.start(requireContext(), PlantGroup.MEDICINE.key)
            }
            layoutThesisExtras.cardAgriculture.onClickWithScale {
                MedicinalCatalogActivity.start(requireContext(), PlantGroup.AGRICULTURE.key)
            }
            layoutThesisExtras.cardMushroom.onClickWithScale {
                MedicinalCatalogActivity.start(requireContext(), PlantGroup.MUSHROOM.key)
            }
            layoutThesisExtras.ivEducation.let {
                PlantImages.load(it, "plant_images/cay-bang.jpg")
            }
            layoutThesisExtras.ivMedicine.let {
                PlantImages.load(it, "plant_images/diep-ca.jpg")
            }
            layoutThesisExtras.ivAgriculture.let {
                PlantImages.load(it, "plant_images/lua.jpg")
            }
            layoutThesisExtras.ivMushroom.let {
                PlantImages.load(it, "plant_images/nam-so.jpg")
            }
            layoutThesisExtras.btnFolk.onClickWithScale {
                FolkRemediesActivity.start(requireContext())
            }
            layoutThesisExtras.btnSymptom.onClickWithScale {
                SymptomRemedyActivity.start(requireContext())
            }
            etSearchPlants.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    context?.startActivity<SearchActivity>()
                })
            }
            btnVoiceSearch.onClickWithScale {
                showInterAdBeforeDoAction(Runnable {
                    SearchActivity.start(requireContext(), launchVoice = true)
                })
            }
            etSearchPlants.apply {
                isLongClickable = false
                setTextIsSelectable(false)
                isFocusable = false
                isFocusableInTouchMode = false
            }

            root.onDebounceClick {
                etSearchPlants.hideKeyboard()
                etSearchPlants.clearFocus()
            }
        }
    }


    private fun checkPermissionAndLoad() {
        if (viewModel.hasLocationPermission(requireContext())) {
            startWeatherLoad()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startWeatherLoad() {
        viewModel.fetchLocation(requireContext())
    }

    private fun setupDefaultWeatherUI() {
        with(viewBinding.weatherHomeArea) {
            tvTemperature.text = ""
            tvMinTemperature.text = ""
            tvMaxTemperature.text = ""
            tvLocation.text = getString(R.string.location_unavailable)
            ivWeatherIcon.visibility = View.GONE
            tvTap.visibility = View.VISIBLE
        }
    }

    private fun setupPermissionRequiredUI() {
        with(viewBinding.weatherHomeArea) {
            tvTemperature.text = ""
            tvMinTemperature.text = ""
            tvMaxTemperature.text = ""
            tvLocation.text = getString(R.string.tap_to_enable_location_and_get_weather_updates)
            ivWeatherIcon.visibility = View.GONE
            tvTap.visibility = View.VISIBLE
        }
    }

    private fun handleWeatherLoading() {
        with(viewBinding.weatherHomeArea) {
            tvTemperature.text = ""
            tvMinTemperature.text = ""
            tvMaxTemperature.text = ""
            tvLocation.text = ""
            ivWeatherIcon.beVisible()
            tvTap.beGone()
        }
    }

    private fun showLocationOnly(locationName: String) {
        with(viewBinding.weatherHomeArea) {
            tvTemperature.text = ""
            tvMinTemperature.text = ""
            tvMaxTemperature.text = ""
            tvLocation.text = locationName
            ivWeatherIcon.visibility = View.GONE
            tvTap.visibility = View.VISIBLE
        }
    }

    private fun updateWeatherUI(info: WeatherInfo) {
        with(viewBinding.weatherHomeArea) {
            tvTemperature.text = info.temperature
            tvMinTemperature.text = info.minTemperature
            tvMaxTemperature.text = info.maxTemperature
            tvLocation.text = info.location
            ivWeatherIcon.setImageResource(info.weatherIcon)
            ivWeatherIcon.beVisible()
            tvTap.beGone()
        }
    }

    private fun applyHomeUiVariant() {
        // "Premium" banners used as identify CTA; hide paywall-only old strip
        viewBinding.layoutPremiumNew.root.isVisible = useNewUi
        viewBinding.layoutPremiumOld.root.isVisible = false
        viewBinding.layoutFeaturesNew.root.isVisible = true
        viewBinding.layoutFeaturesOld.root.isVisible = false
        viewBinding.weatherHomeArea.ivPremiumBadge.show(false)
    }
}
