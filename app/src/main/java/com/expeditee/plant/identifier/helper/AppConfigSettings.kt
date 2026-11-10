package com.expeditee.plant.identifier.helper

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.core.content.edit
import com.expeditee.plant.identifier.model.ModeScan

class AppConfigSettings(private val context: Context) {

    companion object {
        private const val KEY_INTRO_COMPLETE = "intro_complete"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_CONFIRM_CONSENT = ".KEY_CONFIRM_CONSENT"
        private const val KEY_USER_GLOBAL = ".KEY_USER_GLOBAL"
        private const val KEY_HAS_RATED_OR_FEEDBACK = "key_has_rated_or_feedback"
        private const val KEY_SCAN_MODE = "key_scan_mode"
        private const val KEY_PREMIUM_OFFLINE_MODE = "key_premium_offline_mode"
        private const val KEY_TEMPERATURE_UNIT = "key_temperature_unit"
        private const val KEY_CHILD_SAFETY = "key_child_safety"
        private const val KEY_CHILD_SAFETY_SOUND = "key_child_safety_sound"
        private const val KEY_CHILD_SAFETY_RED = "key_child_safety_red"
        private const val KEY_CHILD_SAFETY_GUIDE = "key_child_safety_guide"
        private const val KEY_CHILD_SAFETY_LOCK_SHARE = "key_child_safety_lock_share"
        private const val PREFS_KEY = "plant_app_base"

        private fun Context.sharedPreferences(mode: Int = Context.MODE_PRIVATE): SharedPreferences {
            return getSharedPreferences(PREFS_KEY, mode)
        }
    }

    private val preferences: SharedPreferences = context.sharedPreferences()

    var introCompleted: Boolean
        get() = preferences.getBoolean(KEY_INTRO_COMPLETE, false)
        set(show) = preferences.edit { putBoolean(KEY_INTRO_COMPLETE, show) }

    var currentLanguage: String?
        get() = preferences.getString(
            KEY_CURRENT_LANGUAGE,
            Resources.getSystem().configuration.locales[0].language,
        )
        set(language) = preferences.edit { putString(KEY_CURRENT_LANGUAGE, language) }

    var isConfirmConsent: Boolean
        get() = preferences.getBoolean(KEY_CONFIRM_CONSENT, false)
        set(show) = preferences.edit { putBoolean(KEY_CONFIRM_CONSENT, show) }


    var userScan: Long
        get() = preferences.getLong(KEY_CONFIRM_CONSENT, 0)
        set(show) = preferences.edit { putLong(KEY_CONFIRM_CONSENT, show) }

    var isUserGlobal: Boolean
        get() = preferences.getBoolean(KEY_USER_GLOBAL, false)
        set(show) = preferences.edit { putBoolean(KEY_USER_GLOBAL, show) }

    var hasRatedOrFeedback: Boolean
        get() = preferences.getBoolean(KEY_HAS_RATED_OR_FEEDBACK, false)
        set(value) = preferences.edit { putBoolean(KEY_HAS_RATED_OR_FEEDBACK, value) }

    var scanMode: ModeScan
        get() = runCatching {
            ModeScan.valueOf(preferences.getString(KEY_SCAN_MODE, ModeScan.ONLINE.name).orEmpty())
        }.getOrDefault(ModeScan.ONLINE)
        set(value) = preferences.edit { putString(KEY_SCAN_MODE, value.name) }

    var isPremiumUseOfflineMode: Boolean
        get() = preferences.getBoolean(KEY_PREMIUM_OFFLINE_MODE, false)
        set(value) = preferences.edit { putBoolean(KEY_PREMIUM_OFFLINE_MODE, value) }

    var temperatureUnit: String
        get() = preferences.getString(KEY_TEMPERATURE_UNIT, "fahrenheit") ?: "fahrenheit"
        set(value) = preferences.edit { putString(KEY_TEMPERATURE_UNIT, value) }

    var childSafetyEnabled: Boolean
        get() = preferences.getBoolean(KEY_CHILD_SAFETY, true)
        set(value) = preferences.edit { putBoolean(KEY_CHILD_SAFETY, value) }

    var childSafetySound: Boolean
        get() = preferences.getBoolean(KEY_CHILD_SAFETY_SOUND, true)
        set(value) = preferences.edit { putBoolean(KEY_CHILD_SAFETY_SOUND, value) }

    var childSafetyRed: Boolean
        get() = preferences.getBoolean(KEY_CHILD_SAFETY_RED, true)
        set(value) = preferences.edit { putBoolean(KEY_CHILD_SAFETY_RED, value) }

    var childSafetyGuide: Boolean
        get() = preferences.getBoolean(KEY_CHILD_SAFETY_GUIDE, true)
        set(value) = preferences.edit { putBoolean(KEY_CHILD_SAFETY_GUIDE, value) }

    var childSafetyLockShare: Boolean
        get() = preferences.getBoolean(KEY_CHILD_SAFETY_LOCK_SHARE, false)
        set(value) = preferences.edit { putBoolean(KEY_CHILD_SAFETY_LOCK_SHARE, value) }

}
