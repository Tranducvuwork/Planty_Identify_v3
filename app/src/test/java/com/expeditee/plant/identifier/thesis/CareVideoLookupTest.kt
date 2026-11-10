package com.expeditee.plant.identifier.thesis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareVideoLookupTest {
    @Test
    fun `parse watch url`() {
        assertEquals(
            "kJQP7kiw5Fk",
            CareVideoLookup.parseVideoId("https://www.youtube.com/watch?v=kJQP7kiw5Fk"),
        )
    }

    @Test
    fun `parse bare id`() {
        assertEquals("abcdefghijk", CareVideoLookup.parseVideoId("abcdefghijk"))
    }

    @Test
    fun `reject search url`() {
        assertNull(
            CareVideoLookup.parseVideoId(
                "https://www.youtube.com/results?search_query=cach+trong+xa+lach",
            ),
        )
    }

    @Test
    fun `reject rickroll ids`() {
        assertNull(CareVideoLookup.parseVideoId("dQw4w9WgXcQ"))
        assertNull(CareVideoLookup.parseVideoId("xvFZjo5PgG0"))
        assertNull(
            CareVideoLookup.parseVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun `parse search hits skips rickroll and keeps plant video`() {
        val html = """
            {"videoRenderer":{"videoId":"dQw4w9WgXcQ","title":{"runs":[{"text":"Never Gonna Give You Up"}]}}}
            {"videoRenderer":{"videoId":"Abcdefghijk","title":{"runs":[{"text":"Cách trồng trầu bà Epipremnum"}]}}}
        """.trimIndent()
        val hits = CareVideoLookup.parseSearchHits(html)
        assertEquals(1, hits.size)
        assertEquals("Abcdefghijk", hits[0].videoId)
        val picked = CareVideoLookup.pickForPlant("Trầu bà", "Epipremnum aureum", hits)
        assertEquals("Abcdefghijk", picked?.videoId)
    }

    @Test
    fun `rickroll title is detected`() {
        assertTrue(CareVideoLookup.isRickrollTitle("Rick Astley - Never Gonna Give You Up"))
        assertFalse(CareVideoLookup.isRickrollTitle("Cách trồng trầu bà"))
    }

    @Test
    fun `parse unicode escaped mobile html`() {
        val html =
            "\\u0022videoRenderer\\u0022:{\\u0022videoId\\u0022:\\u0022Abcdefghijk\\u0022," +
                "\\u0022title\\u0022:{\\u0022runs\\u0022:[{\\u0022text\\u0022:\\u0022Cach trong Anthurium\\u0022}]}}"
        val hits = CareVideoLookup.parseSearchHits(html)
        assertEquals("Abcdefghijk", hits.firstOrNull()?.videoId)
    }

    @Test
    fun `pick mai chi thien not mai vang`() {
        val hits = listOf(
            CareVideoHit("MaiVangVid1", "Bí quyết trồng mai vàng đúng tết"),
            CareVideoHit("ChiThien11x", "CÁCH CHĂM SÓC CÂY MAI CHỈ THIÊN"),
            CareVideoHit("NhatChiMai", "Cách chăm sóc nhất chi mai"),
        )
        val picked = CareVideoLookup.pickForPlant(
            "Mai chỉ thiên",
            "Tabernaemontana divaricata",
            hits,
        )
        assertEquals("ChiThien11x", picked?.videoId)
    }

    @Test
    fun `prefer scientific genus over hong token`() {
        val hits = listOf(
            CareVideoHit("RoseVideo11", "Cách trồng hoa hồng"),
            CareVideoHit("AnthVideo11", "Cách trồng Anthurium hồng môn"),
        )
        val picked = CareVideoLookup.pickForPlant("Hoa hồng hạc", "Anthurium andraeanum", hits)
        assertEquals("AnthVideo11", picked?.videoId)
    }

    @Test
    fun `pick eclipta not co kieng`() {
        val hits = listOf(
            CareVideoHit("CoKiengVid", "Cách trồng cọ kiểng đẹp"),
            CareVideoHit("EcliptaVid", "Trồng cây nhọ nồi cỏ mực Eclipta prostrata"),
        )
        val picked = CareVideoLookup.pickForPlant(
            "Cỏ Nhọ Nồi",
            "Herba Ecliptae",
            hits,
        )
        assertEquals("EcliptaVid", picked?.videoId)
    }

    @Test
    fun `resolve herba ecliptae to binomial`() {
        assertEquals(
            "Eclipta prostrata",
            CareVideoLookup.resolveScientificName("Herba Ecliptae", "Cỏ Nhọ Nồi"),
        )
    }

    @Test
    fun `bestExisting prefers watch url over search`() {
        val citations = listOf(
            com.expeditee.plant.identifier.network.model.CitationData(
                type = "youtube",
                title = "Video: bón phân hợp lý cho Lúa",
                url = "https://www.youtube.com/watch?v=EV0_bEo0NyM",
                videoId = "EV0_bEo0NyM",
            ),
        )
        val picked = CareVideoLookup.bestExisting(citations)
        assertEquals("EV0_bEo0NyM", picked?.videoId)
        assertEquals("https://www.youtube.com/watch?v=EV0_bEo0NyM", picked?.url)
    }
}
