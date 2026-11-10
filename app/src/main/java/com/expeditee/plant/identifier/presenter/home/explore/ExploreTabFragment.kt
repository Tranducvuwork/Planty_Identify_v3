package com.expeditee.plant.identifier.presenter.home.explore

import androidx.databinding.DataBindingUtil
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentExploreTabBinding
import com.expeditee.plant.identifier.extension.GridSpacingItemDecoration
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.presenter.detail.PlantDetailActivity
import com.expeditee.plant.identifier.presenter.home.managementsite.SitePlantsActivity
import com.expeditee.plant.identifier.presenter.home.search.SearchActivity
import com.expeditee.plant.identifier.model.ExploreCategoryModel
import com.expeditee.plant.identifier.model.PlantCategory
import com.expeditee.plant.identifier.thesis.OrchidVariety
import com.expeditee.plant.identifier.thesis.OrchidVarietyCatalog
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailActivity
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExploreTabFragment :
    BaseFragment<FragmentExploreTabBinding, ExploreTabViewModel, ExploreTabState>(
        inflate = { layoutInflater, viewGroup, attachToParent ->
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.fragment_explore_tab,
                viewGroup,
                attachToParent,
            )
        },
    ) {

    override fun getLazyViewModel() = activityViewModel<ExploreTabViewModel>()

    private val popularPlantAdapter by lazy { PopularPlantAdapter() }

    override fun initViews() {
        setupRecyclerViewCategory()
        setupRecyclerViewOrchidVarieties()
        setupRecyclerViewPopular()
        popularPlantAdapter.onPlantClick = { plant ->
            showInterAdBeforeDoAction {
                PlantDetailActivity.start(
                    context = requireContext(),
                    plantId = plant.id,
                    plantName = plant.name,
                    scientificName = plant.scientificName,
                    imageUrl = plant.imageUrl,
                    allowResolveFallback = true,
                )
            }
        }
    }

    override fun initListeners() {
        super.initListeners()

        viewBinding.ivBack.onDebounceClick {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        viewBinding.etSearchPlants.isFocusable = false
        viewBinding.etSearchPlants.isFocusableInTouchMode = false
        viewBinding.etSearchPlants.setOnClickListener {
            SearchActivity.start(requireContext())
        }

        viewBinding.ivPremiumBadge.show(false)
    }

    override fun initObservers() {
        super.initObservers()
        observePopularPlants()
        collectLatestRepeatOnLifecycle(viewModel.errorEvent) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        collectLatestRepeatOnLifecycle(viewModel.isLoading) { isLoading ->
            viewBinding.pbLoading.show(isLoading)
        }
    }

    private fun setupRecyclerViewCategory() {
        viewBinding.rvCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categoryModels = PlantCategory.entries.map { category ->
            ExploreCategoryModel(
                name = category.getDisplayText(requireContext()),
                key = category.name,
                imageRes = category.getDisplayImageResId(),
            )
        }

        viewBinding.rvCategories.adapter = ExploreCategoryAdapter(categoryModels) { category ->
            val safeContext = context ?: return@ExploreCategoryAdapter
            SearchActivity.start(
                context = safeContext,
                category = category.key
            )
        }
    }

    private fun setupRecyclerViewPopular() {
        with(viewBinding.rvPopularPlants) {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = popularPlantAdapter
        }
    }

    private fun setupRecyclerViewOrchidVarieties() {
        viewBinding.rvOrchidVarieties.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false,
            )
            adapter = OrchidVarietyAdapter(OrchidVarietyCatalog.varieties) { variety ->
                openOrchidVariety(variety)
            }
        }
    }

    private fun openOrchidVariety(variety: OrchidVariety) {
        showInterAdBeforeDoAction {
            ThesisPlantDetailActivity.startWithResult(
                requireContext(),
                OrchidVarietyCatalog.resultFor(variety),
            )
        }
    }

    private fun observePopularPlants() {
        collectLatestRepeatOnLifecycle(viewModel.popularPlants) { plants ->
            if (plants.isEmpty()) {
                viewBinding.rvPopularPlants.beGone()
            } else {
                popularPlantAdapter.submitList(plants)
                viewBinding.rvPopularPlants.beVisible()
            }
        }
    }


    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        viewBinding.ivPremiumBadge.show(false)
    }
}
