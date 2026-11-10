package com.expeditee.plant.identifier.thesis

import org.junit.Assert.assertEquals
import org.junit.Test

class DiseaseImagesTest {
    @Test
    fun `maps mealybugs before generic aphids`() {
        assertEquals("pest_images/pest-ray-sun.jpg", DiseaseImages.urlFor("Rệp sáp"))
        assertEquals("pest_images/pest-ray-sun.jpg", DiseaseImages.urlFor("Rầy sáp"))
    }

    @Test
    fun `maps aphids`() {
        assertEquals("pest_images/pest-rep.jpg", DiseaseImages.urlFor("Rệp"))
        assertEquals("pest_images/pest-rep.jpg", DiseaseImages.urlFor("Rệp muội"))
    }

    @Test
    fun `maps caterpillar`() {
        assertEquals("pest_images/pest-sau.jpg", DiseaseImages.urlFor("Sâu ăn lá"))
    }

    @Test
    fun `maps leaf spot fallback`() {
        assertEquals("pest_images/pest-dom-la.jpg", DiseaseImages.urlFor("Đốm lá do ẩm"))
    }

    @Test
    fun `maps rice blast before yellow leaf`() {
        assertEquals("pest_images/pest-dao-on.jpg", DiseaseImages.urlFor("Đạo ôn"))
        assertEquals("pest_images/pest-dao-on.jpg", DiseaseImages.urlFor("Đạo ôn / vàng lá"))
    }

    @Test
    fun `maps leaf roller and stem borer before generic caterpillar`() {
        assertEquals("pest_images/pest-sau-cuon-la.jpg", DiseaseImages.urlFor("Sâu cuốn lá"))
        assertEquals("pest_images/pest-sau-duc-than.jpg", DiseaseImages.urlFor("Sâu đục thân"))
        assertEquals("pest_images/pest-sau-duc-qua.jpg", DiseaseImages.urlFor("Sâu đục quả"))
    }

    @Test
    fun `maps yellow leaf and blight`() {
        assertEquals("pest_images/pest-vang-la.jpg", DiseaseImages.urlFor("Vàng lá"))
        assertEquals("pest_images/pest-chay-la.jpg", DiseaseImages.urlFor("Bệnh cháy lá"))
    }
}
