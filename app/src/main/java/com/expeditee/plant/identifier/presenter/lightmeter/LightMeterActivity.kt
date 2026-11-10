package com.expeditee.plant.identifier.presenter.lightmeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityLightMeterBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.presenter.lightmeter.model.LightLevel
import com.expeditee.plant.identifier.widget.dialog.LightMeterTipsDialog
import com.otaliastudios.cameraview.CameraUtils
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LightMeterActivity :
    BaseActivity<ActivityLightMeterBinding, LightMeterViewModel, LightMeterState>() {

    override fun getLazyViewModel() = viewModel<LightMeterViewModel>()
    override fun provideLayout() = R.layout.activity_light_meter
    override fun isFitsSystemWindows(): Boolean = false

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.cameraView.post {
            viewBinding.cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        }
        setupToolbar()
        if (checkCameraPermission()) {
            setupCamera()
        } else {
            requestCameraPermission()
        }
    }


    override fun initListeners() {
        viewBinding.tutorial.setOnClickListener {
            val dialog = LightMeterTipsDialog()
            dialog.show(supportFragmentManager)
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            showInterAdBeforeDoAction(kotlinx.coroutines.Runnable { finish() })
        }

        viewBinding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }
    }

    override fun onHandleBackPressed(onBackPressed: com.expeditee.plant.identifier.common.OnPerformBackPressed?) =
        super.onHandleBackPressed {
            showInterAdBeforeDoAction(Runnable { finish() })
        }

    private fun setupToolbar() {
        applyInsetToView { _, insets ->
            viewBinding.toolbar.updatePadding(
                top = insets.statusBars().top
            )
        }
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.onLoadCollapseBanner(this@LightMeterActivity)
        }
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
        lifecycleScope.launch {
            if (hasPurchase) {
                viewBinding.frBanner.beGone()
            } else {
                viewBinding.frBanner.beVisible()
            }
        }
    }

    private fun setupCamera() {
        with(viewBinding) {
            runCatching {
                cameraView.setLifecycleOwner(this@LightMeterActivity)
                cameraView.facing = when {
                    CameraUtils.hasCameraFacing(this@LightMeterActivity, Facing.BACK) -> Facing.BACK
                    CameraUtils.hasCameraFacing(
                        this@LightMeterActivity,
                        Facing.FRONT
                    ) -> Facing.FRONT

                    else -> Facing.BACK
                }
                cameraView.flash = Flash.OFF
                if (!cameraView.isOpened) cameraView.open()
            }
        }
    }

    private fun switchCamera() {
        try {
            val cameraView = viewBinding.cameraView
            if (!cameraView.isOpened) {
                cameraView.open()
                return
            }

            val hasFrontCamera = CameraUtils.hasCameraFacing(this, Facing.FRONT)
            val hasBackCamera = CameraUtils.hasCameraFacing(this, Facing.BACK)

            if (!hasFrontCamera && !hasBackCamera) {
                Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show()
                return
            }
            if (cameraView.facing == Facing.FRONT && !hasBackCamera) {
                Toast.makeText(this, "Back camera not available", Toast.LENGTH_SHORT).show()
                return
            }
            if (cameraView.facing == Facing.BACK && !hasFrontCamera) {
                Toast.makeText(this, "Front camera not available", Toast.LENGTH_SHORT).show()
                return
            }

            viewBinding.btnSwitchCamera.isEnabled = false
            cameraView.close()
            cameraView.facing = if (cameraView.facing == Facing.FRONT) Facing.BACK else Facing.FRONT
            cameraView.open()

            viewBinding.root.postDelayed({
                viewBinding.btnSwitchCamera.isEnabled = true
            }, 700)
        } catch (_: Exception) {
            Toast.makeText(this, "Error switching camera. Please try again.", Toast.LENGTH_SHORT)
                .show()
            viewBinding.btnSwitchCamera.isEnabled = true
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun initObservers() {
        super.initObservers()
        lifecycleScope.launch {
            viewModel.lightState.collect { state ->
                when (state) {
                    is LightMeterState.Idle -> {
                        viewBinding.textLightMeterLux.text = getString(R.string.light_idle)
                    }

                    is LightMeterState.LuxChanged -> {
                        val level = LightLevel.fromLux(state.lux)
                        viewBinding.textLightMeterLux.text = getString(
                            R.string.light_value,
                            state.lux,
                        )
                        viewBinding.textLightMeterStatus.text = getString(level.messageRes)
                        viewBinding.tvValue.text =
                            getString(
                                R.string.light_value_range,
                                level.minLux,
                                level.maxLux
                            )

                        viewBinding.iconLightStatus.setImageResource(level.iconRes)
                        viewBinding.lightMeterBar.updateRatio(state.ratio)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_required_light_meter),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.registerLightSensor()
    }

    override fun onPause() {
        super.onPause()
        viewBinding.cameraView.close()
        viewModel.unregisterLightSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.cameraView.destroy()
    }


    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
    }
}
