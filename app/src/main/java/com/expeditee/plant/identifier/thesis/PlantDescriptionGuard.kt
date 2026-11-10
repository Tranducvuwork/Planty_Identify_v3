package com.expeditee.plant.identifier.thesis

import java.text.Normalizer

object PlantDescriptionGuard {
    private val otherPlantLeads = listOf(
        "hoa hong",
        "dam but",
        "ixora",
        "bong trang",
        "hoa giay",
        "hoa cuc",
        "hoa sen",
        "hoa hue",
        "nha dam",
        "trau ba",
        "luoi ho",
        "grewia",
        "quả sơn dương",
        "qua son duong",
        "thận quang",
        "than quang",
        "phalsa",
    )

    fun fits(description: String, plantNames: List<String>): Boolean {
        val desc = description.trim()
        if (desc.isEmpty()) return false
        val folded = fold(desc)
        val own = plantNames.map { fold(it) }.filter { it.length >= 3 }.distinct()
        if (own.any { it.isNotBlank() && folded.contains(it) }) return true
        if (otherPlantLeads.any { lead ->
                folded == lead || folded.startsWith("$lead ") || folded.startsWith("$lead la ")
            }
        ) {
            return false
        }
        return desc.length >= 48
    }

    private fun fold(value: String?): String {
        val nfd = Normalizer.normalize(value.orEmpty().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }
}
