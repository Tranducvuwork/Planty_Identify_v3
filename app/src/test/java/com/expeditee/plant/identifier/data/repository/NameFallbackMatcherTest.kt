package com.expeditee.plant.identifier.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameFallbackMatcherTest {

    private val map = mapOf(
        "rose" to "Hoa hồng",
        "china rose" to "Hoa hồng",
        "lettuce" to "Xà lách",
        "tuberose" to "Hoa huệ",
        "rice" to "Lúa",
        "corn" to "Ngô",
    )

    @Test
    fun `exact rose name maps to hoa hong`() {
        assertEquals("Hoa hồng", NameFallbackMatcher.match("rose", map))
        assertEquals("Hoa hồng", NameFallbackMatcher.match("China rose", map))
    }

    @Test
    fun `rosette in a lettuce description is not hoa hong`() {
        assertNull(
            NameFallbackMatcher.match(
                "Lettuce is a leafy vegetable that forms a loose rosette of leaves.",
                map,
            ),
        )
    }

    @Test
    fun `tuberose prefers the longer key over rose substring`() {
        assertEquals("Hoa huệ", NameFallbackMatcher.match("tuberose", map))
    }

    @Test
    fun `rice is not matched inside price`() {
        assertNull(NameFallbackMatcher.match("fair price", map))
    }
}
