package com.expeditee.plant.identifier.presenter.error_process

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityIdentifyErrorBinding
import com.expeditee.plant.identifier.extension.showToast
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.processing.ScanProcessingState
import com.expeditee.plant.identifier.presenter.processing.ScanProcessingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ScanProcessingErrorActivity :
    BaseActivity<ActivityIdentifyErrorBinding, ScanProcessingViewModel, ScanProcessingState>() {

    companion object {
        private const val TAG = "ScanProcessingError"
        private const val EXTRA_MESSAGE_RES = "extra_message_res"
        private const val EXTRA_SUBTITLE_RES = "extra_subtitle_res"
        private const val EXTRA_SUBTITLE_TEXT = "extra_subtitle_text"
        private const val EXTRA_IS_DIAGNOSE = "extra_is_diagnose"
        private const val EXTRA_IS_HEALTHY = "extra_is_healthy"
        private const val EXTRA_LOCAL_IMAGE_PATH = "extra_local_image_path"

        fun start(
            context: Context,
            messageRes: Int,
            subtitleRes: Int = messageRes,
            subtitleText: String? = null,
            isDiagnose: Boolean = false,
            isHealthy: Boolean = false,
            localImagePath: String? = null,
        ) {
            val intent = Intent(context, ScanProcessingErrorActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE_RES, messageRes)
                putExtra(EXTRA_SUBTITLE_RES, subtitleRes)
                putExtra(EXTRA_SUBTITLE_TEXT, subtitleText)
                putExtra(EXTRA_IS_DIAGNOSE, isDiagnose)
                putExtra(EXTRA_IS_HEALTHY, isHealthy)
                putExtra(EXTRA_LOCAL_IMAGE_PATH, localImagePath)
                // Tránh SecurityException khi load content:// từ gallery picker.
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }

    private val isDiagnose: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_DIAGNOSE, false)
    }
    private val isHealthy: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_HEALTHY, false)
    }
    private val localImagePath: String by lazy {
        intent.getStringExtra(EXTRA_LOCAL_IMAGE_PATH).orEmpty()
    }

    override fun provideLayout() = R.layout.activity_identify_error
    override fun getLazyViewModel() = viewModel<ScanProcessingViewModel>()
    override fun isFitsSystemWindows() = false
    override fun isLightStatusBar() = true

    override fun initViews(savedInstanceState: Bundle?) {
        applyInset()
        bindPreviewImage()

        val defaultMessageRes = intent.getIntExtra(EXTRA_MESSAGE_RES, R.string.scan_processing_failed)
        val defaultSubtitleRes = intent.getIntExtra(EXTRA_SUBTITLE_RES, defaultMessageRes)
        val defaultSubtitleText = intent.getStringExtra(EXTRA_SUBTITLE_TEXT).orEmpty()
        val uiModel = when {
            isDiagnose && isHealthy -> ErrorUiModel(
                iconRes = R.drawable.img_well_done,
                titleRes = R.string.diagnose_healthy_title,
                subtitleRes = R.string.diagnose_healthy_message,
                actionRes = R.string.check_other_plant,
            )

            isDiagnose -> ErrorUiModel(
                iconRes = R.drawable.img_unidentifiable,
                titleRes = R.string.diagnose_not_detected_title,
                subtitleRes = R.string.diagnose_not_detected_message,
                actionRes = R.string.retake_a_photo,
            )

            else -> ErrorUiModel(
                iconRes = R.drawable.img_unidentifiable,
                titleRes = defaultMessageRes,
                subtitleRes = defaultSubtitleRes,
                subtitleText = defaultSubtitleText.takeIf { it.isNotBlank() },
                actionRes = R.string.try_again,
            )
        }

        viewBinding.ivSuccessIcon.setImageResource(uiModel.iconRes)
        viewBinding.tvSuccessTitle.setText(uiModel.titleRes)
        // Ưu tiên mô tả từ server (description đã dịch); fallback string resource.
        val subtitle = uiModel.subtitleText?.takeIf { it.isNotBlank() }
            ?: getString(uiModel.subtitleRes)
        viewBinding.tvSuccessSubtitle.text = subtitle
        Log.d(TAG, "show error titleRes=${uiModel.titleRes} subtitle=${subtitle.take(160)}")
        viewBinding.tvAction.setText(uiModel.actionRes)
    }

    private fun bindPreviewImage() {
        if (localImagePath.isBlank()) return
        val uri = localImagePath.toUri()
        if (isTransientPhotoPickerUri(uri)) {
            Log.w(TAG, "skip picker content uri preview path=$localImagePath")
            return
        }
        runCatching {
            viewBinding.ivPlantMain.setImageURI(uri)
        }.onFailure {
            Log.w(TAG, "bindPreviewImage failed path=$localImagePath error=${it.message}")
        }
    }

    private fun isTransientPhotoPickerUri(uri: android.net.Uri): Boolean {
        if (uri.scheme != "content") return false
        val authority = uri.authority.orEmpty()
        val path = uri.path.orEmpty()
        return authority.contains("photopicker", ignoreCase = true) ||
            path.contains("/picker/", ignoreCase = true)
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.btnAction.setOnClickListener {
            val retryMode = if (isDiagnose) CameraMode.DIAGNOSE else CameraMode.IDENTIFY
            startActivity(Intent(this, ScanCameraActivity::class.java).apply {
                putExtra(ScanCameraActivity.EXTRA_CAMERA_MODE, retryMode)
            })
            finish()
        }

        viewBinding.ivBackToolbar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        viewBinding.accurateAsk.yesArea.setOnClickListener {
            if (updateAccuracyState(isLiked = true)) {
                showToast(R.string.thanks_for_feedback)
            }
        }

        viewBinding.accurateAsk.noArea.setOnClickListener {
            if (updateAccuracyState(isLiked = false)) {
                showToast(R.string.thanks_for_feedback)
            }
        }
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.llToolbar.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    private var hasResponded = false

    private fun updateAccuracyState(isLiked: Boolean): Boolean {
        if (hasResponded) return false
        hasResponded = true

        viewBinding.accurateAsk.icLikeStatus.setImageResource(
            if (isLiked) R.drawable.ic_like_selected else R.drawable.ic_like
        )
        viewBinding.accurateAsk.icDislikeStatus.setImageResource(
            if (!isLiked) R.drawable.ic_dislike_selected else R.drawable.ic_dislike
        )

        viewBinding.accurateAsk.yesArea.isEnabled = false
        viewBinding.accurateAsk.noArea.isEnabled = false

        return true
    }

    private data class ErrorUiModel(
        val iconRes: Int,
        val titleRes: Int,
        val subtitleRes: Int,
        val subtitleText: String? = null,
        val actionRes: Int,
    )
}
