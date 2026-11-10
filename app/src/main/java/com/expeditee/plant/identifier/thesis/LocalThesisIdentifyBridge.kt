package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.model.IdentifiedPlant
import com.expeditee.plant.identifier.network.model.PlantScanResult

/**
 * Maps offline TFLite / fallback identify results onto [thesis_plants.json] catalog data.
 */
object LocalThesisIdentifyBridge {
    fun enrichAndStore(
        context: Context,
        identified: IdentifiedPlant,
        imageUri: String?,
    ): PlantScanResult {
        val base = PlantScanResult(
            scientificName = identified.scientificName,
            commonNames = identified.commonNames.filter { it.isNotBlank() },
            confidence = (identified.confidence * 100.0).coerceIn(0.0, 100.0),
            description = identified.description,
            detailAvailable = true,
        )
        val enriched = ThesisPlantCatalog.enrich(context, base)
        PlantScanSession.store(enriched, imageUri)
        return enriched
    }

    fun matchedCatalog(context: Context, scientificName: String, commonNames: List<String>): Boolean {
        return ThesisPlantCatalog.findByName(context, scientificName, commonNames) != null ||
            LocalIdentifyTargets.find(context, scientificName) != null
    }
}
