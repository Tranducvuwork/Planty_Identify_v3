package com.expeditee.plant.identifier.data.repository

import com.expeditee.plant.identifier.model.ModeScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantIdentifyValidationTest {

    @Test
    fun `online identify accepts valid scientific name at threshold`() {
        val error = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = "Monstera deliciosa",
            confidence = 70.0,
            mode = ModeScan.ONLINE,
        )

        assertNull(error)
    }

    @Test
    fun `online identify rejects confidence below threshold`() {
        val error = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = "Monstera deliciosa",
            confidence = 69.0,
            mode = ModeScan.ONLINE,
        )

        assertEquals("Confidence 69.0 below threshold 70.0 for ONLINE mode", error)
    }

    @Test
    fun `offline identify accepts normalized confidence`() {
        val error = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = "Ficus lyrata",
            confidence = 0.30,
            mode = ModeScan.OFFLINE,
        )

        assertNull(error)
    }

    @Test
    fun `identify rejects unknown scientific names`() {
        val error = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = "Unknown",
            confidence = 95.0,
            mode = ModeScan.ONLINE,
        )

        assertEquals("Invalid scientificName: 'Unknown'", error)
    }
}
