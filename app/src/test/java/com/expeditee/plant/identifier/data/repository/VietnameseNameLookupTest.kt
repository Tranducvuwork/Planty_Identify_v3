package com.expeditee.plant.identifier.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseNameLookupTest {

    @Test
    fun `placeholder common name needs remote lookup`() {
        assertTrue(
            VietnameseNameLookup.needsRemoteLookup(
                commonNames = listOf("Cây chưa rõ tên Việt"),
                scientificName = "Averrhoa carambola",
            ),
        )
    }

    @Test
    fun `scientific-only names need remote lookup`() {
        assertTrue(
            VietnameseNameLookup.needsRemoteLookup(
                commonNames = listOf("Averrhoa carambola"),
                scientificName = "Averrhoa carambola",
            ),
        )
    }

    @Test
    fun `vietnamese name does not need remote lookup`() {
        assertFalse(
            VietnameseNameLookup.needsRemoteLookup(
                commonNames = listOf("Khế"),
                scientificName = "Averrhoa carambola",
            ),
        )
    }

    @Test
    fun `parse keeps a short vietnamese name`() {
        assertEquals("Khế", VietnameseNameLookup.parseModelAnswer("Khế"))
    }

    @Test
    fun `parse strips quotes and scientific parenthesis`() {
        assertEquals(
            "Khế",
            VietnameseNameLookup.parseModelAnswer("\"Khế (Averrhoa carambola)\""),
        )
    }

    @Test
    fun `parse rejects a thinking dump even if it mentions a vietnamese name`() {
        val dump = """
            Analyze User Input:
            The most common Vietnamese name for Averrhoa carambola is "Khế" or sometimes "Khế chua".
            Check Constraints and keep thinking.
        """.trimIndent()
        assertNull(VietnameseNameLookup.parseModelAnswer(dump))
    }
}
