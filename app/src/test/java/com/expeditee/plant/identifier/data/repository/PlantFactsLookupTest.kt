package com.expeditee.plant.identifier.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantFactsLookupTest {

    @Test
    fun `generic school-garden uses need nvidia facts`() {
        assertTrue(
            PlantFactsLookup.needsFacts(
                uses = listOf("Cây cảnh trong nhà, phù hợp HS làm vườn."),
                description = "Phù hợp học sinh làm vườn.",
            ),
        )
    }

    @Test
    fun `thin catalog uses need nvidia facts`() {
        assertTrue(
            PlantFactsLookup.needsFacts(
                uses = listOf("Rau ăn lá", "Canh"),
                description = "Rau dền dễ trồng.",
            ),
        )
    }

    @Test
    fun `specific uses do not need nvidia facts`() {
        assertFalse(
            PlantFactsLookup.needsFacts(
                uses = listOf("Quả ăn tươi, làm nước ép và món tráng miệng."),
                description = "Khế là cây ăn quả nhiệt đới, quả có hình sao khi cắt ngang.",
            ),
        )
    }

    @Test
    fun `parse json facts strips markdown and reads uses`() {
        val raw = """
            ```json
            {"description":"Khế là cây ăn quả.","uses":["Ăn tươi","Nấu canh"],"origin":"Đông Nam Á","planting":"Trồng nắng","care":"Tưới vừa"}
            ```
        """.trimIndent()
        val facts = PlantFactsLookup.parseFacts(raw)
        assertEquals("Khế là cây ăn quả.", facts?.description)
        assertEquals(listOf("Ăn tươi", "Nấu canh"), facts?.uses)
        assertEquals("Đông Nam Á", facts?.origin)
    }

    @Test
    fun `user override replaces filled fields and keeps missing ones`() {
        val merged = PlantUserOverride.merge(
            commonNames = listOf("Starfruit"),
            description = "Server desc",
            uses = listOf("Server use"),
            imageUrl = "http://server",
            origin = "Server origin",
            override = PlantUserOverride(
                scientificName = "Averrhoa carambola",
                commonNames = listOf("Khế"),
                description = "User desc",
                uses = listOf("Ăn tươi"),
                imagePath = "/tmp/khe.jpg",
            ),
        )
        assertEquals(listOf("Khế"), merged.commonNames)
        assertEquals("User desc", merged.description)
        assertEquals(listOf("Ăn tươi"), merged.uses)
        assertEquals("/tmp/khe.jpg", merged.imageUrl)
        assertEquals("Server origin", merged.origin)
    }
}
