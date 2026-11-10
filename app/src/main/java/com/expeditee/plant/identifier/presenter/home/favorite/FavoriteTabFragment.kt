package com.expeditee.plant.identifier.presenter.home.favorite

import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentFavoriteTabBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.presenter.home.history.HistoryAdapter
import com.expeditee.plant.identifier.presenter.home.history.HistoryTabState
import com.expeditee.plant.identifier.presenter.home.history.HistoryTabViewModel
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteTabFragment :
    BaseFragment<FragmentFavoriteTabBinding, HistoryTabViewModel, HistoryTabState>(
        { inflater, container, attachToParent ->
            FragmentFavoriteTabBinding.inflate(inflater, container, attachToParent)
        },
    ) {

    override fun getLazyViewModel() = viewModel<HistoryTabViewModel>()

    private val adapter = HistoryAdapter(
        onClick = { viewModel.open(it) },
        onFavorite = { viewModel.toggleFavorite(it.id) },
    )

    override fun initViews() {
        applyStatusBarInset()
        viewBinding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.rvFavorites.adapter = adapter
        viewBinding.emptyState.tvEmptyTitle.setText(R.string.thesis_favorite_empty_title)
        viewBinding.emptyState.tvEmptyMessage.setText(R.string.thesis_favorite_empty)
        viewBinding.emptyState.ivEmpty.setImageResource(R.drawable.ic_favorite_outline)
    }

    private fun applyStatusBarInset() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            insets
        }
        ViewCompat.requestApplyInsets(viewBinding.root)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh(favoritesOnly = true)
    }

    override fun initObservers() {
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            adapter.submit(state.items)
            val empty = state.items.isEmpty()
            viewBinding.emptyState.root.isVisible = empty
            viewBinding.rvFavorites.isVisible = !empty
            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
            if (state.opened) {
                viewModel.onState(state.copy(opened = false))
                ThesisPlantDetailActivity.start(requireContext())
            }
        }
    }
}
