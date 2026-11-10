package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class IdentifiedPlant(
    val scientificName: String,
    val commonNames: List<String> = emptyList(),
    val confidence: Double,
    val description: String? = null,
    val matchedFromCatalog: Boolean = false,
)
