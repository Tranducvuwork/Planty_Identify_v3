package com.expeditee.plant.identifier.presenter.disease_list

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityDiseaseListBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.model.PestDisease
import com.expeditee.plant.identifier.presenter.disease_detail.DiseaseDetailActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class DiseaseListActivity :
    BaseActivity<ActivityDiseaseListBinding, DiseaseListViewModel, DiseaseListState>() {

    private val diseaseAdapter by lazy {
        DiseaseListAdapter { disease ->
            navigateToDiseaseDetail(disease)
        }
    }

    override fun isFitsSystemWindows(): Boolean = false
    override fun getLazyViewModel() = viewModel<DiseaseListViewModel>()
    override fun provideLayout() = R.layout.activity_disease_list

    override fun initViews(savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupSearchBar()
        viewModel.onState(DiseaseListState.LoadDiseases)
        applyInset()
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.let {
            it.setTitle(R.string.common_diseases)
            it.setDisplayHomeAsUpEnabled(true)
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.state) { state ->
            when (state) {
                DiseaseListUiState.Loading -> {
                    viewBinding.loadingState.root.visibility = View.VISIBLE
                }

                is DiseaseListUiState.Content -> {
                    viewBinding.loadingState.root.visibility = View.GONE
                    showDiseases(state.diseases)
                }

                is DiseaseListUiState.Error -> {
                    viewBinding.loadingState.root.visibility = View.GONE
                    showDiseases(state.diseases)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        viewBinding.rvDiseases.apply {
            layoutManager = LinearLayoutManager(this@DiseaseListActivity)
            adapter = diseaseAdapter
        }
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    private fun setupSearchBar() {
        viewBinding.searchEditText.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            viewModel.onState(DiseaseListState.SearchDiseases(query))
            viewBinding.removeTextIc.visibility =
                if (query.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewBinding.removeTextIc.setOnClickListener {
            viewBinding.searchEditText.setText("")
        }
    }

    private fun showDiseases(diseases: List<PestDisease>) {
        viewBinding.tvEmptyState.visibility = if (diseases.isEmpty()) View.VISIBLE else View.GONE
        viewBinding.rvDiseases.visibility = if (diseases.isEmpty()) View.GONE else View.VISIBLE
        diseaseAdapter.submitList(diseases)
    }

    private fun navigateToDiseaseDetail(disease: PestDisease) {
        DiseaseDetailActivity.start(
            context = this,
            diseaseId = disease.id,
            imagePath = disease.firstImage(),
            probability = 0,
            solution = null,
        )
    }
}
