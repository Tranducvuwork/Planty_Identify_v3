package com.expeditee.plant.identifier.thesis

import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Context
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityThesisPlantDetailBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.collectRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.loadImageWithGlide
import com.expeditee.plant.identifier.network.model.CitationData
import com.expeditee.plant.identifier.network.model.CommonDiseaseData
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.network.model.PlantScanResult
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.chat.PlantAssistantChatActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThesisPlantDetailActivity :
    BaseActivity<ActivityThesisPlantDetailBinding, ThesisPlantDetailViewModel, ThesisPlantDetailState>() {

    companion object {
        private const val EXTRA_IMAGE = "extra_image"

        fun start(context: Context, localImagePath: String? = null) {
            context.startActivity(
                Intent(context, ThesisPlantDetailActivity::class.java).apply {
                    putExtra(EXTRA_IMAGE, localImagePath ?: PlantScanSession.lastLocalImagePath)
                },
            )
        }

        fun startWithResult(context: Context, result: PlantScanResult, localImagePath: String? = null) {
            PlantScanSession.store(result, localImagePath)
            start(context, localImagePath)
        }
    }

    private val diaryStore: ThesisLocalStore by inject()

    override fun provideLayout() = R.layout.activity_thesis_plant_detail
    override fun getLazyViewModel() = viewModel<ThesisPlantDetailViewModel>()
    override fun isFitsSystemWindows() = false

    private var alertPlayed = false
    private var contentAnimated = false
    private var historyRecorded = false
    private var currentName = ""
    private var currentScientificName = ""
    private var currentImageUrl = ""
    private var toxicVisible = false
    private var localImagePath: String? = null
    private var lastGroup: PlantGroup = PlantGroup.ORNAMENTAL
    private var morphologyPartsVisible = false

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.load(PlantScanSession.lastResult, PlantScanSession.lastLocalImagePath)
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.layoutDetailBody.isVisible = true
        viewBinding.ivAiMascot.loadImageWithGlide(R.drawable.img_ai_plant_mascot)
        viewBinding.tabInfo.setOnClickListener { selectTab("info", scrollTo = viewBinding.tvDescriptionTitle) }
        viewBinding.tabUses.setOnClickListener { selectTab("uses", scrollTo = viewBinding.tvUsesTitle) }
        viewBinding.tabCare.setOnClickListener { selectTab("care", scrollTo = viewBinding.tvCareTitle) }
        viewBinding.tabNotes.setOnClickListener { selectTab("notes", scrollTo = viewBinding.tvDiseasesTitle) }
        viewBinding.tabMap.setOnClickListener { openPlantMap() }
        viewBinding.btnSaveDiary.setOnClickListener { saveToDiary() }
        viewBinding.btnShare.setOnClickListener { shareResult() }
        viewBinding.btnMap.setOnClickListener { openPlantMap() }
        viewBinding.btnFindFertilizerStore.setOnClickListener { openFertilizerStoreMap() }
        viewBinding.btnReport.setOnClickListener {
            val current = PlantScanSession.lastResult ?: return@setOnClickListener
            editLauncher.launch(
                ThesisPlantEditActivity.intent(
                    this,
                    result = current,
                    imagePath = localImagePath ?: current.imageUrl,
                ),
            )
        }
        viewBinding.ivPlant.setOnClickListener {
            val path = localImagePath?.takeIf { it.isNotBlank() }
                ?: currentImageUrl.takeIf { it.isNotBlank() }
            PlantImagePreviewActivity.start(this, path)
        }
        val image = intent.getStringExtra(EXTRA_IMAGE)
        localImagePath = image ?: PlantScanSession.lastLocalImagePath
        viewModel.load(PlantScanSession.lastResult, localImagePath)
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.uiState) { state ->
            when (state) {
                ThesisPlantDetailState.Empty -> finish()
                is ThesisPlantDetailState.Content -> bindContent(state)
            }
        }
        collectRepeatOnLifecycle(viewModel.openLink) { link ->
            openCareLink(link)
        }
        collectLatestRepeatOnLifecycle(viewModel.morphologyImages) { images ->
            applyMorphologyImages(images)
        }
    }

    private fun bindContent(state: ThesisPlantDetailState.Content) {
        val r = state.result
        val name = r.commonNames.firstOrNull().orEmpty().ifBlank { r.scientificName }
        currentName = name
        currentScientificName = r.scientificName
        val group = PlantGroup.from(r)
        lastGroup = group
        viewBinding.tvCommonName.text = name
        viewBinding.tvScientific.text = r.scientificName
        viewBinding.tvFamily.text = r.family.orEmpty()
        viewBinding.tvPlantGroup.isVisible = false
        viewBinding.tvConfidence.text = getString(R.string.thesis_confidence_format, r.confidence.toInt())
        localImagePath = state.localImagePath
        bindTextSection(
            title = viewBinding.tvDescriptionTitle,
            body = viewBinding.tvDescription,
            value = r.description.orEmpty(),
        )
        bindTextSection(
            title = viewBinding.tvMorphologyTitle,
            body = viewBinding.tvMorphology,
            value = r.morphology.orEmpty(),
        )
        bindMorphologyParts(r.morphology.orEmpty().isNotBlank())
        applyMorphologyImages(viewModel.morphologyImages.value)
        bindTextSection(
            title = viewBinding.tvOriginTitle,
            body = viewBinding.tvOrigin,
            value = r.origin.orEmpty(),
        )
        bindTextSection(
            title = viewBinding.tvUsesTitle,
            body = viewBinding.tvUses,
            value = r.uses.filter { it.isNotBlank() }.joinToString("\n") { "• $it" },
        )
        bindRelatedVarieties(name, r.scientificName)

        val medicinal = (r.isMedicinal || group == PlantGroup.MEDICINE) &&
            group != PlantGroup.MUSHROOM &&
            r.toxicity?.isFungus != true
        val tox = r.toxicity
        val showToxic = ThesisPlantCatalog.shouldPlayToxicAlert(r) || tox?.isToxic == true
        val edibleMushroom = (group == PlantGroup.MUSHROOM || tox?.isFungus == true) && !showToxic
        viewBinding.tvMedicinalBadge.isVisible = (medicinal || edibleMushroom) && !showToxic
        if (edibleMushroom) {
            viewBinding.tvMedicinalBadge.text = getString(R.string.thesis_edible_mushroom)
        } else if (medicinal) {
            viewBinding.tvMedicinalBadge.text =
                r.medicinalLabel?.takeIf { it.isNotBlank() } ?: getString(R.string.thesis_medicinal_badge)
        }
        viewBinding.cardSummary.setCardBackgroundColor(
            when {
                showToxic -> 0xFFFFEBEE.toInt()
                edibleMushroom || medicinal -> 0xFFE8F5E9.toInt()
                else -> 0xFFFFFFFF.toInt()
            },
        )

        bindCare(r, group)
        bindTextOnly(viewBinding.tvDisclaimer, r.medicalDisclaimer.orEmpty())

        val safetyOn = appConfigSettings.childSafetyEnabled
        val useRed = !safetyOn || appConfigSettings.childSafetyRed
        toxicVisible = showToxic && useRed
        viewBinding.layoutToxic.isVisible = toxicVisible
        if (showToxic) {
            val title = when {
                tox?.isFungus == true -> getString(R.string.thesis_toxic_mushroom)
                tox?.toxicSap == true -> getString(R.string.thesis_toxic_sap)
                else -> getString(R.string.thesis_toxic_plant)
            }
            viewBinding.tvToxicTitle.text = title
            viewBinding.tvToxicMessage.text = tox?.warningMessage.orEmpty()
            val aid = tox?.firstAid
            viewBinding.tvFirstAid.isVisible = !aid.isNullOrBlank()
            viewBinding.tvFirstAid.text =
                if (aid.isNullOrBlank()) "" else getString(R.string.thesis_first_aid_format, aid)
        }

        viewBinding.tvAiPrompt.text = getString(R.string.thesis_ai_prompt_format, name)
        bindAiChips(group, name)

        bindCitations(r.care?.citations.orEmpty())
        bindDiseases(r.commonDiseases)
        bindFolk(r.folkRemedies)

        val localPath = state.localImagePath?.takeIf {
            it.isNotBlank() && !it.startsWith("http") && !it.startsWith("plant_images/")
        }
        val catalogImage = state.result.imageUrl?.takeIf { it.isNotBlank() }
        val image = localPath ?: catalogImage
        currentImageUrl = image.orEmpty()
        PlantImages.loadOptional(viewBinding.ivPlant, image)

        viewBinding.toolbar.title = getString(R.string.thesis_result_title)
        playIdentifySound(showToxic, medicinal && !edibleMushroom)
        recordHistory(r, state.localImagePath)
        animateContentOnce()
        selectTab("info")
    }

    private fun recordHistory(result: PlantScanResult, imagePath: String?) {
        if (historyRecorded) return
        historyRecorded = true
        val name = result.commonNames.firstOrNull().orEmpty().ifBlank { result.scientificName }
        val rawImage = imagePath.orEmpty().ifBlank { result.imageUrl.orEmpty() }
        val storedImage = if (rawImage.startsWith("http")) {
            rawImage
        } else {
            HistoryImages.persist(this, rawImage).ifBlank { result.imageUrl.orEmpty() }
        }
        val added = diaryStore.addHistory(
            ScanHistoryItem(
                commonName = name,
                scientificName = result.scientificName,
                imagePath = storedImage,
                plantGroup = PlantGroup.from(result).key,
            ),
        )
        if (!added) {
            Toast.makeText(this, R.string.thesis_history_exists, Toast.LENGTH_SHORT).show()
        }
    }

    private fun playIdentifySound(toxic: Boolean, medicinal: Boolean) {
        if (alertPlayed) return
        val safetyOn = appConfigSettings.childSafetyEnabled
        val soundOn = if (safetyOn) appConfigSettings.childSafetySound else true
        if (!soundOn) return
        alertPlayed = true
        ToxicAlertPlayer.playIdentifyCue(this, toxic = toxic, medicinal = medicinal)
    }

    private fun bindAiChips(group: PlantGroup, name: String) {
        val container = viewBinding.layoutAiChips
        container.removeAllViews()
        PlantGroup.chips(group, name).forEach { chip ->
            val tv = TextView(this).apply {
                text = chip.label
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@ThesisPlantDetailActivity, R.color.primary_green))
                setPadding(28, 16, 28, 16)
                background = ContextCompat.getDrawable(this@ThesisPlantDetailActivity, R.drawable.bg_chip_idle)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                lp.topMargin = (6 * resources.displayMetrics.density).toInt()
                layoutParams = lp
                setOnClickListener { onAiChip(chip) }
            }
            container.addView(tv)
        }
    }

    private fun onAiChip(chip: AiChip) {
        when (chip.id) {
            "diagnose" -> {
                startActivity(Intent(this, ScanCameraActivity::class.java).apply {
                    putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.DIAGNOSE)
                })
            }
            else -> {
                // Mở chat để AI trả lời đúng câu hỏi người dùng chọn
                PlantAssistantChatActivity.startWithQuestion(
                    this,
                    question = buildChatQuestion(chip),
                    plantName = currentName,
                )
                // Đồng thời hiện nội dung tương ứng trên màn hình
                when (chip.id) {
                    "distribution" -> selectTab("info", scrollTo = viewBinding.tvOriginTitle)
                    "structure", "learn" -> selectTab("info", scrollTo = viewBinding.tvMorphologyTitle)
                    "uses" -> selectTab("uses", scrollTo = viewBinding.tvUsesTitle)
                    "folk" -> selectTab("uses", scrollTo = viewBinding.tvFolkTitle)
                    "care" -> selectTab("care", scrollTo = viewBinding.tvCareTitle)
                    "fertilizer" -> selectTab("care", scrollTo = viewBinding.tvFertilizerStagesTitle)
                    "harvest" -> selectTab("care", scrollTo = viewBinding.tvHarvestTitle)
                    "pests" -> selectTab("notes", scrollTo = viewBinding.tvDiseasesTitle)
                    "warning", "notes" -> selectTab("notes", scrollTo = viewBinding.layoutToxic)
                }
            }
        }
    }

    private fun buildChatQuestion(chip: AiChip): String {
        val plant = currentName.ifBlank { "cây này" }
        return when (chip.id) {
            "learn", "structure" -> "Hãy giải thích đặc điểm và cấu tạo của $plant."
            "distribution" -> "Cho tôi biết vùng phân bố và môi trường sống của $plant."
            "uses" -> "Công dụng của $plant là gì? Phân biệt dân gian và tham khảo khoa học."
            "folk" -> "Giới thiệu bài thuốc dân gian từ $plant (chỉ tham khảo)."
            "care" -> "Hướng dẫn cách trồng và chăm sóc $plant: nước, phân bón, ánh sáng."
            "fertilizer" -> "Bón phân thế nào cho hợp lý với $plant? Gợi ý liều lượng và giai đoạn bón."
            "harvest" -> "Khi nào thu hoạch và bảo quản $plant?"
            "pests" -> "Sâu bệnh thường gặp trên $plant và cách phòng trừ tham khảo?"
            "warning", "notes" -> "Cảnh báo an toàn khi gặp $plant? Có độc không?"
            else -> chip.label
        }
    }

    private fun selectTab(tab: String, scrollTo: View? = null) {
        val tabs = listOf(
            viewBinding.tabInfo to "info",
            viewBinding.tabUses to "uses",
            viewBinding.tabCare to "care",
            viewBinding.tabNotes to "notes",
            viewBinding.tabMap to "map",
        )
        tabs.forEach { (tv, key) ->
            val selected = key == tab
            tv.setBackgroundResource(
                if (selected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected,
            )
            tv.setTextColor(
                if (selected) ContextCompat.getColor(this, android.R.color.white)
                else 0xFF666666.toInt(),
            )
        }
        val showInfo = tab == "info"
        val showUses = tab == "uses"
        val showCare = tab == "care"
        val showNotes = tab == "notes"
        fun show(v: View, visible: Boolean) {
            v.isVisible = visible
        }
        show(viewBinding.tvDescriptionTitle, showInfo && viewBinding.tvDescription.text.isNotBlank())
        show(viewBinding.tvDescription, showInfo && viewBinding.tvDescription.text.isNotBlank())
        show(viewBinding.tvMorphologyTitle, showInfo && viewBinding.tvMorphology.text.isNotBlank())
        show(viewBinding.tvMorphology, showInfo && viewBinding.tvMorphology.text.isNotBlank())
        show(
            viewBinding.layoutMorphologyParts,
            showInfo && morphologyPartsVisible,
        )
        show(viewBinding.tvOriginTitle, showInfo && viewBinding.tvOrigin.text.isNotBlank())
        show(viewBinding.tvOrigin, showInfo && viewBinding.tvOrigin.text.isNotBlank())
        show(viewBinding.tvUsesTitle, showUses && viewBinding.tvUses.text.isNotBlank())
        show(viewBinding.tvUses, showUses && viewBinding.tvUses.text.isNotBlank())
        show(viewBinding.tvRelatedVarietiesTitle, showUses && viewBinding.tvRelatedVarieties.text.isNotBlank())
        show(viewBinding.tvRelatedVarieties, showUses && viewBinding.tvRelatedVarieties.text.isNotBlank())
        show(viewBinding.tvFolkTitle, showUses && viewBinding.layoutFolk.childCount > 0)
        show(viewBinding.layoutFolk, showUses && viewBinding.layoutFolk.childCount > 0)
        show(viewBinding.tvCareTitle, showCare)
        show(viewBinding.tvPlanting, showCare && viewBinding.tvPlanting.text.isNotBlank())
        show(viewBinding.tvCare, showCare && viewBinding.tvCare.text.isNotBlank())
        show(viewBinding.tvWater, showCare && viewBinding.tvWater.text.isNotBlank())
        val showFertStages = showCare && (viewBinding.layoutFertilizerStages.tag as? Boolean) == true
        show(viewBinding.tvFertilizer, showCare && viewBinding.tvFertilizer.text.isNotBlank())
        show(viewBinding.btnFindFertilizerStore, showCare && viewBinding.tvFertilizer.text.isNotBlank())
        show(viewBinding.tvBuySeedLinksTitle, showCare && viewBinding.layoutBuySeedLinks.childCount > 0)
        show(viewBinding.layoutBuySeedLinks, showCare && viewBinding.layoutBuySeedLinks.childCount > 0)
        show(viewBinding.tvFertilizerStagesTitle, showFertStages)
        show(viewBinding.layoutFertilizerStages, showFertStages)
        show(viewBinding.tvLight, showCare && viewBinding.tvLight.text.isNotBlank())
        show(viewBinding.tvBestSeason, showCare && viewBinding.tvBestSeason.text.isNotBlank())
        show(viewBinding.tvHarvestTitle, showCare && viewBinding.tvHarvest.text.isNotBlank())
        show(viewBinding.tvHarvest, showCare && viewBinding.tvHarvest.text.isNotBlank())
        show(viewBinding.tvCitationsTitle, showCare && viewBinding.layoutCitations.childCount > 0)
        show(viewBinding.layoutCitations, showCare && viewBinding.layoutCitations.childCount > 0)
        show(viewBinding.tvCitationNote, showCare && viewBinding.tvCitationNote.text.isNotBlank())
        show(viewBinding.tvDiseasesTitle, showNotes && viewBinding.layoutDiseases.childCount > 0)
        show(viewBinding.layoutDiseases, showNotes && viewBinding.layoutDiseases.childCount > 0)
        show(viewBinding.tvDisclaimer, showNotes && viewBinding.tvDisclaimer.text.isNotBlank())
        viewBinding.layoutToxic.isVisible = (showNotes || showInfo) && toxicVisible
        val target = scrollTo?.takeIf { it.isVisible } ?: when (tab) {
            "uses" -> viewBinding.tvUsesTitle
            "care" -> viewBinding.tvCareTitle
            "notes" -> viewBinding.tvDiseasesTitle
            else -> viewBinding.tvDescriptionTitle
        }
        viewBinding.scrollContent.post {
            val y = target.top + viewBinding.layoutDetailBody.top
            viewBinding.scrollContent.smoothScrollTo(0, y.coerceAtLeast(0))
        }
    }

    private fun saveToDiary() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val photo = DiaryPhotos.persistPath(this, localImagePath)
        val entry = DiaryEntry(
            title = "Nhận diện $currentName",
            plantName = currentName.ifBlank { "Cây của tôi" },
            startDate = today,
            specialties = lastGroup.label,
            coverNote = careSuggestionText(currentName, lastGroup),
            timeline = listOf(
                DiaryTimelineEvent(
                    eventDate = today,
                    title = "Nhận diện AI",
                    content = buildString {
                        append("Đã nhận diện $currentName (${viewBinding.tvScientific.text})")
                        append("\n\n")
                        append(careSuggestionText(currentName, lastGroup))
                    },
                    photoUri = photo,
                ),
            ),
        )
        diaryStore.upsertDiary(entry)
        Toast.makeText(this, getString(R.string.thesis_diary_saved), Toast.LENGTH_SHORT).show()
        PlantDiaryDetailActivity.start(this, entry.id)
    }

    private fun openPlantMap() {
        val result = PlantScanSession.lastResult
        val plantName = currentName.ifBlank {
            result?.commonNames?.firstOrNull().orEmpty().ifBlank {
                result?.scientificName.orEmpty()
            }
        }
        val scientificName = currentScientificName.ifBlank {
            result?.scientificName.orEmpty()
        }
        val imageUrl = currentImageUrl.ifBlank {
            result?.imageUrl.orEmpty()
        }
        HerbalMapActivity.start(
            context = this,
            plantName = plantName,
            scientificName = scientificName,
            imageUrl = imageUrl,
        )
    }

    private fun careSuggestionText(name: String, group: PlantGroup): String {
        val plant = name.ifBlank { "cây" }
        return when (group) {
            PlantGroup.MEDICINE ->
                "Gợi ý chăm sóc: đặt $plant nơi có nắng nhẹ hoặc bán râm, giữ đất ẩm vừa, bón phân hữu cơ loãng mỗi 3-4 tuần. Khi có dấu hiệu bệnh, kiểm tra mặt dưới lá và hỏi mục chữa bệnh trước khi dùng thuốc."
            PlantGroup.FRUIT, PlantGroup.VEGETABLE, PlantGroup.AGRICULTURE ->
                "Gợi ý chăm sóc: $plant cần nắng tốt, tưới đều theo độ khô của đất, bón phân hữu cơ/vi sinh định kỳ. Theo dõi sâu ăn lá, rệp và nấm đốm lá để xử lý sớm."
            else ->
                "Gợi ý chăm sóc: tưới khi mặt đất se khô, đặt cây ở nơi sáng thoáng, tránh úng nước. Nếu cây vàng lá, đốm lá hoặc héo bất thường, dùng chức năng chẩn đoán bệnh để tìm cách chữa."
        }
    }

    private fun shareResult() {
        if (appConfigSettings.childSafetyEnabled && appConfigSettings.childSafetyLockShare) {
            Toast.makeText(this, R.string.thesis_share_locked, Toast.LENGTH_SHORT).show()
            return
        }
        val text = buildString {
            append(currentName)
            if (viewBinding.tvScientific.text.isNotBlank()) {
                append("\n").append(viewBinding.tvScientific.text)
            }
            append("\n").append(viewBinding.tvConfidence.text)
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                getString(R.string.share),
            ),
        )
    }

    private fun bindTextSection(title: View, body: TextView, value: String) {
        val show = value.isNotBlank()
        title.isVisible = show
        body.isVisible = show
        body.text = value
    }

    private fun bindMorphologyParts(show: Boolean) {
        morphologyPartsVisible = false
        viewBinding.layoutMorphologyParts.isVisible = false
        listOf(
            viewBinding.ivMorphRoot,
            viewBinding.ivMorphStem,
            viewBinding.ivMorphLeaf,
            viewBinding.ivMorphFlower,
            viewBinding.ivMorphFruit,
        ).forEach { hideMorphPart(it) }
    }

    private fun applyMorphologyImages(images: MorphologyPartImages) {
        morphologyPartsVisible = false
        viewBinding.layoutMorphologyParts.isVisible = false
        val hasAnyImage = listOf(images.root, images.stem, images.leaf, images.flower, images.fruit)
            .any { !it.isNullOrBlank() }
        if (!hasAnyImage) {
            listOf(
                viewBinding.ivMorphRoot,
                viewBinding.ivMorphStem,
                viewBinding.ivMorphLeaf,
                viewBinding.ivMorphFlower,
                viewBinding.ivMorphFruit,
            ).forEach { hideMorphPart(it) }
            return
        }
        loadMorphPart(viewBinding.ivMorphRoot, images.root)
        loadMorphPart(viewBinding.ivMorphStem, images.stem)
        loadMorphPart(viewBinding.ivMorphLeaf, images.leaf)
        loadMorphPart(viewBinding.ivMorphFlower, images.flower)
        loadMorphPart(viewBinding.ivMorphFruit, images.fruit)
    }

    private fun loadMorphPart(imageView: ImageView, remoteUrl: String?) {
        val container = imageView.parent as? View
        container?.isVisible = false
        imageView.setOnClickListener(null)
        PlantImages.loadOptional(
            imageView = imageView,
            url = remoteUrl,
            roundedCornerDp = 10,
            onReady = {
                morphologyPartsVisible = true
                if (viewBinding.tvMorphology.isVisible) {
                    viewBinding.layoutMorphologyParts.isVisible = true
                }
                container?.isVisible = true
                imageView.setOnClickListener {
                    PlantImagePreviewActivity.start(this, remoteUrl)
                }
            },
            onFailed = { hideMorphPart(imageView) },
        )
    }

    private fun hideMorphPart(imageView: ImageView) {
        imageView.setOnClickListener(null)
        (imageView.parent as? ViewGroup)?.isVisible = false
        PlantImages.clear(imageView)
    }

    private fun bindFertilizerStages(group: PlantGroup) {
        val show = group == PlantGroup.AGRICULTURE ||
            group == PlantGroup.VEGETABLE ||
            group == PlantGroup.FRUIT
        viewBinding.layoutFertilizerStages.tag = show
        viewBinding.tvFertilizerStagesTitle.isVisible = show
        viewBinding.layoutFertilizerStages.isVisible = show
        if (!show) return
        PlantImages.load(viewBinding.ivFertOrganic, FertilizerImages.ORGANIC, roundedCornerDp = 10)
        PlantImages.load(viewBinding.ivFertNpk, FertilizerImages.NPK, roundedCornerDp = 10)
        PlantImages.load(viewBinding.ivFertKali, FertilizerImages.KALI, roundedCornerDp = 10)
    }

    private fun bindTextOnly(body: TextView, value: String) {
        body.isVisible = value.isNotBlank()
        body.text = value
    }

    private fun bindCare(result: PlantScanResult, group: PlantGroup = PlantGroup.ORNAMENTAL) {
        val care = result.care
        val planting = care?.planting?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_planting_format, it) }
            .orEmpty()
        val careText = care?.care?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_care_format, it) }
            .orEmpty()
        val water = care?.water?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_water_format, it) }
            .orEmpty()
        val fertilizer = care?.fertilizer?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_fertilizer_format, it) }
            .orEmpty()
        val light = care?.light?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_light_format, it) }
            .orEmpty()
        val bestSeason = care?.bestSeason?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.thesis_season_format, it) }
            .orEmpty()
        val harvest = when (group) {
            PlantGroup.AGRICULTURE, PlantGroup.VEGETABLE, PlantGroup.FRUIT ->
                getString(R.string.thesis_harvest_default, currentName)
            else -> ""
        }
        val hasCare = planting.isNotBlank() || careText.isNotBlank() || bestSeason.isNotBlank() ||
            water.isNotBlank() || fertilizer.isNotBlank() || light.isNotBlank()
        viewBinding.tvCareTitle.isVisible = hasCare
        bindTextOnly(viewBinding.tvPlanting, planting)
        bindTextOnly(viewBinding.tvCare, careText)
        bindTextOnly(viewBinding.tvWater, water)
        bindTextOnly(viewBinding.tvFertilizer, fertilizer)
        bindFertilizerStages(group)
        bindTextOnly(viewBinding.tvLight, light)
        bindTextOnly(viewBinding.tvBestSeason, bestSeason)
        bindTextOnly(viewBinding.tvHarvest, harvest)
        viewBinding.tvHarvestTitle.isVisible = harvest.isNotBlank()
        bindTextOnly(viewBinding.tvCitationNote, care?.citationNote.orEmpty())
        bindSeedPurchaseLinks(currentName)
    }

    private fun animateContentOnce() {
        if (contentAnimated) return
        contentAnimated = true
        viewBinding.ivPlant.apply {
            alpha = 0f
            scaleX = 0.98f
            scaleY = 0.98f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240L)
                .start()
        }
        viewBinding.layoutDetailBody.apply {
            alpha = 0f
            translationY = 18f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(70L)
                .setDuration(260L)
                .start()
        }
    }

    private fun openCareLink(link: CareLink) {
        Log.i(CareVideoLookup.LOG_TAG, "Activity open $link")
        when (link) {
            is CareLink.Watch -> {
                val uri = Uri.parse(CareVideoLookup.watchUrl(link.videoId))
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            is CareLink.Search -> openYoutubeSearch(link.query)
            is CareLink.Web -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            }
        }
    }

    private fun openYoutubeSearch(query: String) {
        val youtubeSearch = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra(SearchManager.QUERY, query)
            putExtra("query", query)
        }
        try {
            startActivity(youtubeSearch)
            Log.i(CareVideoLookup.LOG_TAG, "opened YouTube SEARCH query='$query'")
        } catch (e: ActivityNotFoundException) {
            Log.w(CareVideoLookup.LOG_TAG, "YouTube SEARCH missing, open browser", e)
            val encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8.name())
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=$encoded"),
            )
            startActivity(Intent.createChooser(web, "YouTube"))
        }
    }

    private fun openFertilizerStoreMap() {
        val groupQuery = when (lastGroup) {
            PlantGroup.AGRICULTURE, PlantGroup.VEGETABLE, PlantGroup.FRUIT ->
                "cửa hàng vật tư nông nghiệp phân bón NPK hữu cơ gần đây"
            else -> "cửa hàng phân bón cây cảnh phân hữu cơ gần đây"
        }
        val encoded = java.net.URLEncoder.encode(groupQuery, java.nio.charset.StandardCharsets.UTF_8.name())
        val mapsUri = Uri.parse("geo:0,0?q=$encoded")
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"),
                ),
            )
        }
    }

    private fun bindRelatedVarieties(commonName: String, scientificName: String) {
        val family = relatedVarietiesFor(commonName, scientificName)
        val text = if (family.isEmpty()) {
            val name = commonName.ifBlank { scientificName }
            if (name.isBlank()) {
                ""
            } else {
                getString(R.string.thesis_related_varieties_generic, name, name)
            }
        } else {
            getString(
                R.string.thesis_related_varieties_format,
                family.label,
                family.items.joinToString(", "),
                family.label,
            )
        }
        bindTextOnly(viewBinding.tvRelatedVarieties, text)
        viewBinding.tvRelatedVarietiesTitle.isVisible = text.isNotBlank()
    }

    private fun relatedVarietiesFor(commonName: String, scientificName: String): PlantVarietyFamily {
        val key = foldPlantKeyword("$commonName $scientificName")
        return when {
            key.hasAny("cuc", "chrysanthemum") -> PlantVarietyFamily(
                "cúc",
                listOf("cúc đại đóa", "cúc pingpong", "cúc tây", "cúc mâm xôi", "cúc họa mi", "cúc tana", "cúc thạch thảo"),
            )
            key.hasAny("hoa hong", "hong trung quoc", "rosa") -> PlantVarietyFamily(
                "hoa hồng",
                listOf("hồng cổ Sapa", "hồng leo", "hồng tỉ muội", "hồng nhung", "hồng ngoại", "hồng bụi"),
            )
            key.hasAny("lan", "phalaenopsis", "orchid") -> PlantVarietyFamily(
                "lan",
                listOf("lan hồ điệp", "lan dendro", "lan mokara", "lan vũ nữ", "lan cattleya", "lan kiếm"),
            )
            key.hasAny("mai", "ochna") -> PlantVarietyFamily(
                "mai",
                listOf("mai vàng", "mai tứ quý", "mai chiếu thủy", "mai giảo", "mai bonsai"),
            )
            key.hasAny("dao", "prunus persica") -> PlantVarietyFamily(
                "đào",
                listOf("đào bích", "đào phai", "đào thất thốn", "đào Nhật Tân", "đào chuông"),
            )
            key.hasAny("ot", "capsicum") -> PlantVarietyFamily(
                "ớt",
                listOf("ớt chỉ thiên", "ớt sừng", "ớt chuông", "ớt hiểm", "ớt xiêm xanh"),
            )
            key.hasAny("ca chua", "solanum lycopersicum") -> PlantVarietyFamily(
                "cà chua",
                listOf("cà chua bi", "cà chua beef", "cà chua socola", "cà chua cherry vàng", "cà chua Roma"),
            )
            key.hasAny("dua hau", "citrullus") -> PlantVarietyFamily(
                "dưa hấu",
                listOf("dưa hấu không hạt", "dưa hấu ruột vàng", "dưa hấu baby", "dưa hấu Hắc Mỹ Nhân"),
            )
            else -> PlantVarietyFamily()
        }
    }

    private fun bindSeedPurchaseLinks(plantName: String) {
        val name = plantName.ifBlank { currentScientificName }
        val container = viewBinding.layoutBuySeedLinks
        container.removeAllViews()
        if (name.isBlank()) {
            viewBinding.tvBuySeedLinksTitle.isVisible = false
            container.isVisible = false
            return
        }
        listOf(
            getString(R.string.thesis_buy_seed_shopee) to
                "https://shopee.vn/search?keyword=${urlEncode("hạt giống cây giống $name")}",
            getString(R.string.thesis_buy_seed_facebook) to
                "https://www.facebook.com/search/top?q=${urlEncode("cây giống hạt giống $name")}",
            getString(R.string.thesis_buy_seed_company) to
                "https://www.google.com/search?q=${urlEncode("công ty cung cấp giống cây $name")}",
            getString(R.string.thesis_buy_seed_nearby) to
                "https://www.google.com/maps/search/?api=1&query=${urlEncode("vườn ươm cây giống $name gần đây")}",
        ).forEach { (label, url) ->
            container.addView(linkText(label) { openWeb(url) })
        }
        viewBinding.tvBuySeedLinksTitle.isVisible = true
        container.isVisible = true
    }

    private fun openWeb(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun bindCitations(items: List<CitationData>) {
        val container = viewBinding.layoutCitations
        container.removeAllViews()
        if (items.isEmpty()) {
            viewBinding.tvCitationsTitle.isVisible = false
            container.isVisible = false
            viewBinding.tvCitationNote.isVisible = false
            return
        }
        viewBinding.tvCitationsTitle.isVisible = true
        container.isVisible = true
        items.forEach { cite ->
            val tv = simpleText(ThesisPlantCatalog.citationLabel(cite))
            tv.setTextColor(0xFF1565C0.toInt())
            tv.setOnClickListener {
                viewModel.openCitation(cite, currentName, currentScientificName)
            }
            container.addView(tv)
        }
    }

    private fun bindDiseases(items: List<CommonDiseaseData>) {
        val container = viewBinding.layoutDiseases
        container.removeAllViews()
        val cleanItems = items.filter {
            !it.name.isNullOrBlank() ||
                !it.description.isNullOrBlank() ||
                !it.ecoTreatment.isNullOrBlank()
        }
        if (cleanItems.isEmpty()) {
            viewBinding.tvDiseasesTitle.isVisible = false
            container.isVisible = false
            return
        }
        viewBinding.tvDiseasesTitle.isVisible = true
        container.isVisible = true
        cleanItems.forEach { d ->
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 12)
            }
            val pestImage = DiseaseImages.urlFor(d.name)
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (168 * resources.displayMetrics.density).toInt(),
                ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = ContextCompat.getDrawable(
                    this@ThesisPlantDetailActivity,
                    R.drawable.bg_confidence_pill,
                )
                contentDescription = d.name
            }
            block.addView(iv)
            PlantImages.load(iv, pestImage, roundedCornerDp = 12)
            d.name?.takeIf { it.isNotBlank() }?.let { block.addView(simpleText(it, bold = true)) }
            d.description?.takeIf { it.isNotBlank() }?.let { block.addView(simpleText(it)) }
            d.ecoTreatment?.takeIf { it.isNotBlank() }?.let {
                block.addView(simpleText(getString(R.string.thesis_eco_treatment_format, it)))
            }
            val cure = TextView(this).apply {
                text = getString(R.string.thesis_suggest_cure)
                setTextColor(ContextCompat.getColor(this@ThesisPlantDetailActivity, R.color.primary_green))
                textSize = 13f
                setPadding(0, 8, 0, 0)
                setOnClickListener {
                    startActivity(Intent(this@ThesisPlantDetailActivity, ScanCameraActivity::class.java).apply {
                        putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, CameraMode.DIAGNOSE)
                    })
                }
            }
            block.addView(cure)
            container.addView(block)
        }
    }

    private fun bindFolk(items: List<com.expeditee.plant.identifier.network.model.FolkRemedyData>) {
        val container = viewBinding.layoutFolk
        container.removeAllViews()
        if (items.isEmpty()) {
            viewBinding.tvFolkTitle.isVisible = false
            container.isVisible = false
            return
        }
        viewBinding.tvFolkTitle.isVisible = true
        container.isVisible = true
        items.forEach { f ->
            val tv = simpleText("• ${f.title}\n  ${f.ailment.orEmpty()}\n  ${f.method.orEmpty()}")
            container.addView(tv)
        }
    }

    private fun simpleText(text: String, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xFF333333.toInt())
            textSize = 14f
            typeface = resources.getFont(if (bold) R.font.poppins_semibold else R.font.poppins)
            includeFontPadding = false
            setLineSpacing(3f, 1f)
            setPadding(0, 3, 0, 3)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun linkText(text: String, onClick: () -> Unit): TextView {
        return simpleText(text, bold = true).apply {
            setTextColor(0xFF1565C0.toInt())
            minHeight = (44 * resources.displayMetrics.density).toInt()
            gravity = android.view.Gravity.CENTER_VERTICAL
            setOnClickListener { onClick() }
        }
    }

    private fun foldPlantKeyword(value: String): String {
        val normalized = java.text.Normalizer.normalize(value.lowercase(Locale.getDefault()), java.text.Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.hasAny(vararg needles: String): Boolean = needles.any { contains(it) }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.name())

    override fun onDestroy() {
        ToxicAlertPlayer.stop()
        super.onDestroy()
    }
}

private data class PlantVarietyFamily(
    val label: String = "",
    val items: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean = label.isBlank() || items.isEmpty()
}
