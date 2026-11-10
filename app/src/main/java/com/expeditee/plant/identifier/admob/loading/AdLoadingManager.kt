package com.expeditee.plant.identifier.admob.loading

import android.app.Activity
import android.content.Intent

object AdLoadingManager {

    private var isShowing: Boolean = false

    private const val REQUEST_CODE_LOADING = 9999

    fun show(activity: Activity) {
        if (isShowing) return
        isShowing = true
        val loadingIntent = Intent(activity, AdLoadingActivity::class.java)
        activity.startActivityForResult(loadingIntent, REQUEST_CODE_LOADING)
    }

    fun dismiss(activity: Activity) {
        if (!isShowing) return
        isShowing = false
        activity.finishActivity(REQUEST_CODE_LOADING)
    }
}
