package com.expeditee.plant.identifier.thesis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphologyImageLookupTest {
    @Test
    fun `search queries use scientific name and part`() {
        val queries = MorphologyImageLookup.searchQueries("Oryza sativa", MorphologyPart.LEAF)
        assertTrue(queries.any { it.contains("Oryza sativa") && it.contains("leaf") })
        assertTrue(queries.any { it.startsWith("Oryza ") })
    }

    @Test
    fun `pick rice leaf over disease title`() {
        val candidates = listOf(
            MorphologyCandidate(
                title = "Helminthosporium oryzae at Oryza sativa disease.jpg",
                url = "https://example.com/disease.jpg",
            ),
            MorphologyCandidate(
                title = "Oryza sativa leaf detail.jpg",
                url = "https://example.com/leaf.jpg",
            ),
        )
        val picked = MorphologyImageLookup.pickBest("Oryza sativa", MorphologyPart.LEAF, candidates)
        assertEquals("https://example.com/leaf.jpg", picked?.url)
    }

    @Test
    fun `pick apple fruit for malus`() {
        val candidates = listOf(
            MorphologyCandidate(
                title = "Malus domestica fruit ripe.jpg",
                url = "https://example.com/apple-fruit.jpg",
            ),
            MorphologyCandidate(
                title = "Random tree bark.jpg",
                url = "https://example.com/bark.jpg",
            ),
        )
        val picked = MorphologyImageLookup.pickBest("Malus domestica", MorphologyPart.FRUIT, candidates)
        assertEquals("https://example.com/apple-fruit.jpg", picked?.url)
    }

    @Test
    fun `parse commons json pages`() {
        val json =
            "{\"query\":{\"pages\":{\"1\":{" +
                "\"title\":\"File:Oryza sativa leaf.jpg\"," +
                "\"imageinfo\":[{\"url\":\"https://upload.wikimedia.org/a.jpg\"," +
                "\"thumburl\":\"https://upload.wikimedia.org/a-640.jpg\",\"mime\":\"image/jpeg\"}]" +
                "}}}}"
        val candidates = MorphologyImageLookup.parseCandidates(json)
        assertEquals(1, candidates.size)
        assertTrue(candidates.first().url.contains("upload.wikimedia.org"))
        assertEquals(
            "https://upload.wikimedia.org/a-640.jpg",
            MorphologyImageLookup.pickBest("Oryza sativa", MorphologyPart.LEAF, candidates)?.url,
        )
    }
}
