package com.expeditee.plant.identifier.data.repository

object NameFallbackMatcher {
    fun match(text: String, map: Map<String, String>): String? {
        val key = text.trim().lowercase()
        if (key.isBlank()) return null
        map[key]?.let { return it }
        if (!isNameLike(key)) return null
        return map.entries
            .filter { (english, _) -> english.length >= 3 && containsWholePhrase(key, english) }
            .maxByOrNull { it.key.length }
            ?.value
    }

    private fun isNameLike(key: String): Boolean {
        if (key.length > 48) return false
        val words = key.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.size <= 6 && !key.contains('.')
    }

    private fun containsWholePhrase(haystack: String, needle: String): Boolean {
        if (needle.any { it.isWhitespace() }) return haystack.contains(needle)
        return Regex("(?<![a-z0-9])${Regex.escape(needle)}(?![a-z0-9])").containsMatchIn(haystack)
    }
}
