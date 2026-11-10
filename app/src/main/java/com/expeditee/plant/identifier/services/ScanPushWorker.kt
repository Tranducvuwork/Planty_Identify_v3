package com.expeditee.plant.identifier.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ScanPushWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        // Notifications disabled
        return Result.success()
    }
}
