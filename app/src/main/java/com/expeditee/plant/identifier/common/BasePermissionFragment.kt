package com.expeditee.plant.identifier.common

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.helper.permission.PermissionDialog

abstract class BasePermissionFragment<VB, VM, State>(
    inflate: Inflater<VB>
) : BaseFragment<VB, VM, State>(inflate), IPermissionController
        where VB : ViewDataBinding,
              VM : BaseViewModel<State>,
              State : BaseViewModel.State {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGranted(currentRequestPermission)
            } else {
                if (!shouldShowRequestPermissionRationale(currentRequestPermission)) {
                    showNeverAskAgainDialog(
                        title = R.string.lb_camera_permission,
                        message = R.string.please_enable_camera_permission_in_settings,
                        permission = currentRequestPermission
                    )
                } else {
                    onPermissionDenied(currentRequestPermission)
                }
            }
        }

    private var currentRequestPermission: String = ""

    protected open fun onPermissionGranted(permission: String) {}
    protected open fun onPermissionDenied(permission: String) {}

    override fun askPermission(
        permission: String,
        onAcceptedAction: () -> Unit,
        onDeniedAction: () -> Unit
    ) {
        currentRequestPermission = permission

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                onAcceptedAction.invoke()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                showRationaleDialog(
                    title = R.string.lb_camera_permission,
                    message = R.string.camera_permission_is_required,
                    onProceed = { requestPermissionLauncher.launch(permission) },
                    onCancel = { onDeniedAction.invoke() }
                )
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showRationaleDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        onProceed: () -> Unit,
        onCancel: () -> Unit
    ) {
        PermissionDialog(
            title = getString(title),
            subTitle = getString(message),
            positiveText = getString(R.string.allow),
            negativeText = getString(R.string.denied),
            onAllow = onProceed,
            onDeny = onCancel
        ).show(parentFragmentManager, "PermissionDialog")
    }

    private fun showNeverAskAgainDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        permission: String
    ) {
        PermissionDialog(
            title = getString(title),
            subTitle = getString(message),
            positiveText = getString(R.string.to_settings),
            negativeText = getString(R.string.cancel),
            onAllow = { openAppSettings() },
            onDeny = { onPermissionDenied(permission) }
        ).show(parentFragmentManager, "PermissionDialog")
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
            startActivity(this)
        }
    }
}
