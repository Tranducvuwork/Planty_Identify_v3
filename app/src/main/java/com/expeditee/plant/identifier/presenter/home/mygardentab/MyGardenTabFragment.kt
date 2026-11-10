package com.expeditee.plant.identifier.presenter.home.mygardentab

import android.graphics.Color
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentMyGardenTabBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.gone
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.helper.SiteManager
import com.expeditee.plant.identifier.model.GardenStats
import com.expeditee.plant.identifier.model.HealthStats
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.detail.PlantDetailActivity
import com.expeditee.plant.identifier.presenter.home.managementsite.ManagementSiteActivity
import com.expeditee.plant.identifier.presenter.home.search.SearchActivity
import com.expeditee.plant.identifier.widget.bottomsheet.DeleteConfirmDialog
import com.expeditee.plant.identifier.widget.bottomsheet.EditReminderBottomSheetDialogFragment
import com.expeditee.plant.identifier.widget.bottomsheet.ManagementSiteBottomSheetDialog
import com.expeditee.plant.identifier.widget.bottomsheet.MoveSuccessDialog
import com.expeditee.plant.identifier.widget.bottomsheet.MoveToBottomSheetDialog
import com.expeditee.plant.identifier.widget.bottomsheet.SelectPlantBottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MyGardenTabFragment :
    BaseFragment<FragmentMyGardenTabBinding, MyGardenTabViewModel, MyGardenTabState>(
        inflate = { layoutInflater, viewGroup, attachToParent ->
            DataBindingUtil.inflate(
                layoutInflater, R.layout.fragment_my_garden_tab, viewGroup, attachToParent
            )
        }) {

    override fun getLazyViewModel() = activityViewModel<MyGardenTabViewModel>()

    private var selectedSiteId: String = SiteManager.ALL_SITE_ID

    private val myGardenPlantAdapter by lazy {
        MyGardenPlantAdapter(
            onItemClick = { plant ->
                showInterAdBeforeDoAction {
                    PlantDetailActivity.start(
                        context = requireContext(),
                        plantId = plant.id.toString(),
                        plantName = plant.name,
                        scientificName = plant.scientificName,
                        imageUrl = plant.imageFile,
                        gardenId = plant.gardenId
                    )
                }
            },
            onMove = { plant ->
                MoveToBottomSheetDialog.newInstance(plant)
                    .setOnMoveConfirmed { selectedPlant, gardenOption ->
                        viewModel.onState(MyGardenTabState.MovePlant(selectedPlant.copy(gardenId = gardenOption.id)))
                        MoveSuccessDialog(requireContext()).show()
                    }
                    .show(parentFragmentManager)
            },
            onDelete = { plant ->
                DeleteConfirmDialog(requireContext()) {
                    viewModel.onState(MyGardenTabState.DeletePlant(plant))
                }.show()
            },
            onAddReminder = { plant ->
                val prefilledReminder = ReminderScheduleModel(
                    plantId = plant.id,
                    plantName = plant.name,
                    imagePath = plant.imageFile,
                    remindType = getString(R.string.reminder_watering),
                    remindTime = "09:00",
                    repeatType = getString(R.string.reminder_every_day)
                )
                EditReminderBottomSheetDialogFragment.newInstance(prefilledReminder)
                    .show(parentFragmentManager)
            }
        )
    }

    override fun initViews() {
        setupTabs()
        setupRecyclerView()

    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.selectedGardenId) { gardenId ->
            selectedSiteId = gardenId ?: SiteManager.ALL_SITE_ID
            val label = resolveSiteLabel(gardenId)
            updateSelectedSiteLabel(label)
        }

        collectLatestRepeatOnLifecycle(viewModel.myPlants) { pagingData ->
            myGardenPlantAdapter.submitData(pagingData)
        }

        myGardenPlantAdapter.addLoadStateListener { loadState ->
            val isEmpty =
                loadState.source.refresh is LoadState.NotLoading && myGardenPlantAdapter.itemCount == 0
            viewBinding.layoutEmpty.show(isEmpty)
            viewBinding.rvMyGarden.gone(isEmpty)
        }

        collectLatestRepeatOnLifecycle(viewModel.gardenStats) { stats ->
            bindGardenStats(stats)
        }

        collectLatestRepeatOnLifecycle(viewModel.healthStats) { health ->
            bindHealthStats(health)
        }
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.ivPremiumBadge.show(false)
    }

    private fun resolveSiteLabel(gardenId: String?): String {
        if (gardenId == null) return getString(R.string.filter_all)
        if (gardenId == SiteManager.DEFAULT_SITE_ID) return getString(R.string.my_garden_default)
        val sites = SiteManager.getSites(requireContext())
        return sites.firstOrNull { it.id == gardenId }?.name ?: getString(R.string.filter_all)
    }

    private fun bindGardenStats(stats: GardenStats) {
        with(viewBinding) {
            tvBadge.text = getString(
                R.string.garden_badge_format,
                stats.totalPlants,
                stats.totalFlowers
            )
            tvTotalCount.text = getString(
                R.string.total_count_format,
                stats.totalCount
            )
        }
    }

    private fun bindHealthStats(health: HealthStats) {
        with(viewBinding) {
            tvHealthSub.text = getString(
                R.string.health_count_format,
                health.healthyCount
            )
            val unhealthyTextView = llUnhealthy.getChildAt(1) as? android.widget.TextView
            unhealthyTextView?.text = getString(
                R.string.unhealthy_count_format,
                health.unhealthyCount
            )
        }
    }

    private fun setupTabs() {
        viewBinding.btnTabPlant.onClickWithScale {
            selectTab(isPlant = true)
        }
        viewBinding.btnTabFlower.onClickWithScale {
            selectTab(isPlant = false)
        }
        viewBinding.layoutFilter.onClickWithScale {
            ManagementSiteBottomSheetDialog()
                .setSelectedSiteId(selectedSiteId)
                .setOnSiteSelected { selectedSite ->
                    val filterId =
                        if (selectedSite.id == SiteManager.ALL_SITE_ID) null else selectedSite.id
                    viewModel.onState(MyGardenTabState.SelectGarden(filterId))
                }
                .setOnManagementClick {
                    ManagementSiteActivity.start(requireContext())
                }
                .show(parentFragmentManager)
        }
        viewBinding.ivAdd.onClickWithScale {
            val addGardenId =
                if (selectedSiteId == SiteManager.ALL_SITE_ID) SiteManager.DEFAULT_SITE_ID else selectedSiteId
            showSelectPlant(addGardenId)
        }
    }

    private fun showSelectPlant(gardenId: String) {
        val dialog = SelectPlantBottomSheetDialog.newInstance(gardenId)
            .setOnIdentifyByPhotoListener { id ->
                val safeContext = context ?: return@setOnIdentifyByPhotoListener
                ScanCameraActivity.start(safeContext, gardenId = id)
            }
            .setOnSearchByNameListener { id ->
                val safeContext = context ?: return@setOnSearchByNameListener
                SearchActivity.start(safeContext, gardenId = id)
            }
        dialog.show(parentFragmentManager)
    }

    private fun selectTab(isPlant: Boolean) {
        if (isPlant) {
            viewBinding.btnTabPlant.setBackgroundResource(R.drawable.bg_tab_selected)
            viewBinding.btnTabPlant.setTextColor(Color.WHITE)
            viewBinding.btnTabFlower.setBackground(null)
            viewBinding.btnTabFlower.setTextColor(Color.GRAY)
            viewModel.onState(MyGardenTabState.SelectType(PopularPlantModel.TYPE_PLANT))
        } else {
            viewBinding.btnTabFlower.setBackgroundResource(R.drawable.bg_tab_selected)
            viewBinding.btnTabFlower.setTextColor(Color.WHITE)
            viewBinding.btnTabPlant.setBackground(null)
            viewBinding.btnTabPlant.setTextColor(Color.GRAY)
            viewModel.onState(MyGardenTabState.SelectType(PopularPlantModel.TYPE_FLOWER))
        }
    }

    private fun setupRecyclerView() {
        with(viewBinding.rvMyGarden) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myGardenPlantAdapter
        }
    }

    private fun updateSelectedSiteLabel(siteName: String) {
        viewBinding.tvFilterLabel.text = siteName
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        viewBinding.ivPremiumBadge.show(false)
    }
}