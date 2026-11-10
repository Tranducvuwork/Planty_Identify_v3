package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityMedicinalCatalogBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.network.model.PlantListItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class MedicinalCatalogViewModel(
    application: android.app.Application,
) : BaseViewModel<MedicinalCatalogState>(application) {
    private val _state = MutableStateFlow(MedicinalCatalogState())
    val uiState = _state.asStateFlow()

    override fun onState(state: MedicinalCatalogState) {
        _state.value = state
    }

    fun load(groupKey: String = PlantGroup.MEDICINE.key) {
        onState(MedicinalCatalogState(loading = true, groupKey = groupKey))
        launchBlock(Dispatchers.IO) {
            val group = PlantGroup.fromKey(groupKey) ?: PlantGroup.MEDICINE
            val items = ThesisPlantCatalog.byGroup(getApplication(), group).map { plant ->
                PlantListItemData(
                    plantId = plant.plantId,
                    qrCode = plant.qrCode,
                    scientificName = plant.scientificName,
                    commonNames = plant.commonNames,
                    isMedicinal = plant.isMedicinal,
                    medicinalLabel = plant.medicinalLabel,
                    warningLevel = plant.toxicity?.warningLevel,
                    imageUrl = plant.imageUrl,
                )
            }
            onState(MedicinalCatalogState(items = items, groupKey = groupKey))
        }
    }

    fun openPlant(plantId: String) {
        val data = ThesisPlantCatalog.findByQrOrId(getApplication(), plantId)
        if (data != null) {
            PlantScanSession.clear()
            PlantScanSession.store(ThesisPlantCatalog.toResult(data))
            onState(_state.value.copy(opened = true, error = null))
        } else {
            onState(_state.value.copy(error = "Không tìm thấy cây"))
        }
    }
}

data class MedicinalCatalogState(
    val items: List<PlantListItemData> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val opened: Boolean = false,
    val groupKey: String = PlantGroup.MEDICINE.key,
) : BaseViewModel.State

class MedicinalCatalogActivity :
    BaseActivity<ActivityMedicinalCatalogBinding, MedicinalCatalogViewModel, MedicinalCatalogState>() {

    companion object {
        private const val EXTRA_GROUP = "extra_group"

        fun start(context: Context, groupKey: String = PlantGroup.MEDICINE.key) {
            context.startActivity(
                Intent(context, MedicinalCatalogActivity::class.java).putExtra(EXTRA_GROUP, groupKey),
            )
        }
    }

    private val adapter = CatalogAdapter { item ->
        val id = item.plantId.orEmpty()
        if (id.isNotBlank()) viewModel.openPlant(id)
    }

    override fun provideLayout() = R.layout.activity_medicinal_catalog
    override fun getLazyViewModel() = viewModel<MedicinalCatalogViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        val group = PlantGroup.fromKey(intent.getStringExtra(EXTRA_GROUP)) ?: PlantGroup.MEDICINE
        viewBinding.toolbar.title = group.label
        viewBinding.rvPlants.layoutManager = LinearLayoutManager(this)
        viewBinding.rvPlants.setHasFixedSize(true)
        viewBinding.rvPlants.setItemViewCacheSize(10)
        viewBinding.rvPlants.adapter = adapter
        viewModel.load(group.key)
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            adapter.submit(state.items)
            state.error?.let {
                Toast.makeText(this@MedicinalCatalogActivity, it, Toast.LENGTH_SHORT).show()
            }
            if (state.opened) {
                viewModel.onState(state.copy(opened = false))
                ThesisPlantDetailActivity.start(this@MedicinalCatalogActivity)
            }
        }
    }
}

private class CatalogAdapter(
    private val onClick: (PlantListItemData) -> Unit,
) : RecyclerView.Adapter<CatalogAdapter.VH>() {
    private val items = mutableListOf<PlantListItemData>()
    fun submit(list: List<PlantListItemData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thesis_plant_row, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun getItemId(position: Int): Long =
        items[position].plantId.orEmpty().hashCode().toLong()

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], onClick)

    override fun onViewRecycled(holder: VH) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    init {
        setHasStableIds(true)
    }

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        private val thumb = itemView.findViewById<ImageView>(R.id.ivThumb)
        private val title = itemView.findViewById<TextView>(R.id.tvTitle)
        private val subtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)

        fun bind(item: PlantListItemData, onClick: (PlantListItemData) -> Unit) {
            val name = item.commonNames?.firstOrNull().orEmpty().ifBlank { item.scientificName }
            val label = item.medicinalLabel?.takeIf { it.isNotBlank() }
                ?: if (item.isMedicinal == true) "Cây thuốc" else "Thực vật"
            title.text = "$name — $label"
            subtitle.text = item.scientificName.orEmpty()
            PlantImages.loadThumb(thumb, item.imageUrl)
            val level = item.warningLevel.orEmpty().lowercase()
            val mushroomLabel = label.lowercase()
            val tint = when {
                level == "danger" || "độc" in mushroomLabel ->
                    Color.parseColor("#FFCDD2")
                "ăn được" in mushroomLabel || "an duoc" in mushroomLabel ->
                    Color.parseColor("#C8E6C9")
                else -> null
            }
            itemView.backgroundTintList = tint?.let { ColorStateList.valueOf(it) }
            itemView.setOnClickListener { onClick(item) }
        }

        fun clear() {
            Glide.with(itemView).clear(thumb)
        }
    }
}
