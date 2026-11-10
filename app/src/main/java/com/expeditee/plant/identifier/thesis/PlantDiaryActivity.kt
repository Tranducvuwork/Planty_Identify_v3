package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityPlantDiaryDetailBinding
import com.expeditee.plant.identifier.databinding.ActivityPlantDiaryListBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.presenter.chat.PlantAssistantChatActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDiaryListViewModel(
    application: android.app.Application,
    private val store: ThesisLocalStore,
) : BaseViewModel<PlantDiaryListState>(application) {
    private val _state = MutableStateFlow(PlantDiaryListState())
    val uiState = _state.asStateFlow()

    override fun onState(state: PlantDiaryListState) {
        _state.value = state
    }

    fun refresh() {
        onState(PlantDiaryListState(store.listDiaries()))
    }

    fun create(title: String, plantName: String, specialties: String, note: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entry = DiaryEntry(
            title = title,
            plantName = plantName,
            startDate = today,
            specialties = specialties,
            coverNote = note,
            timeline = listOf(
                DiaryTimelineEvent(
                    eventDate = today,
                    title = "Bắt đầu trồng",
                    content = note.ifBlank { "Gieo / trồng ngày $today" },
                ),
            ),
        )
        store.upsertDiary(entry)
        refresh()
    }
}

data class PlantDiaryListState(
    val items: List<DiaryEntry> = emptyList(),
) : BaseViewModel.State

class PlantDiaryListActivity :
    BaseActivity<ActivityPlantDiaryListBinding, PlantDiaryListViewModel, PlantDiaryListState>() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PlantDiaryListActivity::class.java))
        }
    }

    private val adapter = DiaryListAdapter { diary ->
        PlantDiaryDetailActivity.start(this, diary.id)
    }

    override fun provideLayout() = R.layout.activity_plant_diary_list
    override fun getLazyViewModel() = viewModel<PlantDiaryListViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.rvDiaries.layoutManager = LinearLayoutManager(this)
        viewBinding.rvDiaries.adapter = adapter
        viewBinding.emptyState.tvEmptyTitle.setText(R.string.thesis_diary_empty_title)
        viewBinding.emptyState.tvEmptyMessage.setText(R.string.thesis_diary_empty)
        viewBinding.emptyState.ivEmpty.setImageResource(R.drawable.ic_camera_home)
        viewBinding.btnAddDiary.setOnClickListener {
            CreateDiaryDialog(this) { title, plant, spec, note ->
                viewModel.create(title, plant, spec, note)
            }.show()
        }
        viewModel.refresh()
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) {
            adapter.submit(it.items)
            val empty = it.items.isEmpty()
            viewBinding.emptyState.root.isVisible = empty
            viewBinding.rvDiaries.isVisible = !empty
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

private class DiaryListAdapter(
    private val onClick: (DiaryEntry) -> Unit,
) : RecyclerView.Adapter<DiaryListAdapter.VH>() {
    private val items = mutableListOf<DiaryEntry>()
    fun submit(list: List<DiaryEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_list, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], onClick)

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        fun bind(item: DiaryEntry, onClick: (DiaryEntry) -> Unit) {
            val initial = item.plantName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
            val avatar = itemView.findViewById<TextView>(R.id.tvAvatar)
            val cover = itemView.findViewById<ImageView>(R.id.ivCover)
            val coverPath = item.timeline.firstOrNull { it.photoUri.isNotBlank() }?.photoUri
            if (coverPath.isNullOrBlank()) {
                cover.isVisible = false
                avatar.isVisible = true
                avatar.text = initial
            } else {
                avatar.isVisible = false
                DiaryPhotos.load(cover, coverPath)
            }
            itemView.findViewById<TextView>(R.id.tvTitle).text =
                "${item.plantName} · ${item.title}"
            itemView.findViewById<TextView>(R.id.tvMeta).text =
                "Bắt đầu ${item.startDate} · ${item.timeline.size} mốc"
            itemView.findViewById<TextView>(R.id.tvSpecialties).text =
                item.specialties.ifBlank { item.coverNote.ifBlank { "Chưa ghi điểm đặc biệt" } }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}

class PlantDiaryDetailViewModel(
    application: android.app.Application,
    private val store: ThesisLocalStore,
) : BaseViewModel<PlantDiaryDetailState>(application) {
    private val _state = MutableStateFlow(PlantDiaryDetailState())
    val uiState = _state.asStateFlow()
    private var diaryId: String = ""

    override fun onState(state: PlantDiaryDetailState) {
        _state.value = state
    }

    fun load(id: String) {
        diaryId = id
        onState(PlantDiaryDetailState(store.getDiary(id)))
    }

    fun addEvent(title: String, content: String, photoUri: String = "") {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val updated = store.addEvent(
            diaryId,
            DiaryTimelineEvent(
                eventDate = today,
                title = title,
                content = content,
                photoUri = photoUri,
            ),
        )
        onState(PlantDiaryDetailState(updated))
    }
}

data class PlantDiaryDetailState(
    val diary: DiaryEntry? = null,
) : BaseViewModel.State

class PlantDiaryDetailActivity :
    BaseActivity<ActivityPlantDiaryDetailBinding, PlantDiaryDetailViewModel, PlantDiaryDetailState>() {

    companion object {
        private const val EXTRA_ID = "id"
        fun start(context: Context, diaryId: String) {
            context.startActivity(
                Intent(context, PlantDiaryDetailActivity::class.java).putExtra(EXTRA_ID, diaryId),
            )
        }
    }

    private var pendingPhotoPath: String = ""
    private var eventDialog: AddDiaryEventDialog? = null
    private var currentDiary: DiaryEntry? = null
    private val pickPhoto = registerForActivityResult(PickVisualMedia()) { uri ->
        pendingPhotoPath = uri?.let { DiaryPhotos.persist(this, it) }.orEmpty()
        if (pendingPhotoPath.isNotBlank()) {
            eventDialog?.showPhotoPreview(pendingPhotoPath)
            Toast.makeText(this, "Đã gắn ảnh vào mốc", Toast.LENGTH_SHORT).show()
        }
    }
    private val adapter = TimelineAdapter()

    override fun provideLayout() = R.layout.activity_plant_diary_detail
    override fun getLazyViewModel() = viewModel<PlantDiaryDetailViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.rvTimeline.layoutManager = LinearLayoutManager(this)
        viewBinding.rvTimeline.adapter = adapter
        viewBinding.btnAddEvent.setOnClickListener {
            pendingPhotoPath = ""
            eventDialog = AddDiaryEventDialog(
                this,
                onPickPhoto = {
                    pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
            ) { title, content ->
                viewModel.addEvent(title, content, pendingPhotoPath)
                pendingPhotoPath = ""
                eventDialog = null
            }.also { it.show() }
        }
        viewBinding.btnFindCure.setOnClickListener {
            val diary = currentDiary ?: return@setOnClickListener
            PlantAssistantChatActivity.startWithQuestion(
                this,
                question = "Hướng dẫn chăm sóc và tìm cách chữa bệnh thường gặp cho ${diary.plantName}: nước, phân bón, ánh sáng, sâu bệnh và cách xử lý an toàn.",
                plantName = diary.plantName,
            )
        }
        viewModel.load(intent.getStringExtra(EXTRA_ID).orEmpty())
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            val d = state.diary ?: return@collectLatestRepeatOnLifecycle
            currentDiary = d
            viewBinding.toolbar.title = d.plantName
            viewBinding.tvHeader.text = buildString {
                append(d.title)
                append("\nCây: ${d.plantName}")
                append("\nBắt đầu: ${d.startDate}")
                if (d.specialties.isNotBlank()) append("\nĐặc điểm: ${d.specialties}")
                if (d.coverNote.isNotBlank()) append("\n${d.coverNote}")
            }
            viewBinding.tvCareSuggestion.text = diaryCareSuggestion(d)
            adapter.submit(d.timeline)
        }
    }

}

