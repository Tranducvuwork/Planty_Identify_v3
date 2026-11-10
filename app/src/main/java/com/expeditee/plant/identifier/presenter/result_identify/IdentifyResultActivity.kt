package com.expeditee.plant.identifier.presenter.result_identify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityIdentifyResultBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.home.HomeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class IdentifyResultActivity :
    BaseActivity<ActivityIdentifyResultBinding, IdentifyResultViewModel, IdentifyResultState>() {

    companion object {
        private const val EXTRA_PLANT_NAME = "extra_plant_name"
        private const val EXTRA_CONFIDENCE = "extra_confidence"
        private const val EXTRA_SOURCE = "extra_source"

        fun start(context: Context, plantName: String, confidencePercent: Int, source: String?) {
            val intent = Intent(context, IdentifyResultActivity::class.java).apply {
                putExtra(EXTRA_PLANT_NAME, plantName)
                putExtra(EXTRA_CONFIDENCE, confidencePercent)
                putExtra(EXTRA_SOURCE, source)
            }
            context.startActivity(intent)
        }
    }

    override fun provideLayout() = R.layout.activity_identify_result
    override fun getLazyViewModel() = viewModel<IdentifyResultViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        val name = intent.getStringExtra(EXTRA_PLANT_NAME).orEmpty()
        val confidence = intent.getIntExtra(EXTRA_CONFIDENCE, 0)
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        viewModel.loadResult(name, confidence, source)
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.state) { state ->
            when (state) {
                IdentifyResultState.Loading -> Unit
                is IdentifyResultState.Content -> renderContent(state)
            }
        }
    }

    private fun renderContent(state: IdentifyResultState.Content) {
        viewBinding.tvPlantName.text = state.plantName.ifBlank { getString(R.string.unknown) }
        viewBinding.tvConfidence.text =
            getString(R.string.identify_confidence_format, state.confidencePercent)
        viewBinding.tvSource.text =
            getString(
                R.string.identify_source_format,
                state.source?.takeIf { it.isNotBlank() } ?: getString(R.string.identify_source_na),
            )
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.btnDone.onClickWithScale {
            startActivity(Intent(this, ScanCameraActivity::class.java))
            finish()
        }
    }
}