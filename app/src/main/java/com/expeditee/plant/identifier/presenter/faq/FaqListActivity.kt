package com.expeditee.plant.identifier.presenter.faq

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityFaqListBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.hideKeyboard
import com.expeditee.plant.identifier.extension.setOnDoneAction
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.presenter.faq.tag.FaqTagActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqListActivity : BaseActivity<ActivityFaqListBinding, FaqListViewModel, FaqListState>() {

    override fun provideLayout() = R.layout.activity_faq_list
    override fun getLazyViewModel() = viewModel<FaqListViewModel>()
    override fun isFitsSystemWindows(): Boolean = false

    private val faqAdapter by lazy {
        FaqAdapter(
            onFaqClick = { FaqDetailActivity.start(this, it) },
            onTagClick = { FaqTagActivity.start(this, it) }
        )
    }

    override fun initViews(savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        applyInset()
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.faqState) { state ->
            when (state) {
                is FaqListState.Idle -> Unit
                is FaqListState.Loading -> Unit
                is FaqListState.FaqLoaded -> {
                    viewBinding.textFaqListMatchingResults.beGone()
                    faqAdapter.submitList(state.list)
                }
                is FaqListState.FaqSearched -> {
                    viewBinding.textFaqListMatchingResults.text = getString(
                        R.string.matching_results_count,
                        state.list.size
                    )
                    viewBinding.textFaqListMatchingResults.beVisible()
                    faqAdapter.submitList(state.list)
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.plant_faq)
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        viewBinding.collectionFaqList.apply {
            layoutManager = LinearLayoutManager(this@FaqListActivity)
            adapter = faqAdapter
        }
    }

    private fun setupSearchBar() {
        with(viewBinding.editTextFaqListSearch) {
            doAfterTextChanged {
                val text = it.toString()
                viewModel.requestFaq(text)
                if (text.isEmpty()) {
                    viewBinding.imageFaqListClearSearch.beGone()
                    viewBinding.toolbar.title = getString(R.string.plant_faq)
                } else {
                    viewBinding.imageFaqListClearSearch.beVisible()
                    viewBinding.toolbar.title = getString(R.string.search)
                }
            }

            setOnDoneAction {
                hideKeyboard()
                clearFocus()
            }
        }

        viewBinding.imageFaqListClearSearch.setOnClickListener {
            viewBinding.editTextFaqListSearch.setText("")
        }
    }
}
