package com.expeditee.plant.identifier.presenter.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BasePermissionActivity
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.databinding.ActivityScanCameraBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.showToast
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.helper.SiteManager
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.FlashState
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.presenter.home.HomeActivity
import com.expeditee.plant.identifier.presenter.processing.ScanProcessingActivity
import com.expeditee.plant.identifier.services.OfflineModeService
import com.expeditee.plant.identifier.services.ScanLimitService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.concurrent.TimeUnit

class ScanCameraActivity :
    BasePermissionActivity<ActivityScanCameraBinding, ScanCameraViewModel, ScanCameraState>() {

    companion object {
        private const val TAG = "ScanCameraActivity"
        const val EXTRA_CAMERA_MODE = "extra_camera_mode"
        const val EXTRA_GARDEN_ID = "extra_garden_id"

        fun start(context: Context, cameraMode: CameraMode? = null, gardenId: String? = null) {
            context.startActivity(
                Intent(context, ScanCameraActivity::class.java).apply {
                    putExtra(EXTRA_CAMERA_MODE, cameraMode ?: CameraMode.IDENTIFY)
                    putExtra(EXTRA_GARDEN_ID, gardenId.orEmpty())
                }
            )
        }
    }

    private enum class ImageSource(val raw: String) {
        CAMERA("camera"), GALLERY("gallery"),
    }

    private val scanLimitService: ScanLimitService by inject()
    private val offlineModeService: OfflineModeService by inject()

    private val initialMode: CameraMode by lazy {
        (intent.getSerializableExtra(EXTRA_CAMERA_MODE) as? CameraMode) ?: CameraMode.IDENTIFY
    }

    private val gardenId: String by lazy {
        intent.getStringExtra(EXTRA_GARDEN_ID).orEmpty().ifBlank {
            SiteManager.DEFAULT_SITE_ID
        }
    }

    private var imageCapture: ImageCapture? = null
    private var pendingCaptureUri: Uri? = null
    private var pendingCaptureMode: CameraMode? = null
    private var pendingCaptureSource: ImageSource? = null
    private var boundCamera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isBindingCamera = false
    private var isCapturing = false
    private var bypassScanLimit: Boolean = false


    private val pickGalleryImage = registerForActivityResult(PickVisualMedia()) { uri ->
        Log.i(TAG, "gallery picker result uri=$uri")
        if (uri != null) deliverScanIfPossible(viewModel.cameraMode.value, uri, ImageSource.GALLERY)
    }

    override fun isLightStatusBar() = false
    override fun isFitsSystemWindows() = false
    override fun getLazyViewModel() = viewModel<ScanCameraViewModel>()
    override fun provideLayout() = R.layout.activity_scan_camera

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.loadRewardAd(this@ScanCameraActivity)
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.lifecycleOwner = this
        padRootForStatusBarInsets()
        setupModeTabs()
        lifecycleScope.launch(Dispatchers.IO) { scanLimitService.refreshQuotaFromStorage() }
        viewModel.applyInitialScanMode(initialMode)
        viewBinding.btnTabIdentify.text = getString(R.string.identify).uppercase()
        viewBinding.btnTabDiagnose.text = getString(R.string.diagnose).uppercase()

        askPermission(
            permission = Manifest.permission.CAMERA,
            onAcceptedAction = {
                bindCameraUseCases()
            },
            onDeniedAction = {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showToast(R.string.please_enable_camera_permission_in_settings)
                    finish()
                }
            }
        )
    }

    override fun onPermissionGranted(permission: String) {
        bindCameraUseCases()
    }

    override fun onPermissionDenied(permission: String) {
        showToast(R.string.please_enable_camera_permission_in_settings)
        finish()
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.flashState) { state ->
            viewBinding.ivFlash.setImageResource(state.iconRes)
            applyFlashToBoundCamera(state)
        }
        collectLatestRepeatOnLifecycle(viewModel.cameraMode) { mode ->
            renderSelectedModeTab(mode)
            val shouldShow = mode.typeMode == CameraMode.TYPE_IDENTIFY
            viewBinding.ivSwitchMode.apply { if (shouldShow) beVisible() else beGone() }
            viewBinding.tvScanMode.apply { if (shouldShow) beVisible() else beGone() }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                scanLimitService.scanCountFlow.collect { count ->
                    val isPremium = true
                    viewBinding.tvScanCount.apply {
                        if (isPremium) {
                            beGone()
                        } else {
                            beVisible()
                            text = getString(
                                R.string.scans_remaining_format,
                                count.remaining,
                                count.limit
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                offlineModeService.scanModeFlow.collect { mode ->
                    renderScanMode(mode)
                }
            }
        }
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.ivClose.onClickWithScale {
            showInterAdBeforeDoAction(Runnable { navigateBackOrHome() })
        }
        viewBinding.btnTabIdentify.onClickWithScale {
            viewModel.onState(ScanCameraState.SetMode(CameraMode.IDENTIFY))
        }
        viewBinding.btnTabDiagnose.onClickWithScale {
            viewModel.onState(ScanCameraState.SetMode(CameraMode.DIAGNOSE))
        }
        viewBinding.ivCapture.onClickWithScale { takePhoto() }
        viewBinding.ivGallery.onClickWithScale {
            pickGalleryImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        viewBinding.ivFlash.onClickWithScale { viewModel.onState(ScanCameraState.ToggleFlash) }
        viewBinding.ivSwitchMode.onClickWithScale {
            val modelFile = File(filesDir, Constants.IDENTIFY_MODEL_SUB_PATH)
            val taxonomyFile = File(filesDir, Constants.TAXONOMY_SUB_PATH)
            val isModelReady =
                modelFile.exists() && modelFile.length() > 1024 && taxonomyFile.exists()

            if (!isModelReady) {
                showToast(R.string.model_downloading_message)
                return@onClickWithScale
            }

            val currentMode = offlineModeService.scanModeFlow.value
            val targetMode = if (currentMode == ModeScan.ONLINE) ModeScan.OFFLINE else ModeScan.ONLINE
            offlineModeService.setScanMode(targetMode)
        }
    }

    private fun navigateBackOrHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }

    private fun renderScanMode(mode: ModeScan) {
        viewBinding.ivSwitchMode.setImageResource(
            if (mode == ModeScan.OFFLINE) R.drawable.ic_legacy_iap_uncheck
            else R.drawable.ic_legacy_iap_check,
        )
        viewBinding.tvScanMode.text = if (mode == ModeScan.OFFLINE) {
            getString(R.string.mode_offline)
        } else {
            getString(R.string.mode_online)
        }
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?) =
        super.onHandleBackPressed {
            showInterAdBeforeDoAction { finish() }
        }

    private fun padRootForStatusBarInsets() {
        applyInsetToView(viewBinding.root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, top, 0, 0)
        }
    }

    private fun setupModeTabs() {
        renderSelectedModeTab(viewModel.cameraMode.value)
    }

    private fun renderSelectedModeTab(mode: CameraMode) {
        val identifySelected = mode.typeMode == CameraMode.TYPE_IDENTIFY
        renderModeTab(
            isSelected = identifySelected,
            tab = viewBinding.btnTabIdentify,
        )
        renderModeTab(
            isSelected = !identifySelected,
            tab = viewBinding.btnTabDiagnose,
        )
    }

    private fun renderModeTab(isSelected: Boolean, tab: TextView) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_camera_mode_tab_selected)
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_camera_mode_selected))
        } else {
            tab.background = null
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_camera_mode_muted))
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun bindCameraUseCases() {
        if (isBindingCamera || !canUseActivityUi()) return
        isBindingCamera = true
        lifecycleScope.launch {
            try {
                val provider = withContext(Dispatchers.IO) {
                    ProcessCameraProvider.getInstance(this@ScanCameraActivity).get()
                }
                if (!canUseActivityUi() || !isCameraPermissionGranted()) return@launch
                cameraProvider?.unbindAll()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = viewBinding.camPreviewView.surfaceProvider
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(95)
                    .setTargetRotation(previewRotation())
                    .setFlashMode(imageCaptureFlashMode(viewModel.flashState.value))
                    .build()

                boundCamera = provider.bindToLifecycle(
                    this@ScanCameraActivity,
                    selector,
                    preview,
                    imageCapture!!,
                )
                applyFlashToBoundCamera(viewModel.flashState.value)
                wireTapToFocus()
            } catch (_: Exception) {
                if (canUseActivityUi()) showToast(R.string.load_error)
            } finally {
                isBindingCamera = false
            }
        }
    }

    private fun imageCaptureFlashMode(state: FlashState): Int = when (state) {
        FlashState.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashState.ON -> ImageCapture.FLASH_MODE_ON
    }

    private fun applyFlashToBoundCamera(state: FlashState) {
        imageCapture?.flashMode = imageCaptureFlashMode(state)
        boundCamera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(false)
    }

    private fun previewRotation(): Int =
        viewBinding.root.display?.rotation ?: Surface.ROTATION_0

    private fun wireTapToFocus() {
        viewBinding.camPreviewView.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener true
            val factory = viewBinding.camPreviewView.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB,
            )
                .setAutoCancelDuration(5, TimeUnit.SECONDS)
                .build()
            viewBinding.focusView.focusCircle = RectF(
                event.x - 50,
                event.y - 50,
                event.x + 50,
                event.y + 50,
            )
            boundCamera?.cameraControl?.startFocusAndMetering(action)
            v.performClick()
            true
        }
    }

    private fun takePhoto() {
        if (isCapturing || imageCapture == null) return
        isCapturing = true
        viewBinding.ivCapture.isEnabled = false
        lifecycleScope.launch {
            try {
                lockFocusForCapture()
                val liveCapture = imageCapture
                if (!canUseActivityUi() || liveCapture == null) {
                    finishCaptureAttempt(success = false)
                    return@launch
                }
                saveStillImage(liveCapture)
            } catch (_: Exception) {
                finishCaptureAttempt(success = false)
            }
        }
    }

    private suspend fun lockFocusForCapture() {
        val camera = boundCamera ?: return
        val preview = viewBinding.camPreviewView
        if (preview.width <= 0 || preview.height <= 0) return
        val point = preview.meteringPointFactory.createPoint(
            preview.width / 2f,
            preview.height / 2f,
        )
        val upperPoint = preview.meteringPointFactory.createPoint(
            preview.width / 2f,
            preview.height * 0.38f,
        )
        val lowerPoint = preview.meteringPointFactory.createPoint(
            preview.width / 2f,
            preview.height * 0.62f,
        )
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB,
        )
            .addPoint(upperPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .addPoint(lowerPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .disableAutoCancel()
            .build()
        camera.cameraControl.startFocusAndMetering(action).awaitOrNull(2_500L)
    }

    private fun saveStillImage(capture: ImageCapture) {
        val capturedMode = viewModel.cameraMode.value
        val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}.jpeg")
        val uri = Uri.fromFile(file)
        pendingCaptureUri = uri
        pendingCaptureMode = capturedMode
        pendingCaptureSource = ImageSource.CAMERA
        capture.targetRotation = previewRotation()
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(
                        TAG,
                        "captured image path=${file.absolutePath} exists=${file.exists()} size=${file.length()}B uri=$uri",
                    )
                    finishCaptureAttempt(success = true)
                    deliverScanIfPossible(capturedMode, uri, ImageSource.CAMERA)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "capture failed: ${exception.message}", exception)
                    finishCaptureAttempt(success = false)
                    if (canUseActivityUi()) showToast(R.string.load_error)
                }
            },
        )
    }

    private fun finishCaptureAttempt(success: Boolean) {
        isCapturing = false
        if (!success && canUseActivityUi()) {
            viewBinding.ivCapture.isEnabled = true
        }
    }

    private suspend fun <T> ListenableFuture<T>.awaitOrNull(timeoutMs: Long): T? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                addListener(
                    {
                        if (!continuation.isActive) return@addListener
                        runCatching { get() }
                            .onSuccess { continuation.resume(it) }
                            .onFailure { continuation.resumeWithException(it) }
                    },
                    ContextCompat.getMainExecutor(this@ScanCameraActivity),
                )
            }
        }

    private fun deliverScanIfPossible(mode: CameraMode, uri: Uri, source: ImageSource) {
        pendingCaptureUri = uri
        pendingCaptureMode = mode
        pendingCaptureSource = source
        // Ads & premium removed — unlimited scans
        processPendingCapture()
    }

    private fun processPendingCapture() {
        val uri = pendingCaptureUri ?: return
        val mode = pendingCaptureMode ?: viewModel.cameraMode.value
        val source = pendingCaptureSource ?: ImageSource.CAMERA
        pendingCaptureUri = null
        pendingCaptureMode = null
        pendingCaptureSource = null
        lifecycleScope.launch {
            if (!canUseActivityUi()) return@launch
            if (source == ImageSource.GALLERY) {
                // Photo Picker URI mất quyền ngay sau khi rời picker → phải copy local trước.
                val localFile = persistGalleryPickerUri(uri)
                if (localFile == null) {
                    showToast(R.string.unable_to_load_image_from_gallery)
                    return@launch
                }
                openProcessingFile(localFile, mode, source.raw)
            } else {
                val path = uri.path ?: run {
                    showToast(R.string.unable_to_load_image_from_gallery)
                    return@launch
                }
                openProcessingFile(File(path), mode, source.raw)
            }
        }
    }

    /**
     * Copy ảnh từ Photo Picker (content://media/picker/...) vào filesDir/images
     * trong lúc vẫn còn temporary URI permission.
     */
    private suspend fun persistGalleryPickerUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(filesDir, "images").apply { mkdirs() }
            val out = File(dir, "gallery_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            out.takeIf { it.exists() && it.length() > 0L }.also {
                Log.i(
                    TAG,
                    "persistGalleryPickerUri ok uri=$uri path=${it?.absolutePath} size=${it?.length()}B",
                )
            }
        }.onFailure {
            Log.e(TAG, "persistGalleryPickerUri failed uri=$uri error=${it.message}", it)
        }.getOrNull()
    }

    private fun openProcessingFile(file: File, mode: CameraMode, sourceRaw: String) {
        Log.i(
            TAG,
            "open processing file=${file.absolutePath} exists=${file.exists()} size=${file.length()}B mode=${mode.typeMode} source=$sourceRaw",
        )
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        openProcessingUri(uri, mode, sourceRaw)
    }

    private fun openProcessingUri(uri: Uri, mode: CameraMode, sourceRaw: String) {
        Log.i(
            TAG,
            "open processing uri=$uri mode=${mode.typeMode} source=$sourceRaw",
        )
        ScanProcessingActivity.start(
            context = this,
            imageUri = uri.toString(),
            cameraMode = mode,
            imageSource = sourceRaw,
            gardenId = gardenId
        )
        finish()
    }



    override fun onPause() {
        if (!isCapturing) {
            releaseCamera()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) { scanLimitService.refreshQuotaFromStorage() }
        viewBinding.root.post {
            if (isCameraPermissionGranted() && canUseActivityUi()) bindCameraUseCases()
        }
    }

    override fun onDestroy() {
        releaseCamera()
        cameraProvider = null
        super.onDestroy()
    }

    private fun releaseCamera() {
        runCatching { cameraProvider?.unbindAll() }
        boundCamera = null
        imageCapture = null
    }

    private fun canUseActivityUi(): Boolean =
        !isFinishing && !isDestroyed && lifecycle.currentState != Lifecycle.State.DESTROYED
}
