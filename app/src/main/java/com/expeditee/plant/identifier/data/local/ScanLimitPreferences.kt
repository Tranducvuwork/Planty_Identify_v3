package com.expeditee.plant.identifier.data.local

import android.content.Context
import android.content.SharedPreferences
import com.expeditee.plant.identifier.model.ScanCount

class ScanLimitPreferences(context: Context) {

    private companion object {
        const val PREF_NAME = "scan_limit_prefs"
        const val KEY_FREE_SCANS_USED = "free_scans_used"
        const val KEY_REWARD_SCANS_USED = "reward_scans_used"
        const val KEY_LAST_RESET_DATE = "last_reset_date"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getFreeScansUsed(): Int = prefs.getInt(KEY_FREE_SCANS_USED, 0)

    fun incrementFreeScansUsed() {
        prefs.edit().putInt(KEY_FREE_SCANS_USED, getFreeScansUsed() + 1).apply()
    }

    fun getRewardScansUsed(): Int = prefs.getInt(KEY_REWARD_SCANS_USED, 0)

    fun incrementRewardScansUsed() {
        prefs.edit().putInt(KEY_REWARD_SCANS_USED, getRewardScansUsed() + 1).apply()
    }

    fun checkAndResetDailyScans() {
        val lastReset = prefs.getLong(KEY_LAST_RESET_DATE, 0L)
        val now = System.currentTimeMillis()
        if (lastReset == 0L || !isSameDay(lastReset, now)) {
            prefs.edit()
                .putInt(KEY_FREE_SCANS_USED, 0)
                .putInt(KEY_REWARD_SCANS_USED, 0)
                .putLong(KEY_LAST_RESET_DATE, now)
                .apply()
        }
    }

    fun snapshot(): ScanCount = ScanCount(
        freeUsed = getFreeScansUsed(),
        rewardUsed = getRewardScansUsed(),
    )

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
