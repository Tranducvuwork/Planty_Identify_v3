package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.databinding.ActivityFolkRemediesBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.network.model.FolkRemedyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class FolkRemediesViewModel(
    application: android.app.Application,
    private val store: ThesisLocalStore,
) : BaseViewModel<FolkRemediesState>(application) {
    private val _state = MutableStateFlow(FolkRemediesState())
    val uiState = _state.asStateFlow()

    override fun onState(state: FolkRemediesState) {
        _state.value = state
    }

    fun load() {
        val items = store.loadFolkFromAssets(getApplication())
        onState(
            FolkRemediesState(
                items = items,
                disclaimer = getApplication<android.app.Application>()
                    .getString(R.string.thesis_medical_disclaimer),
            ),
        )
    }
}

data class FolkRemediesState(
    val items: List<FolkRemedyData> = emptyList(),
    val disclaimer: String = "",
) : BaseViewModel.State

class FolkRemediesActivity :
    BaseActivity<ActivityFolkRemediesBinding, FolkRemediesViewModel, FolkRemediesState>() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, FolkRemediesActivity::class.java))
        }
    }

    private val adapter = FolkAdapter {
        val url = it.sourceUrl?.takeIf { u -> u.isNotBlank() } ?: Constants.DUOC_DIEN_URL
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun provideLayout() = R.layout.activity_folk_remedies
    override fun getLazyViewModel() = viewModel<FolkRemediesViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.rvFolk.layoutManager = LinearLayoutManager(this)
        viewBinding.rvFolk.adapter = adapter
        viewModel.load()
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            viewBinding.tvDisclaimer.text = state.disclaimer
            adapter.submit(state.items)
        }
    }
}

private class FolkAdapter(
    private val onClick: (FolkRemedyData) -> Unit,
) : RecyclerView.Adapter<FolkAdapter.VH>() {
    private val items = mutableListOf<FolkRemedyData>()

    fun submit(list: List<FolkRemedyData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thesis_row, parent, false)
        return VH(tv)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: FolkRemedyData, onClick: (FolkRemedyData) -> Unit) {
            val t1 = itemView.findViewById<TextView>(R.id.tvTitle)
            val t2 = itemView.findViewById<TextView>(R.id.tvSubtitle)
            t1.text = item.title
            t2.text = buildString {
                append(item.ailment.orEmpty())
                append("\n")
                append(item.method.orEmpty())
                if (!item.caution.isNullOrBlank()) append("\n⚠ ${item.caution}")
                append("\nNguồn: ${item.sourceTitle ?: "Dược liệu VN"}")
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
