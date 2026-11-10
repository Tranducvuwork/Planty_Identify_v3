package com.expeditee.plant.identifier.presenter.detail

class PlantDetailOverviewFormatter {

    fun formatOverview(
        detailDescription: String?,
        catalogEntryLine: String?,
        scientificNameLine: String?,
        previewNotice: String?,
    ): String? {
        clean(detailDescription)?.let { return it }

        val sections = listOfNotNull(
            clean(catalogEntryLine),
            clean(scientificNameLine),
            clean(previewNotice),
        )
        return sections.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
    }

    private fun clean(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }
}
