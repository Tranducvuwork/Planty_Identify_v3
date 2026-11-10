package com.expeditee.plant.identifier.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_REMINDER = "com.expeditee.plant.identifier.ACTION_REMINDER"
        private const val TAG = "ReminderScheduler"
    }

    fun schedule(reminder: ReminderScheduleModel) {
        val requestCode = if (reminder.id <= 0) reminder.hashCode() else reminder.id

        val intent = reminderBroadcastIntent(reminder)

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = (reminder.nextDate - (30_0000L)).coerceAtLeast(60_000L)
        val now = System.currentTimeMillis()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.i(TAG, "===> SCHEDULE ATTEMPT <===")
        Log.i(TAG, "Plant: ${reminder.plantName}")
        Log.i(TAG, "Trigger Time: ${sdf.format(Date(triggerAt))}")
        Log.i(TAG, "Current Time: ${sdf.format(Date(now))}")
        Log.i(TAG, "Diff: ${(triggerAt - now) / 1000} seconds")

        if (triggerAt <= now) {
            Log.e(TAG, "FAILED: triggerAt is in the past!")
            return
        }

        try {
            // Can exact alarms be scheduled? (Android 12+)
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAt, pendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                Log.i(TAG, "SUCCESS: Exact alarm set.")
            } else {
                Log.w(TAG, "WARNING: Cannot schedule exact alarms. Using inexact set().")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ERROR scheduling alarm", e)
        }
    }

    fun cancel(reminderId: Int) {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun reminderBroadcastIntent(reminder: ReminderScheduleModel) =
        Intent(appContext, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("plant_name", reminder.plantName)
            putExtra("remind_type", reminder.remindType)
            putExtra("reminder_id", reminder.id)
        }

    fun calculateNextDate(remindTime: String, repeatType: String): Long {
        val parts = remindTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = java.util.Calendar.getInstance()
        val now = java.util.Calendar.getInstance()

        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Nếu thời gian đã trôi qua TRONG HÔM NAY (trễ hơn 10 giây để tránh lag), cộng thêm chu kỳ lặp
        if (calendar.timeInMillis <= now.timeInMillis + 10000) {
            val repeatLower = repeatType.lowercase()
            val numeric =
                Regex("(\\d+)").find(repeatLower)?.groupValues?.getOrNull(1)?.toIntOrNull()

            when {
                repeatLower.contains("14") -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 14)
                repeatLower.contains("7") -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
                repeatLower.contains("month") -> calendar.add(
                    java.util.Calendar.MONTH,
                    numeric ?: 1
                )

                repeatLower.contains("week") -> calendar.add(
                    java.util.Calendar.WEEK_OF_YEAR,
                    numeric ?: 1
                )

                repeatLower.contains("day") -> calendar.add(
                    java.util.Calendar.DAY_OF_YEAR,
                    numeric ?: 1
                )

                else -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
