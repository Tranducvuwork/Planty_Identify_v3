package com.expeditee.plant.identifier.helper.firebase

import com.expeditee.plant.identifier.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

class RemoteConfigManagement(private val remoteConfig: FirebaseRemoteConfig) {

    var isConfigFetched: Boolean = false
        private set

    private val defaultConfigs: Map<String, Any> = mapOf(
        ConfigKey.INTER_SPLASH to true,
        ConfigKey.NATIVE_LANGUAGE to true,
        ConfigKey.NATIVE_ONBOARDING_1 to true,
        ConfigKey.NATIVE_ONBOARDING_2 to true,
        ConfigKey.NATIVE_ONBOARDING_FULL to true,
        ConfigKey.NATIVE_ONBOARDING_LAST to true,
        ConfigKey.INTER_SHARED to true,
        ConfigKey.DISTANCE_TIME_SHOW_INTER to 20_000L,
        ConfigKey.NATIVE_HOME to true,
        ConfigKey.INTER_SPLASH_HIGH_MODE to true,
        ConfigKey.NATIVE_PERMISSION to true,
        ConfigKey.NATIVE_TEXT_TO_AUDIO to true,
        ConfigKey.INTER_PERMISSION to true,
        ConfigKey.ON_PREMIUM_AFTER_INTRO to true,
        ConfigKey.DEFAULT_SCAN_LIMIT to 5L,
        ConfigKey.ENABLE_FREE_TRIAL to true,
        ConfigKey.SMTP_USERNAME to "",
        ConfigKey.BASE_URL_DATA to "",
        ConfigKey.BASE_URL_WEATHER to "https://api.open-meteo.com/v1/",
        ConfigKey.SMTP_PASSWORD to "",
        ConfigKey.GITHUB_TOKEN to "",
        ConfigKey.GEMINI_MODEL to "gemini-2.5-flash-lite",
        ConfigKey.VERTEX_LOCATION to "us-central1",
        ConfigKey.ENABLE_BANNER_HOME to true,
        ConfigKey.ENABLE_NATIVE_SPLASH to true,
        ConfigKey.ENABLE_OPEN_RESUME to false,
        ConfigKey.ENABLE_LANGUAGE_AUTO_NEXT to false,
        ConfigKey.ENABLE_NEW_HOME_UI to true,
        ConfigKey.ENABLE_NEW_IAP_UI to true,
        ConfigKey.ENABLE_NEW_ONBOARDING_UI to true,
        ConfigKey.ENABLE_ONBOARDING_AUTO_NEXT to true
    )

