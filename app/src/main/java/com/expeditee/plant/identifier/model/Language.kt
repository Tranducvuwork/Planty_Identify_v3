package com.expeditee.plant.identifier.model

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.DrawableRes
import com.expeditee.plant.identifier.R
import java.util.Locale

enum class Language(
    val id: Int,
    val country: String,
    val isoCountry: String,
    @DrawableRes
    val iconFlag: Int
) {
    ENGLISH(1, "English", "en", R.drawable.ic_english),
    GERMAN(2, "German", "de", R.drawable.ic_german),
    SPANISH(3, "Spanish", "es", R.drawable.ic_spanish),
    FILIPINO(4, "Filipino", "fil", R.drawable.ic_filipino),
    FRENCH(5, "French", "fr", R.drawable.ic_france),
    HINDI(6, "Hindi", "hi", R.drawable.ic_hindi),
    CROATIAN(7, "Croatian", "hr", R.drawable.ic_croatia),
    INDONESIAN(8, "Indonesian", "in", R.drawable.ic_indonesian),
    ITALIAN(9, "Italian", "it", R.drawable.ic_italian),
    JAPANESE(10, "Japanese", "ja", R.drawable.ic_japanese),
    KOREAN(11, "Korean", "ko", R.drawable.ic_korean),
    MALAY(12, "Malay", "ms", R.drawable.ic_malay),
    DUTCH(13, "Dutch", "nl", R.drawable.ic_dutch),
    POLISH(14, "Polish", "pl", R.drawable.ic_polish),
    PORTUGUESE(15, "Portuguese", "pt", R.drawable.ic_portugal),
    RUSSIAN(16, "Russian", "ru", R.drawable.ic_russian),
    SWEDISH(17, "Swedish", "sv", R.drawable.ic_swedish),
    TURKISH(18, "Turkish", "tr", R.drawable.ic_turkish),
    VIETNAMESE(19, "Vietnamese", "vi", R.drawable.ic_vietnamese);

    companion object {
        fun getAllLanguages(): List<Language> {
            val languages = Language.entries
            val currentLanguage = Resources.getSystem().configuration.locales[0].language
            if (languages.any { it.isoCountry == currentLanguage }) {
                val (matching, others) = languages.partition { it.isoCountry == currentLanguage }
                return matching + others
            }
            return languages
        }
    }

    @Suppress("DEPRECATION")
    fun applyLanguage(context: Context) {
        val newLocale = Locale(isoCountry)
        Locale.setDefault(newLocale)
        val configuration = Configuration()
        configuration.setLocale(newLocale)
        context.resources.updateConfiguration(
            configuration, context.resources.displayMetrics
        )
    }

    fun getEnglishDisplayName(): String {
        return Locale.forLanguageTag(isoCountry).getDisplayLanguage(Locale.ENGLISH)
    }
}