private fun diaryCareSuggestion(diary: DiaryEntry): String {
    val plant = diary.plantName.ifBlank { "cây" }
    val group = PlantGroup.fromKey(diary.specialties) ?: PlantGroup.entries.firstOrNull {
        it.label.equals(diary.specialties, ignoreCase = true)
    } ?: PlantGroup.ORNAMENTAL
    return when (group) {
        PlantGroup.MEDICINE ->
            "Gợi ý chăm sóc $plant: giữ đất ẩm vừa, ánh sáng tán xạ hoặc nắng nhẹ, bón phân hữu cơ loãng mỗi 3-4 tuần. Theo dõi rệp, nấm đốm lá và úng rễ."
        PlantGroup.AGRICULTURE, PlantGroup.VEGETABLE, PlantGroup.FRUIT ->
            "Gợi ý chăm sóc $plant: cần nắng tốt, tưới đều theo độ khô đất, bổ sung phân hữu cơ/vi sinh định kỳ. Kiểm tra sâu ăn lá, rệp và bệnh nấm sau mưa."
        else ->
            "Gợi ý chăm sóc $plant: đặt nơi sáng thoáng, tưới khi mặt đất se khô, tránh úng nước. Nếu lá vàng, đốm hoặc héo, bấm nút tìm cách chữa bệnh để được hướng dẫn."
    }
}

private class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.VH>() {
    private val items = mutableListOf<DiaryTimelineEvent>()
    fun submit(list: List<DiaryTimelineEvent>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_timeline, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        fun bind(item: DiaryTimelineEvent) {
            itemView.findViewById<TextView>(R.id.tvEventTitle).text =
                "● ${item.eventDate} — ${item.title}"
            itemView.findViewById<TextView>(R.id.tvEventContent).text = item.content
            val photo = itemView.findViewById<ImageView>(R.id.ivEventPhoto)
            DiaryPhotos.load(photo, item.photoUri)
        }
    }
}
