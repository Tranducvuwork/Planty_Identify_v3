package com.expeditee.plant.identifier.data.upload

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanImageSizingTest {

    @Test
    fun `wide image does not oversample a short edge`() {
        val sampleSize = ScanImageSizing.calculateSampleSize(
            width = 8000,
            height = 1000,
            maxDimension = 1280,
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `large square image downsamples until both edges fit`() {
        val sampleSize = ScanImageSizing.calculateSampleSize(
            width = 4000,
            height = 4000,
            maxDimension = 1920,
        )

        assertEquals(2, sampleSize)
    }
}
