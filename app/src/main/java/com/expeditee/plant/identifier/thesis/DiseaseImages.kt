package com.expeditee.plant.identifier.thesis

import java.text.Normalizer

object DiseaseImages {
    fun urlFor(name: String?): String {
        val key = fold(name)
        return when {
            "dao on" in key || "blast" in key -> "pest_images/pest-dao-on.jpg"
            "cuon la" in key || "leaf roller" in key -> "pest_images/pest-sau-cuon-la.jpg"
            "duc qua" in key || "fruit borer" in key -> "pest_images/pest-sau-duc-qua.jpg"
            "duc than" in key || "stem borer" in key -> "pest_images/pest-sau-duc-than.jpg"
            key.contains("sap") || key.contains("ray") || key.contains("mealy") ->
                "pest_images/pest-ray-sun.jpg"
            "vang la" in key || "yellow" in key || "chloro" in key ->
                "pest_images/pest-vang-la.jpg"
            "chay la" in key || "blight" in key || "leaf scorch" in key ->
                "pest_images/pest-chay-la.jpg"
            key.contains("rep") || key.contains("aphid") -> "pest_images/pest-rep.jpg"
            key.contains("sau") || key.contains("caterpillar") || key.contains("an la") ->
                "pest_images/pest-sau.jpg"
            else -> "pest_images/pest-dom-la.jpg"
        }
    }

    private fun fold(value: String?): String {
        val nfd = Normalizer.normalize(value.orEmpty().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }
}

/** Offline placeholders only — species images load from Wikimedia by scientific name. */
object MorphologyImages {
    const val ROOT = "morphology_images/morph-re.jpg"
    const val STEM = "morphology_images/morph-than.jpg"
    const val LEAF = "morphology_images/morph-la.jpg"
    const val FLOWER = "morphology_images/morph-hoa.jpg"
    const val FRUIT = "morphology_images/morph-qua.jpg"
}

object FertilizerImages {
    const val ORGANIC = "fertilizer_images/fert-huu-co.jpg"
    const val NPK = "fertilizer_images/fert-npk.jpg"
    const val KALI = "fertilizer_images/fert-kali.jpg"
}
