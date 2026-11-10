package com.expeditee.plant.identifier.thesis

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.core.view.WindowInsetsCompat
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.data.repository.PlantUserOverride
import com.expeditee.plant.identifier.databinding.ActivityThesisPlantEditBinding
import com.expeditee.plant.identifier.network.model.PlantScanResult
import org.koin.androidx.viewmodel.ext.android.viewModel

class ThesisPlantEditViewModel(
    application: android.app.Application,
    private val store: ThesisLocalStore,
) : BaseViewModel<ThesisPlantEditState>(application) {
    override fun onState(state: ThesisPlantEditState) = Unit

    fun save(
        scientificName: String,
        commonName: String,
        description: String,
        uses: List<String>,
        imagePath: String?,
    ) {
        val override = PlantUserOverride(
            scientificName = scientificName.trim(),
            commonNames = listOf(commonName.trim()).filter { it.isNotBlank() },
            description = description.trim().ifBlank { null },
            uses = uses,
            imagePath = imagePath?.takeIf { it.isNotBlank() },
        )
        store.saveOverride(override)
        store.addHistory(
            ScanHistoryItem(
                commonName = commonName.trim().ifBlank { scientificName.trim() },
                scientificName = scientificName.trim(),
                imagePath = imagePath.orEmpty(),
            ),
        )
        val current = PlantScanSession.lastResult
        val base = current ?: PlantScanResult(
            scientificName = scientificName.trim(),
            commonNames = listOf(commonName.trim()).filter { it.isNotBlank() },
            confidence = 100.0,
            description = description.trim().ifBlank { null },
            uses = uses,
            imageUrl = imagePath,
            detailAvailable = true,
        )
        PlantScanSession.store(
            override.applyTo(
                base.copy(
                    scientificName = scientificName.trim().ifBlank { base.scientificName },
                    commonNames = listOf(commonName.trim()).filter { it.isNotBlank() }
                        .ifEmpty { base.commonNames },
                ),
            ),
            imagePath ?: PlantScanSession.lastLocalImagePath,
        )
    }
}

data class ThesisPlantEditState(val unused: Boolean = false) : BaseViewModel.State

class ThesisPlantEditActivity :
    BaseActivity<ActivityThesisPlantEditBinding, ThesisPlantEditViewModel, ThesisPlantEditState>() {

    companion object {
        const val EXTRA_COMMON = "extra_common"
        const val EXTRA_SCIENTIFIC = "extra_scientific"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_USES = "extra_uses"
        const val EXTRA_IMAGE = "extra_image"
        const val EXTRA_CREATE = "extra_create"

        fun intent(
            context: Context,
            result: PlantScanResult? = null,
            imagePath: String? = null,
            create: Boolean = false,
        ): Intent {
            return Intent(context, ThesisPlantEditActivity::class.java).apply {
                putExtra(EXTRA_COMMON, result?.commonNames?.firstOrNull().orEmpty())
                putExtra(EXTRA_SCIENTIFIC, result?.scientificName.orEmpty())
                putExtra(EXTRA_DESCRIPTION, result?.description.orEmpty())
                putExtra(EXTRA_USES, result?.uses?.joinToString("\n").orEmpty())
                putExtra(EXTRA_IMAGE, imagePath ?: result?.imageUrl)
                putExtra(EXTRA_CREATE, create)
            }
        }

        fun start(
            context: Context,
            result: PlantScanResult? = null,
            imagePath: String? = null,
            create: Boolean = false,
        ) {
            context.startActivity(intent(context, result, imagePath, create))
        }
    }

    private var imagePath: String? = null

    private val pickPhoto = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        imagePath = HistoryImages.persist(this, uri.toString())
        PlantImages.load(viewBinding.ivPlant, imagePath)
    }

    override fun provideLayout() = R.layout.activity_thesis_plant_edit
    override fun getLazyViewModel() = viewModel<ThesisPlantEditViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        val create = intent.getBooleanExtra(EXTRA_CREATE, false)
        viewBinding.toolbar.title = if (create) "Thêm cây" else "Sửa thông tin cây"
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.etCommonName.setText(intent.getStringExtra(EXTRA_COMMON).orEmpty())
        viewBinding.etScientific.setText(intent.getStringExtra(EXTRA_SCIENTIFIC).orEmpty())
        viewBinding.etDescription.setText(intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty())
        viewBinding.etUses.setText(intent.getStringExtra(EXTRA_USES).orEmpty())
        imagePath = intent.getStringExtra(EXTRA_IMAGE)
        if (!imagePath.isNullOrBlank()) {
            PlantImages.load(viewBinding.ivPlant, imagePath)
        }
        viewBinding.btnPickImage.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        viewBinding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val common = viewBinding.etCommonName.text?.toString().orEmpty().trim()
        val scientific = viewBinding.etScientific.text?.toString().orEmpty().trim()
        if (common.isBlank() && scientific.isBlank()) {
            Toast.makeText(this, "Nhập tên cây", Toast.LENGTH_SHORT).show()
            return
        }
        val uses = viewBinding.etUses.text?.toString().orEmpty()
            .lines()
            .map { it.trim().removePrefix("•").trim() }
            .filter { it.isNotBlank() }
        viewModel.save(
            scientificName = scientific.ifBlank { common },
            commonName = common.ifBlank { scientific },
            description = viewBinding.etDescription.text?.toString().orEmpty(),
            uses = uses,
            imagePath = imagePath,
        )
        Toast.makeText(this, "Đã lưu thông tin cây", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }
}
