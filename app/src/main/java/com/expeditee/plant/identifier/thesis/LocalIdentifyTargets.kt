package com.expeditee.plant.identifier.thesis

import android.content.Context
import com.expeditee.plant.identifier.network.model.PlantScanData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.text.Normalizer

/**
 * Priority species for offline TFLite training / catalog matching (see assets/identify_local_targets.json).
 */
object LocalIdentifyTargets {
    private val gson = Gson()
    private var cache: List<Target>? = null

    data class Target(
        @SerializedName("plant_id") val plantId: String,
        @SerializedName("scientific_name") val scientificName: String,
        @SerializedName("common_names") val commonNames: List<String> = emptyList(),
        @SerializedName("in_taxonomy") val inTaxonomy: Boolean = false,
        @SerializedName("taxonomy_leaf_id") val taxonomyLeafId: Int? = null,
        @SerializedName("priority") val priority: String? = null,
    )

    fun all(context: Context): List<Target> {
        cache?.let { return it }
        val loaded = runCatching {
            context.assets.open("identify_local_targets.json").bufferedReader().use { reader ->
                val file = gson.fromJson(reader, TargetsFile::class.java)
                file.targets + file.medicineTargets
            }
        }.getOrDefault(emptyList())
        cache = loaded
        return loaded
    }

    fun find(context: Context, scientificName: String): PlantScanData? {
        val folded = fold(scientificName)
        if (folded.isBlank()) return null
        val target = all(context).firstOrNull { fold(it.scientificName) == folded }
            ?: all(context).firstOrNull { folded.startsWith(fold(it.scientificName).substringBefore(" ")) }
            ?: return null
        return ThesisPlantCatalog.findByQrOrId(context, target.plantId)
    }

    fun plantIds(context: Context): List<String> = all(context).map { it.plantId }

    private data class TargetsFile(
        val targets: List<Target> = emptyList(),
        @SerializedName("medicine_targets") val medicineTargets: List<Target> = emptyList(),
    )

    private fun fold(value: String): String {
        val nfd = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }
}
