package com.expeditee.plant.identifier.thesis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPestIllustrationTest {

    @Test
    fun `rice pest shape question matches`() {
        assertTrue(
            ChatPestIllustration.matchesPestIllustrationQuestion(
                "có hình dạng của sâu bệnh cho cây lúa không",
            ),
        )
    }

    @Test
    fun `planting question does not match`() {
        assertFalse(
            ChatPestIllustration.matchesPestIllustrationQuestion("cách trồng cây lúa"),
        )
    }

    @Test
    fun `pest image urls are local assets`() {
        val urls = listOf(
            DiseaseImages.urlFor("Đạo ôn"),
            DiseaseImages.urlFor("Sâu cuốn lá"),
            DiseaseImages.urlFor("Vàng lá"),
        )
        assertTrue(urls.all { it.startsWith("pest_images/") })
    }
}
