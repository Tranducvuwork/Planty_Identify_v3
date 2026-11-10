package com.expeditee.plant.identifier.presenter.detail

class PlantDetailConditionFormatter {

    fun clean(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun formatList(values: List<String>?): String? {
        return values
            ?.mapNotNull { clean(it) }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
    }

    fun formatLabeled(label: String, value: String?): String? {
        val resolvedLabel = clean(label) ?: return null
        val resolvedValue = clean(value) ?: return null
        val separator = if (resolvedLabel.endsWith(":")) " " else ": "
        return "$resolvedLabel$separator$resolvedValue"
    }

    fun joinLines(main: String?, extras: List<String> = emptyList()): String? {
        val lines = mutableListOf<String>()
        clean(main)?.let(lines::add)
        lines.addAll(extras.mapNotNull { clean(it) })
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}
