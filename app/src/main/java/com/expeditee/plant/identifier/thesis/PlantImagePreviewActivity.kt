package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityPlantImagePreviewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlantImagePreviewViewModel(
    application: android.app.Application,
) : BaseViewModel<PlantImagePreviewState>(application) {
    override fun onState(state: PlantImagePreviewState) = Unit
}

data class PlantImagePreviewState(val unused: Boolean = false) : BaseViewModel.State

class PlantImagePreviewActivity :
    BaseActivity<ActivityPlantImagePreviewBinding, PlantImagePreviewViewModel, PlantImagePreviewState>() {

    companion object {
        private const val EXTRA_IMAGE = "extra_image"

        fun start(context: Context, imagePath: String?) {
            if (imagePath.isNullOrBlank()) return
            context.startActivity(
                Intent(context, PlantImagePreviewActivity::class.java).putExtra(EXTRA_IMAGE, imagePath),
            )
        }
    }

    override fun provideLayout() = R.layout.activity_plant_image_preview
    override fun getLazyViewModel() = viewModel<PlantImagePreviewViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        PlantImages.load(viewBinding.ivPreview, intent.getStringExtra(EXTRA_IMAGE))
    }
}
