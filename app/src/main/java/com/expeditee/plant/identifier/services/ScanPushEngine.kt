package com.expeditee.plant.identifier.services

import android.content.Context
import androidx.work.WorkManager

object ScanPushEngine {

    private const val TAG = "ScanPushEngine"

    /** Push notifications disabled — cancel any leftover work. */
    fun schedulePush(context: Context, delayHours: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}
