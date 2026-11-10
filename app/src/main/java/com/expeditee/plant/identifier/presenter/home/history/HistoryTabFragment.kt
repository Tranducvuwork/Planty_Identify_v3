package com.expeditee.plant.identifier.presenter.home.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.FragmentHistoryTabBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.loadImageWithGlide
import com.expeditee.plant.identifier.thesis.HistoryImages
import com.expeditee.plant.identifier.thesis.PlantImages
import com.expeditee.plant.identifier.thesis.PlantScanSession
import com.expeditee.plant.identifier.thesis.ScanHistoryItem
import com.expeditee.plant.identifier.thesis.ThesisLocalStore
import com.expeditee.plant.identifier.thesis.ThesisPlantCatalog
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailActivity
import com.expeditee.plant.identifier.thesis.ThesisPlantEditActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryTabViewModel(
    application: android.app.Application,
    private val store: ThesisLocalStore,
) : BaseViewModel<HistoryTabState>(application) {
    private val _state = MutableStateFlow(HistoryTabState())
    val uiState = _state.asStateFlow()

    override fun onState(state: HistoryTabState) {
        _state.value = state
    }

    fun refresh(favoritesOnly: Boolean = false) {
        val items = if (favoritesOnly) store.listFavorites() else store.listHistory()
        onState(HistoryTabState(items = items, favoritesOnly = favoritesOnly))
    }

    fun toggleFavorite(id: String) {
        store.toggleFavorite(id)
        refresh(_state.value.favoritesOnly)
    }

    fun clearAll() {
        store.clearHistory()
        refresh(false)
    }

    fun open(item: ScanHistoryItem) {
        val data = ThesisPlantCatalog.findByName(
            getApplication(),
            item.scientificName,
            listOf(item.commonName),
        )
        if (data != null) {
            PlantScanSession.clear()
            val result = store.applyOverride(ThesisPlantCatalog.toResult(data))
            PlantScanSession.store(result, item.imagePath.ifBlank { result.imageUrl })
            onState(_state.value.copy(opened = true))
        } else {
            val override = store.findOverride(item.scientificName, item.commonName)
            if (override != null) {
                PlantScanSession.clear()
                PlantScanSession.store(
                    override.applyTo(
                        com.expeditee.plant.identifier.network.model.PlantScanResult(
                            scientificName = item.scientificName.ifBlank { override.scientificName },
                            commonNames = listOf(item.commonName).filter { it.isNotBlank() },
                            confidence = 100.0,
                            imageUrl = item.imagePath,
                            detailAvailable = true,
                        ),
                    ),
                    item.imagePath.ifBlank { override.imagePath },
                )
                onState(_state.value.copy(opened = true))
            } else {
                onState(_state.value.copy(error = "Không tìm thấy chi tiết cây"))
            }
        }
    }
}

data class HistoryTabState(
    val items: List<ScanHistoryItem> = emptyList(),
    val favoritesOnly: Boolean = false,
    val opened: Boolean = false,
    val error: String? = null,
) : BaseViewModel.State

class HistoryTabFragment :
    BaseFragment<FragmentHistoryTabBinding, HistoryTabViewModel, HistoryTabState>(
        { inflater, container, attachToParent ->
            FragmentHistoryTabBinding.inflate(inflater, container, attachToParent)
        },
    ) {

    override fun getLazyViewModel() = viewModel<HistoryTabViewModel>()

    private val adapter = HistoryAdapter(
        onClick = { viewModel.open(it) },
        onFavorite = { viewModel.toggleFavorite(it.id) },
    )

    override fun initViews() {
        applyStatusBarInset()
        viewBinding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.rvHistory.adapter = adapter
        viewBinding.emptyState.tvEmptyTitle.setText(R.string.thesis_history_empty_title)
        viewBinding.emptyState.tvEmptyMessage.setText(R.string.thesis_history_empty)
        viewBinding.emptyState.ivEmpty.setImageResource(R.drawable.ic_history_nav)
        viewBinding.btnClear.setOnClickListener {
            viewModel.clearAll()
            Toast.makeText(requireContext(), R.string.thesis_history_cleared, Toast.LENGTH_SHORT).show()
        }
        viewBinding.btnAddPlant.setOnClickListener {
            ThesisPlantEditActivity.start(requireContext(), create = true)
        }
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
        viewModel.refresh(false)
    }

    override fun initObservers() {
        viewLifecycleOwner.collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            adapter.submit(state.items)
            val empty = state.items.isEmpty()
            viewBinding.emptyState.root.isVisible = empty
            viewBinding.rvHistory.isVisible = !empty
            viewBinding.btnClear.isVisible = !empty
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

class HistoryAdapter(
    private val onClick: (ScanHistoryItem) -> Unit,
    private val onFavorite: (ScanHistoryItem) -> Unit,
) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    private val items = mutableListOf<ScanHistoryItem>()
    private val timeFmt = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

    fun submit(list: List<ScanHistoryItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_scan_history, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ScanHistoryItem) {
            itemView.findViewById<TextView>(R.id.tvName).text = item.commonName
            itemView.findViewById<TextView>(R.id.tvScientific).text = item.scientificName
            itemView.findViewById<TextView>(R.id.tvTime).text = timeFmt.format(Date(item.timestamp))
            val thumb = itemView.findViewById<ImageView>(R.id.ivThumb)
            thumb.loadImageWithGlide(
                resolveThumb(item),
                error = R.drawable.ic_camera_home,
            )
            val fav = itemView.findViewById<ImageView>(R.id.btnFavorite)
            fav.setImageResource(
                if (item.favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline,
            )
            fav.setOnClickListener { onFavorite(item) }
            itemView.setOnClickListener { onClick(item) }
        }

        private fun resolveThumb(item: ScanHistoryItem): Any {
            val path = item.imagePath
            if (path.isNotBlank()) {
                val local = HistoryImages.resolveLocalFile(path)
                if (local != null && local.exists()) return local
                PlantImages.resolve(itemView.context, path)?.let { return it }
                ThesisPlantCatalog.imageModel(path)?.let { return it }
            }
            val catalog = ThesisPlantCatalog.findByName(
                itemView.context,
                item.scientificName,
                listOf(item.commonName),
            )
            return PlantImages.resolve(itemView.context, catalog?.imageUrl)
                ?: ThesisPlantCatalog.imageModel(catalog?.imageUrl)
                ?: R.drawable.ic_camera_home
        }
    }
}
