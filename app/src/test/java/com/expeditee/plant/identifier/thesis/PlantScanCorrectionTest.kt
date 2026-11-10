package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.PlantScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantScanCorrectionTest {

    @Test
    fun `grewia asiatica scan maps to trung ca catalog`() {
        val result = PlantScanResult(
            scientificName = "Grewia asiatica",
            commonNames = listOf("Phalsa", "Indian sherbet berry"),
            confidence = 85.0,
            description = "Grewia asiatica is a small tree known for edible berries.",
        )
        assertEquals(PlantScanCorrection.TRUNG_CA_ID, PlantScanCorrection.catalogPlantId(result))
        assertTrue(PlantScanCorrection.shouldPreferCatalog(result, PlantScanCorrection.TRUNG_CA_ID))
    }

    @Test
    fun `jamaican cherry common name signals trung ca`() {
        val result = PlantScanResult(
            scientificName = "Unknown",
            commonNames = listOf("Jamaican cherry"),
            confidence = 70.0,
        )
        assertEquals(PlantScanCorrection.TRUNG_CA_ID, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `phyllanthus without trung ca signals is not corrected`() {
        val result = PlantScanResult(
            scientificName = "Phyllanthus urinaria",
            commonNames = listOf("Chó đẻ"),
            confidence = 88.0,
        )
        assertEquals(null, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `trung ca vietnamese name signals correction`() {
        assertTrue(PlantScanCorrection.signalsTrungCa("có hình dạng sâu bệnh cho cây trứng cá"))
    }

    @Test
    fun `hibiscus scan with ixora common name maps to bong trang`() {
        val result = PlantScanResult(
            scientificName = "Hibiscus rosa-sinensis",
            commonNames = listOf("Ixora", "Jungle geranium"),
            confidence = 82.0,
        )
        assertEquals(PlantScanCorrection.BONG_TRANG_ID, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `hibiscus scan with rubiaceae family maps to bong trang`() {
        val result = PlantScanResult(
            scientificName = "Hibiscus rosa-sinensis",
            commonNames = listOf("Chinese hibiscus"),
            family = "Rubiaceae",
            confidence = 80.0,
        )
        assertEquals(PlantScanCorrection.BONG_TRANG_ID, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `ixora scan maps to bong trang catalog`() {
        val result = PlantScanResult(
            scientificName = "Ixora coccinea",
            commonNames = listOf("Jungle geranium"),
            confidence = 88.0,
        )
        assertEquals(PlantScanCorrection.BONG_TRANG_ID, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `true hibiscus scan maps to dam but`() {
        val result = PlantScanResult(
            scientificName = "Hibiscus rosa-sinensis",
            commonNames = listOf("Chinese hibiscus", "Dâm bụt"),
            family = "Malvaceae",
            confidence = 90.0,
        )
        assertEquals(PlantScanCorrection.DAM_BUT_ID, PlantScanCorrection.catalogPlantId(result))
    }

    @Test
    fun `description guard rejects ixora text for dam but`() {
        val desc = "Ixora coccinea là cây bụi với cụm hoa tán màu đỏ."
        assertFalse(
            PlantDescriptionGuard.fits(
                desc,
                listOf("Dâm bụt", "Hibiscus rosa-sinensis"),
            ),
        )
    }
}
