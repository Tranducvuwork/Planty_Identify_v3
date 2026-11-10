package com.expeditee.plant.identifier.services.notification

import android.app.NotificationManager
import android.content.Context

object HomeNotificationManager {

    private const val NOTIFICATION_ID = 1001

    fun showNotification(context: Context) {
        // Notifications disabled
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
