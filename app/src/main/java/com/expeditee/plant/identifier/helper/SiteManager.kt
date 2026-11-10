package com.expeditee.plant.identifier.helper

import android.content.Context
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.model.GardenOption
import java.util.UUID

object SiteManager {
        private const val PREF_NAME = "site_manager_pref"
        private const val KEY_SITES = "key_sites"
        const val DEFAULT_SITE_ID = "my_garden"
        const val ALL_SITE_ID = "all"

        fun getSites(context: Context): List<GardenOption> {
            val allSite = GardenOption(
                id = ALL_SITE_ID,
                name = context.getString(R.string.filter_all)
            )
            val defaultSite = GardenOption(
                id = DEFAULT_SITE_ID,
                name = context.getString(R.string.my_garden_default)
            )
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getStringSet(KEY_SITES, emptySet()).orEmpty()
                .mapNotNull { raw ->
                    val parts = raw.split("::", limit = 2)
                    if (parts.size == 2) GardenOption(parts[0], parts[1]) else null
                }
                .distinctBy { it.id }
            return listOf(allSite, defaultSite) + saved
        }
    fun createSite(context: Context, name: String): GardenOption {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Site name cannot be empty" }

        val newSite = GardenOption(
            id = "site_${UUID.randomUUID()}",
            name = trimmed
        )
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_SITES, emptySet()).orEmpty().toMutableSet()
        current.add("${newSite.id}::${newSite.name}")
        prefs.edit().putStringSet(KEY_SITES, current).apply()
        return newSite
    }
}
