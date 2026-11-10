package com.expeditee.plant.identifier.helper.permission

import android.content.Context
import androidx.fragment.app.FragmentActivity
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest

class PermissionImpl private constructor(
    private val mActivityRequester: FragmentActivity,
) : IPermission {

    companion object {

        fun with(mActivityRequester: FragmentActivity): PermissionImpl {
            return PermissionImpl(mActivityRequester)
        }
    }

    private val permissionHelper = PermissionHelperUtils()

    private var mOnShowRationale: OnShowRationale? = null
    private var mOnPermissionGranted: OnPermissionGranted? = null
    private var mOnPermissionDenied: OnPermissionDenied? = null
    private var mOnNeverAskAgain: OnNeverAskAgain? = null

    private fun addPermissionListener(
        onShowRationale: OnShowRationale,
        onPermissionGranted: OnPermissionGranted,
        onPermissionDenied: OnPermissionDenied = {},
        onNeverAskAgain: OnNeverAskAgain = {},
    ) {
        this.mOnShowRationale = onShowRationale
        this.mOnPermissionGranted = onPermissionGranted
        this.mOnPermissionDenied = onPermissionDenied
        this.mOnNeverAskAgain = onNeverAskAgain
    }

    private fun onShowRationale(request: PermissionRequest) {
        this.mOnShowRationale?.invoke(request)
    }

    private fun onPermissionDenied() {
        this.mOnPermissionDenied?.invoke()
    }

    private fun onNeverAskAgain() {
        this.mOnNeverAskAgain?.invoke()
    }

    private fun onPermissionGranted() {
        this.mOnPermissionGranted?.invoke()
    }


    private inner class PermissionHelperUtils {

    }

    fun Context.hasSelfPermission(vararg permissions: String): Boolean {
        return PermissionUtils.hasSelfPermissions(this, *permissions)
    }

    private fun shouldShowRationale(permission: String): Boolean {
        return mActivityRequester.shouldShowRequestPermissionRationale(permission)
    }

    private fun createPermissionRequester(vararg permissions: String): PermissionsRequester {
        return mActivityRequester.constructPermissionsRequest(
            *permissions,
            onShowRationale = ::onShowRationale,
            onPermissionDenied = ::onPermissionDenied,
            onNeverAskAgain = ::onNeverAskAgain,
            requiresPermission = ::onPermissionGranted
        )
    }
}