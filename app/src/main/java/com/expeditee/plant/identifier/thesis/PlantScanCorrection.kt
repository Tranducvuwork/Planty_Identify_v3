package com.expeditee.plant.identifier.thesis

import com.expeditee.plant.identifier.network.model.PlantScanResult
import java.text.Normalizer

/**
 * Fixes common scan/API misidentifications before catalog merge.
 * Trứng cá (Muntingia calabura) is often returned as Grewia asiatica (Phalsa)
 * or Phyllanthus when the model confuses small red-fruit trees/herbs.
 */
object PlantScanCorrection {
    const val TRUNG_CA_ID = "trung-ca"
    const val BONG_TRANG_ID = "bong-trang"
    const val DAM_BUT_ID = "dam-but"

    private val trungCaSignals = listOf(
        "trung ca",
        "trung-ca",
        "trungca",
        "muntingia",
        "jamaican cherry",
        "calabura",
        "panama berry",
        "capulin",
        "cerise",
        "strawberry tree",
        "cay trung ca",
        "so ri jamaica",
    )

    private val choDeSignals = listOf(
        "cho de",
        "diệp ha chau",
        "diet ha chau",
        "phyllanthus urinaria",
        "chamber bitter",
        "leafflower",
    )

    private val grewiaMisids = setOf(
        "grewia asiatica",
        "grewia occidentalis",
        "grewia",
    )

    private val hibiscusMisids = setOf(
        "hibiscus rosa-sinensis",
        "hibiscus rosa sinensis",
        "hibiscus",
    )

    private val ixoraMisids = setOf(
        "ixora coccinea",
        "ixora chinensis",
        "ixora",
    )

    private val ixoraSignals = listOf(
        "ixora",
        "bong trang",
        "bong-trang",
        "jungle geranium",
        "flame of the woods",
        "scarlet jungle flame",
        "coccinea",
        "cymose",
        "cyme",
        "cluster of small",
        "dense cluster",
        "cụm hoa",
        "cụm hoa tán",
    )

    private val damButSignals = listOf(
        "dam but",
        "dam-but",
        "bong but",
        "hoa dam but",
        "chinese hibiscus",
        "rose of china",
        "hoa lớn",
        "nhị dài",
        "prominent stamen",
    )

    fun catalogPlantId(result: PlantScanResult): String? {
        val blob = foldBlob(result)
        val scientific = fold(result.scientificName)

        if (scientific in grewiaMisids || scientific.startsWith("grewia ")) {
            return TRUNG_CA_ID
        }

        if (scientific in ixoraMisids || scientific.startsWith("ixora ")) {
            if (signalsDamBut(blob) && !signalsIxora(blob)) return DAM_BUT_ID
            return BONG_TRANG_ID
        }

        if (scientific in hibiscusMisids || scientific.startsWith("hibiscus ")) {
            if (signalsIxora(blob) && !signalsDamBut(blob)) return BONG_TRANG_ID
            if (familySignalsRubiaceae(result) && !familySignalsMalvaceae(result)) return BONG_TRANG_ID
            return DAM_BUT_ID
        }

        if (scientific.contains("phyllanthus") && signalsTrungCa(blob) && !signalsChoDe(blob)) {
            return TRUNG_CA_ID
        }

        if (signalsTrungCa(blob) && !signalsChoDe(blob)) {
            return TRUNG_CA_ID
        }

        if (signalsIxora(blob) && !signalsDamBut(blob)) {
            return BONG_TRANG_ID
        }

        return null
    }

    fun shouldPreferCatalog(scan: PlantScanResult, localPlantId: String?): Boolean {
        if (catalogPlantId(scan) == localPlantId) return true
        val scanSci = fold(scan.scientificName)
        val localId = localPlantId.orEmpty()
        if (localId == TRUNG_CA_ID && (scanSci in grewiaMisids || scanSci.startsWith("grewia "))) {
            return true
        }
        if (localId == TRUNG_CA_ID && scanSci.contains("phyllanthus") && signalsTrungCa(foldBlob(scan))) {
            return true
        }
        if (localId == BONG_TRANG_ID && (scanSci in hibiscusMisids || scanSci.startsWith("hibiscus "))) {
            return signalsIxora(foldBlob(scan))
        }
        if (localId == DAM_BUT_ID && (scanSci in ixoraMisids || scanSci.startsWith("ixora "))) {
            return signalsDamBut(foldBlob(scan))
        }
        return false
    }

    fun signalsTrungCa(text: String): Boolean {
        val blob = fold(text)
        return trungCaSignals.any { it in blob }
    }

    fun signalsIxora(text: String): Boolean {
        val blob = fold(text)
        return ixoraSignals.any { it in blob }
    }

    fun signalsDamBut(text: String): Boolean {
        val blob = fold(text)
        return damButSignals.any { it in blob }
    }

    private fun signalsChoDe(blob: String): Boolean = choDeSignals.any { it in blob }

    private fun familySignalsRubiaceae(result: PlantScanResult): Boolean {
        val family = fold(result.family)
        return family.contains("rubiaceae") || family.contains("ca phe")
    }

    private fun familySignalsMalvaceae(result: PlantScanResult): Boolean {
        val family = fold(result.family)
        return family.contains("malvaceae") || family.contains("thien qua")
    }

    private fun foldBlob(result: PlantScanResult): String = fold(
        buildString {
            append(result.scientificName).append(' ')
            result.commonNames.forEach { append(it).append(' ') }
            append(result.description.orEmpty()).append(' ')
            append(result.morphology.orEmpty()).append(' ')
            append(result.plantId.orEmpty()).append(' ')
            append(result.qrCode.orEmpty())
        },
    )

    private fun fold(value: String?): String {
        val nfd = Normalizer.normalize(value.orEmpty().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }
}