    init {
        val configSettings = remoteConfigSettings {
            val intervalSeconds = if (BuildConfig.DEBUG) 0L else 3600L
            minimumFetchIntervalInSeconds = intervalSeconds
        }
        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(defaultConfigs)
        }
    }

    fun fetchRemoteConfig(runnable: Runnable) {
        remoteConfig
            .fetchAndActivate()
            .addOnCompleteListener {
                isConfigFetched = true
                runnable.run()
            }
    }

    internal val enableInterSplash get() = getBooleanValue(ConfigKey.INTER_SPLASH)
    internal val enableNativeLanguage get() = getBooleanValue(ConfigKey.NATIVE_LANGUAGE)
    internal val enableNativeOnboarding1 get() = getBooleanValue(ConfigKey.NATIVE_ONBOARDING_1)
    internal val enableNativeOnboarding2 get() = getBooleanValue(ConfigKey.NATIVE_ONBOARDING_2)
    internal val enableNativeOnboardingFull get() = getBooleanValue(ConfigKey.NATIVE_ONBOARDING_FULL)
    internal val enableNativeOnboardingLast get() = getBooleanValue(ConfigKey.NATIVE_ONBOARDING_LAST)
    internal val enableInterShared get() = getBooleanValue(ConfigKey.INTER_SHARED)
    internal val distanceTimeShowInter get() = getLongValue(ConfigKey.DISTANCE_TIME_SHOW_INTER)
    internal val enableInterSplashHighMode get() = getBooleanValue(ConfigKey.INTER_SPLASH_HIGH_MODE)
    internal val enableNativeSplash get() = getBooleanValue(ConfigKey.ENABLE_NATIVE_SPLASH)
    internal val enableOpenResume get() = getBooleanValue(ConfigKey.ENABLE_OPEN_RESUME)
    internal val enablePremiumAfterIntro get() = getBooleanValue(ConfigKey.ON_PREMIUM_AFTER_INTRO)
    internal val timeShowButtonCloseIAP get() = getLongValue(ConfigKey.ON_TIME_SHOW_BUTTON_CLOSE_IAP, 3000L)

    fun getSmtpUsername(): String {
        if (!isConfigFetched) return ""
        return getStringValue(ConfigKey.SMTP_USERNAME, "")
    }

    fun getSmtpPassword(): String {
        if (!isConfigFetched) return ""
        return getStringValue(ConfigKey.SMTP_PASSWORD, "")
    }

    val token: String get() = getStringValue(ConfigKey.GITHUB_TOKEN)
    val baseUrlWeather: String get() = getStringValue(ConfigKey.BASE_URL_WEATHER)
    val baseUrlData: String get() = getStringValue(ConfigKey.BASE_URL_DATA)

    val enableBannerHome: Boolean = getBooleanValue(ConfigKey.ENABLE_BANNER_HOME, true)

    internal val enableLanguageAutoNext: Boolean
        get() = getBooleanValue(ConfigKey.ENABLE_LANGUAGE_AUTO_NEXT, false)

    val enableNewHomeUi: Boolean
        get() = getBooleanValue(ConfigKey.ENABLE_NEW_HOME_UI, true)

    val enableNewIapUi: Boolean
        get() = getBooleanValue(ConfigKey.ENABLE_NEW_IAP_UI, true)

    val enableNewOnboardingUi: Boolean
        get() = getBooleanValue(ConfigKey.ENABLE_NEW_ONBOARDING_UI, true)

    val enableOnboardingAutoNext: Boolean
        get() = getBooleanValue(ConfigKey.ENABLE_ONBOARDING_AUTO_NEXT, true)

    val baseURLInvalid: Boolean
        get() = baseUrlData.isEmpty() ||
                !baseUrlData.startsWith("https://") ||
                !baseUrlData.startsWith("https://")

    object ConfigKey {
        const val INTER_SPLASH = "on_inter_splash"
        const val NATIVE_LANGUAGE = "on_native_language"
        const val NATIVE_ONBOARDING_1 = "on_native_onboarding_1"
        const val NATIVE_ONBOARDING_2 = "on_native_onboarding_2"
        const val NATIVE_ONBOARDING_FULL = "on_native_onboarding_full"
        const val NATIVE_ONBOARDING_LAST = "on_native_onboarding_last"
        const val INTER_SPLASH_HIGH_MODE = "on_inter_splash_high_mode"
        const val ENABLE_NATIVE_SPLASH = "on_native_splash"
        const val ENABLE_OPEN_RESUME = "on_open_resume"
        const val INTER_SHARED = "on_inter_shared"
        const val NATIVE_HOME = "on_native_home"
        const val DISTANCE_TIME_SHOW_INTER = "distance_time_show_inter_ads"
        const val NATIVE_PERMISSION = "on_native_permission"
        const val NATIVE_TEXT_TO_AUDIO = "on_native_text_to_audio"
        const val INTER_PERMISSION = "on_inter_permission"
        const val ON_PREMIUM_AFTER_INTRO = "on_premium_after_intro"
        const val ENABLE_FREE_TRIAL = "enable_free_trial"
        const val DEFAULT_SCAN_LIMIT = "default_scan_limit"
        const val ON_TIME_SHOW_BUTTON_CLOSE_IAP = "on_time_show_button_close_iap"
        const val SMTP_USERNAME = "config_smtp_username"
        const val SMTP_PASSWORD = "config_smtp_password"
        const val GITHUB_TOKEN = "token"
        const val BASE_URL_DATA = "base_url_data"
        const val BASE_URL_WEATHER = "weather_base_url"
        const val GEMINI_MODEL = "gemini_model"
        const val VERTEX_LOCATION = "vertex_location"
        const val ENABLE_BANNER_HOME = "on_banner_home"
        const val ENABLE_LANGUAGE_AUTO_NEXT = "enable_language_auto_next"
        const val ENABLE_NEW_HOME_UI = "enable_new_home_ui"
        const val ENABLE_NEW_IAP_UI = "enable_new_iap_ui"
        const val ENABLE_NEW_ONBOARDING_UI = "enable_new_onboarding_ui"
        const val ENABLE_ONBOARDING_AUTO_NEXT = "enable_onboarding_auto_next"
    }

    private fun getBooleanValue(key: String, defaultValue: Boolean = false): Boolean {
        if (!isConfigFetched) return defaultValue
        return key.runCatching(remoteConfig::getBoolean).getOrDefault(defaultValue)
    }

    private fun getLongValue(key: String, defaultValue: Long = 0L): Long {
        if (!isConfigFetched) return defaultValue
        return key.runCatching(remoteConfig::getLong).getOrDefault(defaultValue)
    }

    private fun getStringValue(key: String, defaultValue: String = ""): String {
        if (!isConfigFetched) return defaultValue
        return key.runCatching(remoteConfig::getString).getOrDefault(defaultValue)
    }
}
