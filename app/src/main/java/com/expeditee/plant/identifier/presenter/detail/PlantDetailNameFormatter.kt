package com.expeditee.plant.identifier.presenter.detail

class PlantDetailNameFormatter {

    fun clean(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun extractOtherName(scientificName: String?): String? {
        val normalized = clean(scientificName) ?: return null
        val tokens = normalized.split(Regex("\\s+"))
        return tokens.drop(1).joinToString(" ").takeIf { it.isNotBlank() }
    }

    fun extractGenus(scientificName: String?): String? {
        val normalized = clean(scientificName) ?: return null
        return normalized.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }
    }
}
