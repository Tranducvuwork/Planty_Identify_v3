package com.expeditee.plant.identifier.thesis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantDescriptionGuardTest {

    @Test
    fun `bare hoa hong does not fit lettuce`() {
        assertFalse(
            PlantDescriptionGuard.fits(
                description = "Hoa hồng",
                plantNames = listOf("Xà lách", "Lactuca sativa"),
            ),
        )
    }

    @Test
    fun `rose paragraph does not fit lettuce`() {
        assertFalse(
            PlantDescriptionGuard.fits(
                description = "Hoa hồng là cây bụi hoặc cây leo thân có gai, lá kép mép răng cưa và hoa có nhiều màu sắc.",
                plantNames = listOf("Xà lách", "Lactuca sativa", "Lettuce"),
            ),
        )
    }

    @Test
    fun `lettuce paragraph fits lettuce`() {
        assertTrue(
            PlantDescriptionGuard.fits(
                description = "Xà lách (Lactuca sativa) là rau ăn lá ngắn ngày, lá mềm xếp thành hoa thị hoặc cuốn bắp tùy giống.",
                plantNames = listOf("Xà lách", "Lactuca sativa"),
            ),
        )
    }
}
