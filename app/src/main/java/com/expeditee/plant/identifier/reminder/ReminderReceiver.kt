package com.expeditee.plant.identifier.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        if (action == ReminderScheduler.ACTION_REMINDER) {
            val reminderId = intent.getIntExtra("reminder_id", 0)
            Log.i(TAG, "ACTION_REMINDER received reminderId=$reminderId — notifications disabled")
            // Notifications disabled
            return
        }
    }
}
