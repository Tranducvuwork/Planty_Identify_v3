package com.expeditee.plant.identifier.presenter.careguide

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityCareGuidesBinding
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.extension.statusBars

class CareGuidesActivity :
    BaseActivity<ActivityCareGuidesBinding, CareGuidesViewModel, CareGuidesState>() {

    private val careGuideAdapter by lazy {
        CareGuidesAdapter { plant ->
            navigateToCareGuideDetail(plant)
        }
    }

    override fun isFitsSystemWindows(): Boolean = false

    override fun getLazyViewModel() = viewModel<CareGuidesViewModel>()

    override fun provideLayout(): Int = R.layout.activity_care_guides

    override fun initViews(savedInstanceState: Bundle?) {
        setupRecyclerView()
        viewModel.onState(CareGuidesState.LoadGuides)
        applyInset()
        setupToolbar()
    }

    override fun initListeners() {
        super.initListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.care_guides)
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun initObservers() {
        super.initObservers()

        // Observe paging data
        lifecycleScope.launch {
            viewModel.careGuides.collectLatest { pagingData ->
                careGuideAdapter.submitData(pagingData)
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            careGuideAdapter.loadStateFlow.collect { loadState ->
                val isLoading = loadState.refresh is LoadState.Loading
                val isEmpty =
                    loadState.refresh is LoadState.NotLoading && careGuideAdapter.itemCount == 0

                viewBinding.loadingState.root.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        viewBinding.rvCareGuides.apply {
            layoutManager = LinearLayoutManager(this@CareGuidesActivity)
            adapter = careGuideAdapter
        }
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    private fun navigateToCareGuideDetail(plant: SuggestedPlant) {
        plant.scientificName?.let { scientificName ->
            CareGuideDetailActivity.start(this, scientificName)
        }
    }
}

