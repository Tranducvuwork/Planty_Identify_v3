package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.net.Uri
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.expeditee.plant.identifier.data.repository.PlantFactsLookup
import com.expeditee.plant.identifier.data.repository.VietnameseNameLookup
import com.expeditee.plant.identifier.network.model.CareData
import com.expeditee.plant.identifier.network.model.CitationData
import com.expeditee.plant.identifier.network.model.CommonDiseaseData
import com.expeditee.plant.identifier.network.model.PlantScanData
import com.expeditee.plant.identifier.network.model.PlantScanResult
import com.expeditee.plant.identifier.network.model.ToxicityData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.Normalizer

object ThesisPlantCatalog {
    private val gson = Gson()
    private var cache: List<PlantScanData>? = null

    fun all(context: Context): List<PlantScanData> {
        cache?.let { return it }
        val loaded = try {
            context.assets.open("thesis_plants.json").bufferedReader().use { reader ->
                val type = object : TypeToken<List<PlantScanData>>() {}.type
                gson.fromJson<List<PlantScanData>>(reader, type).orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
        val existingIds = loaded.mapNotNull { it.plantId }.toSet()
        val merged = loaded + MedicinalDocumentCatalog.entries.filter { it.plantId !in existingIds }
        cache = merged
        return merged
    }

    fun medicinal(context: Context): List<PlantScanData> =
        all(context).filter { PlantGroup.from(it) == PlantGroup.MEDICINE }

    fun byGroup(context: Context, group: PlantGroup): List<PlantScanData> =
        all(context).filter { plant ->
            val g = PlantGroup.from(plant)
            when (group) {
                PlantGroup.AGRICULTURE -> {
                    val isMedicinal = plant.isMedicinal == true ||
                        !plant.medicinalLabel.isNullOrBlank() ||
                        g == PlantGroup.MEDICINE
                    !isMedicinal && (
                        g == PlantGroup.AGRICULTURE ||
                            g == PlantGroup.VEGETABLE ||
                            g == PlantGroup.FRUIT
                        )
                }
                PlantGroup.MEDICINE -> g == PlantGroup.MEDICINE
                PlantGroup.MUSHROOM -> g == PlantGroup.MUSHROOM || g == PlantGroup.TOXIC && plant.toxicity?.isFungus == true
                else -> g == group
            }
        }

    fun findByQrOrId(context: Context, raw: String): PlantScanData? {
        val code = normalizeQr(raw)
        if (code.isBlank()) return null
        return all(context).firstOrNull { plant ->
            normalizeQr(plant.plantId.orEmpty()) == code ||
                normalizeQr(plant.qrCode.orEmpty()) == code
        } ?: findByName(context, code, emptyList())
    }

    fun findByName(context: Context, scientific: String?, commonNames: List<String>): PlantScanData? {
        val foldedScientific = fold(scientific).takeIf { it.length >= 3 }
        if (!foldedScientific.isNullOrBlank()) {
            all(context).firstOrNull { plant ->
                fold(plant.scientificName) == foldedScientific
            }?.let { return it }

            scientificAliases(foldedScientific).firstNotNullOfOrNull { alias ->
                all(context).firstOrNull { plant ->
                    containsNameToken(plantSearchHaystack(plant), alias)
                }
            }?.let { return it }
        }

        val tokens = buildList {
            commonNames.forEach { add(fold(it)) }
        }.flatMap { scientificAliases(it) }
            .filter { it.length >= 3 }
            .distinct()
        if (tokens.isEmpty()) return null
        return all(context).firstOrNull { plant ->
            val hay = plantSearchHaystack(plant)
            val ids = setOf(normalizeQr(plant.plantId.orEmpty()), normalizeQr(plant.qrCode.orEmpty()))
            tokens.any { containsNameToken(hay, it) } ||
                tokens.any { normalizeQr(it) in ids }
        }
    }

    private fun containsNameToken(haystack: String, token: String): Boolean {
        val value = token.trim()
        if (!isSpecificNameToken(value)) return false
        return " $value " in " $haystack "
    }

    private fun isSpecificNameToken(token: String): Boolean {
        if (token.length < 4) return false
        return token !in GENERIC_NAME_TOKENS
    }

    private fun plantSearchHaystack(plant: PlantScanData): String = buildString {
        append(fold(plant.scientificName))
        append(' ')
        plant.commonNames.orEmpty().forEach { append(fold(it)).append(' ') }
        plant.plantId?.let { append(fold(it)).append(' ') }
        plant.qrCode?.let { append(fold(it)).append(' ') }
    }

    private fun scientificAliases(foldedScientific: String): List<String> = when {
        foldedScientific.contains("eclipta") ||
            foldedScientific.contains("herba ecliptae") -> listOf(
                "eclipta prostrata",
                "eclipta alba",
                "herba ecliptae",
                "co nho noi",
                "co muc",
                "false daisy",
                "bhringraj",
                "bhringaraj",
                "trailing eclipta",
                "white eclipta",
                "karisalankanni",
                "kehraj",
            )
            foldedScientific.contains("scoparia") -> listOf(
                "scoparia dulcis",
                "cam thao dat",
            )
        foldedScientific.contains("dracaena fragrans") -> listOf(
            "dracaena fragrans",
            "thiet moc lan",
            "phat tai",
            "phat du thom",
            "corn plant",
            "mass cane",
        )
        foldedScientific.contains("dieffenbachia") -> listOf(
            "dieffenbachia seguine",
            "dieffenbachia spp",
            "dieffenbachia sp",
            "ngoc ngan",
            "dumb cane",
            "leopard lily",
        )
        foldedScientific.contains("aglaonema") -> listOf(
            "aglaonema",
            "dai phu gia",
        )
        foldedScientific.contains("amaranthus") ||
            foldedScientific.contains("rau den") -> listOf(
                "amaranthus tricolor",
                "amaranthus viridis",
                "amaranthus hybridus",
                "amaranthus spinosus",
                "rau den",
                "den com",
                "den do",
                "green amaranth",
            )
        foldedScientific.contains("sphagneticola") ||
            foldedScientific.contains("wedelia") -> listOf(
                "sphagneticola trilobata",
                "wedelia trilobata",
                "sai dat",
                "sai dat canh",
                "cuc mat troi",
                "cuc xuyen chi bo",
                "singapore daisy",
                "creeping oxeye",
            )
        foldedScientific.contains("dracaena trifasciata") ||
            foldedScientific.contains("sansevieria") -> listOf(
                "dracaena trifasciata",
                "sansevieria trifasciata",
                "sansevieria",
                "luoi ho",
                "cay luoi ho",
                "snake plant",
                "mother in law tongue",
            )
        foldedScientific.contains("callisia fragrans") ||
            foldedScientific.contains("callisia") -> listOf(
                "callisia fragrans",
                "luoc vang",
                "cay luoc vang",
                "lan chi",
                "basket plant",
            )
        foldedScientific.contains("peperomia pellucida") ||
            (foldedScientific.contains("peperomia") && foldedScientific.contains("pellucida")) -> listOf(
                "peperomia pellucida",
                "tan day la",
                "cang cua",
                "rau cang cua",
                "shiny bush",
            )
        foldedScientific.contains("pseuderanthemum") -> listOf(
            "pseuderanthemum reticulatum",
            "sao nhai",
            "cay sao nhai",
            "la cam vang",
            "golden false eranthemum",
        )
        foldedScientific.contains("polianthes") ||
            foldedScientific.contains("tuberosa") -> listOf(
                "polianthes tuberosa",
                "agave amica",
                "hoa hue",
                "hue trang",
                "hue",
                "tuberose",
            )
        foldedScientific.contains("artemisia argyi") ||
            foldedScientific.contains("cuc tan") ||
            foldedScientific.contains("ai diep") ||
            foldedScientific.contains("ngai diep") -> listOf(
                "artemisia argyi",
                "cuc tan",
                "ai diep",
                "ngai diep",
                "argy wormwood",
                "mugwort leaf",
            )
        foldedScientific.contains("artemisia") ||
            foldedScientific.contains("herba artemisiae") -> listOf(
                "artemisia vulgaris",
                "herba artemisiae vulgaris",
                "ngai cuu",
                "mugwort",
            )
        foldedScientific.contains("rosa chinensis") ||
            foldedScientific.contains("rosa") -> listOf(
                "rosa chinensis",
                "hoa hong",
                "hong",
                "china rose",
                "chinese rose",
            )
        foldedScientific.contains("chrysanthemum") ||
            foldedScientific.contains("flos chrysanthemi") -> listOf(
                "chrysanthemum morifolium",
                "chrysanthemum indicum",
                "flos chrysanthemi indici",
                "hoa cuc",
                "cuc hoa vang",
                "cuc",
            )
        foldedScientific.contains("hibiscus rosa") ||
            (foldedScientific.contains("hibiscus") && !foldedScientific.contains("ixora")) -> listOf(
                "hibiscus rosa-sinensis",
                "hibiscus rosa sinensis",
                "dam but",
                "bong but",
                "hoa dam but",
                "chinese hibiscus",
            )
        foldedScientific.contains("ixora") -> listOf(
                "ixora coccinea",
                "ixora chinensis",
                "bong trang",
                "trang",
                "jungle geranium",
                "flame of the woods",
            )
        foldedScientific.contains("muntingia") ||
            foldedScientific.contains("trung ca") ||
            foldedScientific.contains("trung-ca") -> listOf(
                "muntingia calabura",
                "muntingia",
                "trung ca",
                "cay trung ca",
                "jamaican cherry",
                "calabura",
                "panama berry",
                "capulin",
                "strawberry tree",
                "so ri jamaica",
            )
        foldedScientific.contains("terminalia catappa") ||
            foldedScientific.contains("terminalia") && foldedScientific.contains("catappa") ||
            foldedScientific.contains("cay bang") ||
            foldedScientific.contains("cay-bang") -> listOf(
                "terminalia catappa",
                "cay bang",
                "bang",
                "tropical almond",
                "indian almond",
            )
        foldedScientific.contains("delonix regia") ||
            foldedScientific.contains("delonix") ||
            foldedScientific.contains("phuong vi") -> listOf(
                "delonix regia",
                "phuong vi",
                "phuong",
                "royal poinciana",
                "flame tree",
            )
        foldedScientific.contains("bougainvillea") -> listOf(
                "bougainvillea glabra",
                "bougainvillea spectabilis",
                "hoa giay",
                "bong giay",
                "paper flower",
                "great bougainvillea",
            )
        foldedScientific.contains("lagenaria") -> listOf(
                "lagenaria siceraria",
                "bau",
                "bau bi",
                "bottle gourd",
                "calabash",
                "ho lo",
            )
        foldedScientific.contains("benincasa") -> listOf(
                "benincasa hispida",
                "bi xanh",
                "bi dao",
                "wax gourd",
                "winter melon",
            )
        foldedScientific.contains("gnetum") ||
            foldedScientific.contains("la giang") ||
            foldedScientific.contains("cay giang") -> listOf(
                "gnetum gnemon",
                "gnetum",
                "la giang",
                "cay giang",
                "melinjo",
            )
        foldedScientific.contains("andrographis") ||
            foldedScientific.contains("mat gau") ||
            foldedScientific.contains("xuyen tam lien") -> listOf(
                "andrographis paniculata",
                "mat gau",
                "cay mat gau",
                "xuyen tam lien",
                "king of bitters",
                "creat",
            )
        foldedScientific.contains("taraxacum") ||
            foldedScientific.contains("bo cong anh") -> listOf(
                "taraxacum officinale",
                "bo cong anh",
                "cuc dai",
                "dandelion",
            )
        foldedScientific.contains("lonicera japonica") ||
            (foldedScientific.contains("lonicera") && foldedScientific.contains("japonica")) -> listOf(
                "lonicera japonica",
                "kim ngan hoa",
                "nhan dong",
                "honeysuckle",
                "japanese honeysuckle",
            )
        foldedScientific.contains("hedyotis") ||
            foldedScientific.contains("oldenlandia diffusa") ||
            foldedScientific.contains("hoan ngoc") -> listOf(
                "hedyotis diffusa",
                "oldenlandia diffusa",
                "hoan ngoc",
                "cay hoan ngoc",
                "bach hoa xa thiet thao",
                "snake tongue grass",
            )
        foldedScientific.contains("orthosiphon") ||
            foldedScientific.contains("xuong khop") ||
            foldedScientific.contains("rau meo") -> listOf(
                "orthosiphon aristatus",
                "tra xuong khop",
                "cay xuong khop",
                "rau meo",
                "java tea",
                "cats whiskers",
            )
        foldedScientific.contains("curculigo") ||
            foldedScientific.contains("sam cau") -> listOf(
                "curculigo orchioides",
                "sam cau",
                "mach mon den",
                "golden eye grass",
            )
        foldedScientific.contains("allium tuberosum") ||
            foldedScientific.contains("garlic chives") ||
            (foldedScientific.contains("allium") && foldedScientific.contains("tuberosum")) -> listOf(
                "allium tuberosum",
                "he",
                "ca he",
                "garlic chives",
                "chinese chives",
            )
        foldedScientific.contains("salvia miltiorrhiza") ||
            foldedScientific.contains("dan sam") ||
            foldedScientific.contains("danshen") -> listOf(
                "salvia miltiorrhiza",
                "dan sam",
                "sam dan",
                "danshen",
                "red sage",
                "chinese sage",
            )
        foldedScientific.contains("actaea cimicifuga") ||
            foldedScientific.contains("cimicifuga foetida") ||
            foldedScientific.contains("trinh nu hoang cung") ||
            foldedScientific.contains("thang ma") -> listOf(
                "actaea cimicifuga",
                "cimicifuga foetida",
                "trinh nu hoang cung",
                "thang ma",
                "sheng ma",
                "black cohosh",
            )
        else -> listOf(foldedScientific)
    }.map { fold(it) }.filter { it.length >= 3 }.distinct()

    private val GENERIC_NAME_TOKENS = setOf(
        "plant",
        "tree",
        "leaf",
        "leaves",
        "flower",
        "grass",
        "vine",
        "herb",
        "cay",
        "hoa",
        "la",
        "thuc vat",
        "cay thuoc",
        "duoc lieu",
        "unknown",
        "healthy",
    )

    fun toResult(data: PlantScanData): PlantScanResult = completeForDisplay(PlantScanResult(
        scientificName = data.scientificName.orEmpty(),
        commonNames = data.commonNames.orEmpty(),
        confidence = (data.confidence ?: 80).toDouble(),
        description = data.description,
        plantId = data.plantId,
        qrCode = data.qrCode,
        isMedicinal = data.isMedicinal == true,
        medicinalLabel = data.medicinalLabel,
        family = data.family,
        plantGroup = data.plantGroup,
        morphology = data.morphology,
        origin = data.origin,
        uses = data.uses.orEmpty(),
        toxicity = data.toxicity,
        care = data.care,
        commonDiseases = data.commonDiseases.orEmpty(),
        folkRemedies = data.folkRemedies.orEmpty(),
        medicalDisclaimer = data.medicalDisclaimer,
        detailAvailable = true,
        imageUrl = data.imageUrl,
    ))

    fun enrich(context: Context, result: PlantScanResult): PlantScanResult {
        val correctionId = PlantScanCorrection.catalogPlantId(result)
        val local = correctionId?.let { findByQrOrId(context, it) }
            ?: findByName(context, result.scientificName, result.commonNames)
            ?: LocalIdentifyTargets.find(context, result.scientificName)
            ?: findByQrOrId(context, result.plantId.orEmpty())
        val merged = when {
            local != null && PlantScanCorrection.shouldPreferCatalog(result, local.plantId) ->
                overrideWithLocal(local, result)
            local != null -> mergeLocal(result, local)
            else -> result
        }
        return completeForDisplay(OrchidVarietyCatalog.enhance(applyToxicHeuristics(merged)))
    }

    fun completeForDisplay(result: PlantScanResult): PlantScanResult {
        val vnNames = vietnameseNames(result.commonNames, result.scientificName)
        val display = vnNames.first()
        val uses = usableUses(result.uses)
            .ifEmpty { defaultUses(display, result.isMedicinal) }
        val description = usableDescription(result.description)
            ?.takeIf { PlantDescriptionGuard.fits(it, vnNames + listOf(result.scientificName, display)) }
            ?: defaultDescription(display, result.isMedicinal)
        val group = PlantGroup.from(result)
        return result.copy(
            commonNames = vnNames,
            description = description,
            uses = uses,
            family = result.family?.takeIf { it.isNotBlank() } ?: defaultFamily(result.scientificName, display),
            plantGroup = group.key,
            morphology = result.morphology?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: defaultMorphology(display, group),
            origin = result.origin?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: defaultOrigin(display, group),
            care = completeCare(result.care, display, result.scientificName, group),
            commonDiseases = result.commonDiseases.ifEmpty { defaultDiseases() }.map { disease ->
                disease.copy(
                    imageUrl = disease.imageUrl?.takeIf { it.isNotBlank() && !it.startsWith("http") }
                        ?: DiseaseImages.urlFor(disease.name),
                )
            },
            medicalDisclaimer = result.medicalDisclaimer?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: "Chỉ tham khảo giáo dục / y học dân gian. Không tự điều trị bệnh nặng; hỏi bác sĩ hoặc dược sĩ.",
            detailAvailable = true,
        )
    }

    fun imageModel(url: String?): Any? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("http") -> GlideUrl(
                url,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", "PlantyIdentifyThesis/1.0")
                    .build(),
            )
            url.startsWith("content:") -> Uri.parse(url)
            url.startsWith("file:") -> {
                val path = Uri.parse(url).path
                if (!path.isNullOrBlank()) File(path) else Uri.parse(url)
            }
            else -> {
                val file = File(url)
                if (file.exists()) file else url
            }
        }
    }

    fun citationLabel(cite: CitationData): String {
        val kind = when (cite.type?.lowercase()) {
            "youtube" -> "Video YouTube"
            "duocdien" -> "Dược điển Việt Nam"
            "document" -> "Tài liệu"
            else -> "Nguồn"
        }
        val title = cite.title?.takeIf { it.isNotBlank() && !looksEnglish(it) }
            ?: cite.title?.takeIf { it.isNotBlank() }
            ?: "Xem nguồn"
        return "▶ $kind — $title"
    }

    fun shouldPlayToxicAlert(result: PlantScanResult): Boolean {
        val tox = result.toxicity
        if (tox?.isToxic == false && tox.playAlertSound != true) return false
        if (tox?.playAlertSound == true || tox?.isToxic == true) return true
        return looksLikeToxicMushroom(result)
    }

    private fun mergeLocal(scan: PlantScanResult, local: PlantScanData): PlantScanResult {
        val localResult = toResult(local)
        // Giữ danh tính từ server; catalog chỉ bổ sung field còn thiếu (care, toxicity…).
        return scan.copy(
            scientificName = scan.scientificName.ifBlank { localResult.scientificName },
            commonNames = scan.commonNames.ifEmpty { localResult.commonNames },
            plantId = scan.plantId ?: localResult.plantId,
            qrCode = scan.qrCode ?: localResult.qrCode,
            isMedicinal = scan.isMedicinal || localResult.isMedicinal,
            medicinalLabel = scan.medicinalLabel ?: localResult.medicinalLabel,
            family = scan.family ?: localResult.family,
            plantGroup = scan.plantGroup ?: localResult.plantGroup,
            morphology = scan.morphology ?: localResult.morphology,
            origin = scan.origin ?: localResult.origin,
            description = chooseDescription(scan, localResult),
            uses = usableUses(scan.uses).ifEmpty { localResult.uses },
            toxicity = scan.toxicity ?: localResult.toxicity,
            care = mergeCare(scan.care, localResult.care),
            commonDiseases = scan.commonDiseases.ifEmpty { localResult.commonDiseases },
            folkRemedies = scan.folkRemedies.ifEmpty { localResult.folkRemedies },
            medicalDisclaimer = scan.medicalDisclaimer ?: localResult.medicalDisclaimer,
            detailAvailable = true,
            imageUrl = scan.imageUrl?.takeIf { it.isNotBlank() } ?: localResult.imageUrl,
        )
    }

    /** Catalog wins when API misidentified the species (e.g. Grewia instead of Muntingia). */
    private fun overrideWithLocal(local: PlantScanData, scan: PlantScanResult): PlantScanResult {
        val localResult = toResult(local)
        return localResult.copy(
            confidence = maxOf(scan.confidence, localResult.confidence),
            imageUrl = scan.imageUrl?.takeIf { it.isNotBlank() } ?: localResult.imageUrl,
        )
    }

    private fun chooseDescription(scan: PlantScanResult, local: PlantScanResult): String? {
        val localDesc = local.description?.takeIf { it.isNotBlank() }
        val names = scan.commonNames + local.commonNames + listOf(scan.scientificName, local.scientificName)
        val scanDesc = usableDescription(scan.description)
            ?.takeIf { PlantDescriptionGuard.fits(it, names) }
        return scanDesc ?: localDesc ?: scan.description
    }

    private fun mergeCare(scan: CareData?, local: CareData?): CareData? {
        if (local == null) return scan
        if (scan == null) return local
        return CareData(
            planting = scan.planting ?: local.planting,
            care = scan.care ?: local.care,
            bestSeason = scan.bestSeason ?: local.bestSeason,
            water = scan.water ?: local.water,
            fertilizer = scan.fertilizer ?: local.fertilizer,
            light = scan.light ?: local.light,
            citations = scan.citations.orEmpty() + local.citations.orEmpty(),
            citationNote = scan.citationNote ?: local.citationNote,
        )
    }

    private fun applyToxicHeuristics(result: PlantScanResult): PlantScanResult {
        val existing = result.toxicity
        if (existing?.isToxic == false && existing.playAlertSound != true) return result
        if (!looksLikeToxicMushroom(result)) return result
        val tox = existing
        if (tox?.playAlertSound == true && !tox.warningMessage.isNullOrBlank()) return result
        return result.copy(
            toxicity = ToxicityData(
                isToxic = true,
                isFungus = true,
                toxicSap = tox?.toxicSap,
                warningLevel = "danger",
                warningMessage = tox?.warningMessage?.takeIf { it.isNotBlank() }
                    ?: "CẢNH BÁO NẤM ĐỘC: Không ăn, nếm, cho động vật ăn. Độc tố có thể gây suy gan, tử vong.",
                firstAid = tox?.firstAid?.takeIf { it.isNotBlank() }
                    ?: "Đã nuốt: gọi 115 ngay. Không tự gây nôn nếu chưa được hướng dẫn. Mang mẫu nấm tới y tế.",
                playAlertSound = true,
                alertSoundUrl = tox?.alertSoundUrl,
            ),
            detailAvailable = true,
        )
    }

    private fun looksLikeToxicMushroom(result: PlantScanResult): Boolean {
        val blob = fold(
            listOf(
                result.scientificName,
                result.commonNames.joinToString(" "),
                result.description.orEmpty(),
                result.toxicity?.warningMessage.orEmpty(),
            ).joinToString(" "),
        )
        return listOf(
            "nam doc",
            "nam tu than",
            "amanita phalloides",
            "amanita virosa",
            "amanita muscaria",
            "amanita pantherina",
            "toxic mushroom",
            "nam nghi doc",
            "death cap",
            "destroying angel",
        ).any { it in blob }
    }

    private fun normalizeQr(raw: String?): String {
        var value = raw.orEmpty().trim()
        if (value.startsWith("planty://plant/")) {
            value = value.substringAfter("planty://plant/")
        }
        if (value.startsWith("plant:")) {
            value = value.substringAfter("plant:")
        }
        return fold(value).replace(" ", "-")
    }

    private fun fold(value: String?): String {
        val nfd = Normalizer.normalize(value.orEmpty().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .trim()
    }

    private fun vietnameseNames(common: List<String>, scientific: String): List<String> {
        val fromCommon = common.mapNotNull { vietnameseNameFallback(it) }
        if (fromCommon.isNotEmpty()) return fromCommon.distinct()

        val fromScientific = vietnameseNameFallback(scientific)
            ?: vietnameseNameByScientific(scientific)
        if (fromScientific != null) return listOf(fromScientific)

        val filtered = common.map { it.trim() }.filter { it.isNotBlank() && !looksEnglish(it) }
        if (filtered.isNotEmpty()) return filtered
        return listOf(VietnameseNameLookup.UNKNOWN_PLACEHOLDER)
    }

    private fun vietnameseNameByScientific(scientific: String?): String? {
        val key = fold(scientific)
        if (key.isBlank()) return null
        return when {
            key.contains("polyscias") -> "Đinh lăng"
            key.contains("mimosa pudica") -> "Xấu hổ"
            key.contains("epipremnum") || key.contains("pothos") -> "Trầu bà"
            key.contains("aloe") -> "Nha đam"
            key.contains("zingiber") -> "Gừng"
            key.contains("curcuma") -> "Nghệ"
            key.contains("cymbopogon") -> "Sả"
            key.contains("mentha") -> "Bạc hà"
            key.contains("perilla") -> "Tía tô"
            key.contains("piper lolot") || key.contains("piper sarmentosum") -> "Lá lốt"
            key.contains("nephrolepis") -> "Dương sỉ"
            key.contains("cordyline") -> "Mai vạn phúc"
            key.contains("sansevieria") || key.contains("dracaena trifasciata") -> "Cây lưỡi hổ"
            key.contains("callisia") -> "Lược vàng"
            key.contains("peperomia pellucida") -> "Tần dày lá"
            key.contains("pseuderanthemum") -> "Sao nhái"
            key.contains("polianthes") || key.contains("tuberosa") -> "Hoa huệ"
            key.contains("dieffenbachia") -> "Ngọc ngân"
            key.contains("dracaena fragrans") -> "Thiết mộc lan"
            key.contains("aglaonema") -> "Đại phú gia"
            key.contains("dracaena") -> "Phát tài / huyết dụ"
            key.contains("sphagneticola trilobata") ||
                key.contains("wedelia trilobata") ||
                key.contains("sphagneticola") ||
                key.contains("wedelia") -> "Sài đất cảnh"
            key.contains("bougainvillea") -> "Hoa giấy"
            key.contains("hibiscus rosa-sinensis") || (key.contains("hibiscus") && !key.contains("ixora")) -> "Dâm bụt"
            key.contains("ixora") -> "Bông trang"
            key.contains("rosa ") || key.startsWith("rosa") -> "Hoa hồng"
            key.contains("chrysanthemum") -> "Hoa cúc"
            key.contains("phalaenopsis") -> "Lan hồ điệp"
            key.contains("helianthus") -> "Hoa hướng dương"
            key.contains("jasminum") -> "Hoa nhài"
            key.contains("nelumbo") -> "Hoa sen"
            key.contains("spathiphyllum") -> "Cây lan ý"
            key.contains("chlorophytum") -> "Cây dây nhện"
            key.contains("zamioculcas") -> "Cây kim tiền"
            key.contains("ficus elastica") -> "Cây đa búp đỏ"
            key.contains("ficus lyrata") -> "Cây bàng Singapore"
            key.contains("crassula") -> "Cây ngọc bích"
            key.contains("monstera") -> "Cây trầu bà lá xẻ"
            key.contains("centella") -> "Rau má"
            key.contains("houttuynia") -> "Diếp cá"
            key.contains("eclipta") || key.contains("herba ecliptae") -> "Cỏ nhọ nồi"
            key.contains("scoparia dulcis") || key.contains("scoparia") -> "Cam thảo đất"
            key.contains("paederia") -> "Lá mơ"
            key.contains("terminalia") -> "Cây bàng"
            key.contains("delonix") -> "Phượng vĩ"
            key.contains("oryza") -> "Lúa"
            key.contains("ipomoea batatas") -> "Khoai lang"
            key.contains("zea mays") || key.contains("zea ") -> "Ngô"
            key.contains("solanum lycopersicum") -> "Cà chua"
            key.contains("capsicum annuum") -> "Ớt"
            key.contains("vigna radiata") -> "Đậu xanh"
            key.contains("glycine max") -> "Đậu nành"
            key.contains("arachis hypogaea") -> "Lạc"
            key.contains("vigna mungo") -> "Đậu đen"
            key.contains("solanum tuberosum") -> "Khoai tây"
            key.contains("manihot esculenta") -> "Sắn"
            key.contains("colocasia esculenta") -> "Khoai môn"
            key.contains("cucumis sativus") -> "Dưa chuột"
            key.contains("cucurbita") -> "Bí đỏ"
            key.contains("benincasa hispida") -> "Bí xanh"
            key.contains("lagenaria siceraria") -> "Bầu"
            key.contains("luffa") -> "Mướp hương"
            key.contains("ipomoea aquatica") -> "Rau muống"
            key.contains("brassica rapa") -> "Cải xanh"
            key.contains("brassica oleracea") -> "Cải bắp"
            key.contains("lactuca sativa") -> "Xà lách"
            key.contains("allium fistulosum") -> "Hành lá"
            key.contains("allium sativum") -> "Tỏi"
            key.contains("citrullus lanatus") -> "Dưa hấu"
            key.contains("carica papaya") -> "Đu đủ"
            key.contains("musa paradisiaca") -> "Chuối"
            key.contains("mangifera indica") -> "Xoài"
            key.contains("citrus sinensis") -> "Cam"
            key.contains("citrus") && key.contains("limon") -> "Chanh"
            key.contains("citrus maxima") -> "Bưởi"
            key.contains("artocarpus heterophyllus") -> "Mít"
            key.contains("nephelium") -> "Chôm chôm"
            key.contains("durio") -> "Sầu riêng"
            key.contains("syzygium samarangense") -> "Mận"
            key.contains("phaseolus vulgaris") -> "Đậu que"
            key.contains("sauropus androgynus") -> "Rau ngót"
            key.contains("amaranthus") -> "Rau dền"
            key.contains("plantago") -> "Mã đề"
            key.contains("ocimum") -> "Hương nhu"
            key.contains("plectranthus") || key.contains("coleus amboinicus") -> "Húng chanh"
            key.contains("artemisia argyi") || key.contains("cuc tan") || key.contains("ai diep") -> "Cúc tần"
            key.contains("artemisia") -> "Ngải cứu"
            key.contains("portulaca") -> "Rau sam"
            key.contains("muntingia") -> "Trứng cá"
            key.contains("terminalia catappa") || (key.contains("terminalia") && key.contains("catappa")) -> "Cây bàng"
            key.contains("delonix") -> "Phượng vĩ"
            key.contains("bougainvillea") -> "Hoa giấy"
            key.contains("lagenaria") -> "Bầu"
            key.contains("benincasa") -> "Bí xanh"
            key.contains("gnetum") -> "Lá giang"
            key.contains("andrographis") || key.contains("mat gau") -> "Cây mật gấu"
            key.contains("taraxacum") || key.contains("bo cong anh") -> "Bồ công anh"
            key.contains("lonicera japonica") || (key.contains("lonicera") && key.contains("japonica")) -> "Kim ngân hoa"
            key.contains("hedyotis") || key.contains("hoan ngoc") || key.contains("oldenlandia diffusa") -> "Hoàn ngọc"
            key.contains("orthosiphon") || key.contains("xuong khop") -> "Trà xương khớp"
            key.contains("curculigo") || key.contains("sam cau") -> "Sâm cau"
            key.contains("artemisia argyi") || key.contains("cuc tan") -> "Cúc tần"
            key.contains("allium tuberosum") || (key.contains("allium") && key.contains("tuberosum")) -> "Hẹ"
            key.contains("salvia miltiorrhiza") || key.contains("dan sam") -> "Đan sâm"
            key.contains("actaea cimicifuga") || key.contains("cimicifuga") || key.contains("trinh nu hoang cung") -> "Trinh nữ hoàng cung"
            key.contains("spondias") -> "Cóc"
            key.contains("momordica") -> "Mướp đắng"
            key.contains("piper nigrum") -> "Hồ tiêu"
            key.contains("cinnamomum") -> "Quế"
            key.contains("glycyrrhiza") -> "Cam thảo"
            key.contains("stevia") -> "Cỏ ngọt"
            key.contains("dimocarpus") || key.contains("longan") -> "Long nhãn"
            key.contains("averrhoa carambola") || key.contains("carambola") -> "Khế"
            key.contains("averrhoa bilimbi") -> "Khế tàu"
            key.contains("volvariella") -> "Nấm rơm"
            key.contains("agaricus bisporus") -> "Nấm mỡ"
            key.contains("lentinula") -> "Nấm hương"
            key.contains("pleurotus ostreatus") -> "Nấm sò"
            key.contains("pleurotus eryngii") -> "Nấm đùi gà"
            key.contains("auricularia") -> "Mộc nhĩ"
            key.contains("flammulina") -> "Nấm kim châm"
            key.contains("termitomyces") -> "Nấm mối"
            key.contains("amanita phalloides") -> "Nấm tử thần"
            key.contains("amanita muscaria") -> "Nấm mũ ruồi"
            key.contains("chlorophyllum molybdites") -> "Nấm dù xanh"
            else -> null
        }
    }

    private fun vietnameseNameFallback(name: String?): String? {
        val key = fold(name)
        if (key.isBlank()) return null
        return when {
            key == "snake plant" || key.contains("mother in law") || key.contains("sansevieria") -> "Cây lưỡi hổ"
            key == "aloe vera" || key == "aloe" -> "Nha đam"
            key.contains("paper flower") || key.contains("bougainvillea") -> "Hoa giấy"
            key == "rose" || key.contains("china rose") || key.contains("chinese rose") -> "Hoa hồng"
            key.contains("dracaena fragrans") ||
                key.contains("corn plant") ||
                key.contains("mass cane") ||
                key.contains("fragrant dracaena") -> "Thiết mộc lan"
            key.contains("dai phu gia") ||
                key.contains("aglaonema") ||
                key.contains("dieffenbachia spp") ||
                key == "dieffenbachia sp" ||
                key == "dumb cane" -> "Đại phú gia"
            key.contains("sphagneticola") ||
                key.contains("wedelia") ||
                key.contains("singapore daisy") ||
                key.contains("creeping oxeye") -> "Sài đất cảnh"
            key.contains("chrysanthemum") || (key.contains("florist") && key.contains("daisy")) -> "Hoa cúc"
            key.contains("moth orchid") || key.contains("moon orchid") || key.contains("phalaenopsis") -> "Lan hồ điệp"
            key.contains("sunflower") -> "Hoa hướng dương"
            key.contains("arabian jasmine") || key.contains("sambac jasmine") || key == "jasmine" -> "Hoa nhài"
            key.contains("sacred lotus") || key == "lotus" -> "Hoa sen"
            key.contains("golden pothos") || key == "money plant" || key == "pothos" -> "Cây trầu bà"
            key.contains("false daisy") ||
                key.contains("eclipta") ||
                key.contains("yerba de tago") ||
                key.contains("yerba de tajo") ||
                key.contains("bhringraj") ||
                key.contains("bhringaraj") ||
                key.contains("karisalankanni") ||
                key.contains("kehraj") -> "Cỏ nhọ nồi"
            key.contains("sweet broomweed") || key.contains("licorice weed") || key.contains("scoparia dulcis") -> "Cam thảo đất"
            key.contains("peace lily") -> "Cây lan ý"
            key.contains("spider plant") -> "Cây dây nhện"
            key.contains("zz plant") -> "Cây kim tiền"
            key.contains("rubber plant") -> "Cây đa búp đỏ"
            key.contains("fiddle leaf fig") -> "Cây bàng Singapore"
            key.contains("jade plant") -> "Cây ngọc bích"
            key.contains("swiss cheese plant") || key.contains("monstera") -> "Cây trầu bà lá xẻ"
            key.contains("boston fern") || key.contains("sword fern") -> "Dương sỉ"
            key.contains("mint") -> "Bạc hà"
            key.contains("basil") -> "Húng / hương nhu"
            key.contains("turmeric") -> "Nghệ"
            key.contains("ginger") && !key.contains("wild") -> "Gừng"
            key.contains("lemongrass") -> "Sả"
            key.contains("orchid") -> "Lan"
            key.contains("fern") -> "Dương sỉ"
            key.contains("cactus") -> "Xương rồng"
            key.contains("bamboo") -> "Tre / trúc"
            key.contains("palm") -> "Cây cọ"
            key.contains("starfruit") || key.contains("star fruit") || key.contains("carambola") -> "Khế"
            key.contains("paederia") || key.contains("skunkvine") || key.contains("stinkvine") ||
                key.contains("la mo") || key.contains("mo long") -> "Lá mơ"
                key.contains("green amaranth") ||
                key.contains("slender amaranth") ||
                key.contains("amaranth") -> "Rau dền"
            key.contains("purslane") || key.contains("portulaca") -> "Rau sam"
            key.contains("jamaica cherry") ||
                key.contains("strawberry tree") ||
                key.contains("muntingia") ||
                key.contains("trung ca") ||
                key.contains("calabura") ||
                key.contains("panama berry") ||
                key.contains("phalsa") ||
                key.contains("grewia asiatica") -> "Trứng cá"
            key.contains("ambarella") ||
                key.contains("golden apple") ||
                key.contains("spondias") ||
                key.contains("cay coc") -> "Cóc"
            else -> vietnameseNameByScientific(name)
        }
    }

    private fun usableUses(uses: List<String>): List<String> =
        uses.map { it.trim() }.filter {
            it.isNotBlank() && !looksEnglish(it) && !PlantFactsLookup.looksGeneric(it)
        }

    private fun usableDescription(text: String?): String? =
        text?.trim()?.takeIf {
            it.isNotBlank() && !looksEnglish(it) && !PlantFactsLookup.looksGeneric(it)
        }

    private fun looksEnglish(text: String): Boolean {
        val value = text.trim()
        if (value.isEmpty()) return false
        if (value.any { ch ->
                ch in "ăâêôơưđĂÂÊÔƠƯĐáàảãạéèẻẽẹíìỉĩịóòỏõọúùủũụýỳỷỹỵ" ||
                    ch in "ÁÀẢÃẠÉÈẺẼẸÍÌỈĨỊÓÒỎÕỌÚÙỦŨỤÝỲỶỸỴ"
            }
        ) {
            return false
        }
        val letters = value.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        val asciiLetters = letters.count { it.code < 128 }
        if (asciiLetters.toFloat() / letters.length < 0.9f) return false
        if (Regex("^[A-Z][a-z]+(\\s+[a-z]+)+$").matches(value)) return true
        val words = Regex("[A-Za-z']+").findAll(value.lowercase()).map { it.value }.toSet()
        val markers = setOf(
            "the", "and", "with", "from", "this", "that", "plant", "leaf", "used",
            "for", "care", "video", "toxic", "mushroom", "suspect", "aralia", "pothos",
            "flower", "tree", "herb", "rose", "orchid", "fern", "lily", "palm",
        )
        return words.intersect(markers).isNotEmpty() || (words.size <= 4 && asciiLetters == letters.length)
    }

    private fun defaultDescription(name: String, medicinal: Boolean): String {
        return if (medicinal) {
            "$name là cây / dược liệu thường gặp. Thông tin dưới đây lấy từ Dược điển và tài liệu tham khảo giáo dục, không thay thế chỉ định y tế."
        } else {
            "$name được nhận diện. Dưới đây là hướng dẫn trồng, chăm sóc và nguồn video / tài liệu tham khảo bằng tiếng Việt."
        }
    }

    private fun defaultUses(name: String, medicinal: Boolean): List<String> {
        return if (medicinal) {
            listOf(
                "Dùng làm dược liệu / gia vị theo y học cổ truyền — đối chiếu Dược điển.",
                "Có thể trồng chậu hoặc vườn nhà để lấy lá, hoa tùy loài.",
                "Không tự ý sắc uống liều cao khi chưa hỏi thầy thuốc.",
            )
        } else {
            listOf(
                "Trồng làm cây cảnh, bóng mát hoặc hàng rào tùy loài.",
                "Xem hướng dẫn chăm sóc, tưới và ánh sáng bên dưới.",
            )
        }
    }

    private fun completeCare(
        care: CareData?,
        name: String,
        scientificName: String,
        group: PlantGroup,
    ): CareData {
        val isFarm = group == PlantGroup.AGRICULTURE ||
            group == PlantGroup.VEGETABLE ||
            group == PlantGroup.FRUIT
        val query = CareVideoLookup.searchUrl(name, scientificName)
        val existing = care?.citations.orEmpty().filter {
            !it.url.isNullOrBlank() || !it.videoId.isNullOrBlank()
        }
        val youtube = CareVideoLookup.bestExisting(existing) ?: CitationData(
            type = "youtube",
            title = if (isFarm) {
                "Video: bón phân hợp lý cho $name"
            } else {
                "Video: cách trồng và chăm sóc $name"
            },
            url = query,
            videoId = null,
        )
        val nonYoutube = existing.filterNot { CareVideoLookup.isYoutube(it) }
        val citations = if (isFarm) {
            listOf(youtube) + nonYoutube.filter { it.type.equals("duocdien", ignoreCase = true) }.take(1)
        } else {
            listOf(youtube) + nonYoutube.ifEmpty {
                listOf(
                    CitationData(
                        type = "duocdien",
                        title = "Dược điển Việt Nam — danh mục dược liệu",
                        url = "https://duocdienvietnam.com/duoc-lieu/",
                        videoId = null,
                    ),
                )
            }
        }
        return CareData(
            planting = care?.planting?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: "Trồng $name trên đất tơi xốp, thoát nước, nơi có ánh sáng vừa. Có thể trồng chậu hoặc luống vườn.",
            care = care?.care?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: "Tưới ẩm vừa, tránh úng. Làm cỏ sớm, che phủ gốc nếu cần để giảm cạnh tranh dinh dưỡng. Cắt lá già, theo dõi sâu bệnh.",
            bestSeason = care?.bestSeason?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: "Xuân – đầu hè, thời tiết ấm, ít ngập úng.",
            water = care?.water?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: "Tưới khi mặt đất se, giữ ẩm vừa, không để úng rễ. Mùa nắng tưới sáng sớm hoặc chiều mát.",
            fertilizer = care?.fertilizer?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: defaultFertilizer(name, group),
            light = care?.light?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: if (group == PlantGroup.EDUCATION) {
                    "Ánh sáng tán xạ đến nắng buổi sáng. Quan sát hướng nắng khi học cấu tạo lá."
                } else {
                    "Nắng vừa đến nắng sáng. Tránh nắng gắt giữa trưa với cây non."
                },
            citations = citations,
            citationNote = care?.citationNote?.takeIf { it.isNotBlank() && !looksEnglish(it) }
                ?: if (isFarm) {
                    "Tóm tắt thêm từ tài liệu nhân giống, cỏ dại và bệnh cây: ưu tiên phân hữu cơ hoai, NPK cân đối, làm cỏ/che phủ và điều chỉnh liều theo đất, tuổi cây."
                } else {
                    "Nguồn video và tài liệu tham khảo — đối chiếu Dược điển trước khi dùng làm thuốc."
                },
        )
    }

    private fun defaultFertilizer(name: String, group: PlantGroup): String {
        val plantName = name.ifBlank { "cây" }
        return when (group) {
            PlantGroup.FRUIT ->
                "Bón lót phân hữu cơ hoai mục khi trồng. Khi $plantName còn nhỏ, dùng NPK cân đối liều nhẹ, chia 4–6 tuần/lần; giai đoạn ra hoa/nuôi quả giảm đạm, tăng lân-kali theo khuyến cáo địa phương. Bón theo mép tán, lấp đất mỏng rồi tưới ẩm; không bón sát gốc hoặc khi đất đang khô nóng."
            PlantGroup.AGRICULTURE, PlantGroup.VEGETABLE ->
                "Bón lót hữu cơ hoai mục và lân trước trồng; bón thúc NPK theo giai đoạn cây con, phát triển thân lá và trước ra hoa/nuôi củ/quả. Làm sạch cỏ trước khi bón để phân không bị cỏ cạnh tranh; tránh bón dư đạm, đặc biệt gần thu hoạch."
            PlantGroup.MEDICINE ->
                "Dùng phân hữu cơ hoai mục hoặc phân vi sinh là chính; có thể tưới NPK loãng 20–30 ngày/lần khi cây sinh trưởng mạnh. Sau bón cần tưới ẩm, ngừng bón trước khi thu hái lá vài ngày và không dùng phân tươi."
            else ->
                "Bón phân hữu cơ / NPK loãng 3–4 tuần/lần. Bón xa cổ rễ, tưới ẩm sau bón; ngừng bón nếu lá vàng do úng hoặc rễ yếu."
        }
    }

    private fun defaultDiseases(): List<CommonDiseaseData> = listOf(
        CommonDiseaseData(
            name = "Rệp",
            description = "Rệp bám mặt dưới lá, hút nhựa, lá vàng và cây chậm lớn.",
            ecoTreatment = "Rửa nước, dầu neem loãng, bắt bằng tay.",
            imageUrl = DiseaseImages.urlFor("Rệp"),
        ),
        CommonDiseaseData(
            name = "Sâu ăn lá",
            description = "Sâu cắn khuyết lá, phân sâu trên mặt lá.",
            ecoTreatment = "Bắt sâu bằng tay, dầu neem loãng, kiểm tra mặt dưới lá buổi tối.",
            imageUrl = DiseaseImages.urlFor("Sâu ăn lá"),
        ),
        CommonDiseaseData(
            name = "Đốm lá do ẩm",
            description = "Đốm nâu khi trời ẩm kéo dài.",
            ecoTreatment = "Cắt lá bệnh, thông gió, baking soda loãng hoặc chế phẩm sinh học.",
            imageUrl = DiseaseImages.urlFor("Đốm lá"),
        ),
    )

    private fun defaultFamily(scientific: String, display: String): String {
        val key = fold(scientific)
        return when {
            key.contains("rosa") -> "Họ Hoa hồng (Rosaceae)"
            key.contains("chrysanthemum") -> "Họ Cúc (Asteraceae)"
            key.contains("phalaenopsis") -> "Họ Lan (Orchidaceae)"
            key.contains("terminalia") -> "Họ Bàng (Combretaceae)"
            key.contains("delonix") -> "Họ Đậu (Fabaceae)"
            key.contains("portulaca") -> "Họ Rau sam (Portulacaceae)"
            key.contains("muntingia") -> "Họ Trứng cá (Muntingiaceae)"
            key.contains("spondias") -> "Họ Đào lộn hột (Anacardiaceae)"
            key.contains("averrhoa") -> "Họ Chua me đất (Oxalidaceae)"
            key.contains("paederia") -> "Họ Cà phê (Rubiaceae)"
            key.contains("ipomoea") -> "Họ Bìm bìm (Convolvulaceae)"
            key.contains("zea") -> "Họ Lúa (Poaceae)"
            key.contains("houttuynia") -> "Họ Diếp cá (Saururaceae)"
            key.contains("centella") -> "Họ Hoa tán (Apiaceae)"
            key.contains("polyscias") -> "Họ Nhân sâm (Araliaceae)"
            key.contains("amanita") -> "Họ Amanitaceae"
            else -> "Họ thực vật — $display"
        }
    }

    private fun defaultMorphology(name: String, group: PlantGroup): String {
        return if (group == PlantGroup.EDUCATION) {
            "$name gồm rễ (hút nước, khoáng), thân (nâng đỡ, dẫn nhựa) và lá (quang hợp). Quan sát kiểu rễ cọc/chùm, thân gỗ/thảo, gân lá và mép lá khi học Khoa học."
        } else {
            "Hình thái nổi bật của $name: quan sát lá, thân, hoa/quả để đối chiếu khi nhận diện."
        }
    }

    private fun defaultOrigin(name: String, group: PlantGroup): String {
        return when (group) {
            PlantGroup.MEDICINE -> "$name phân bố ở nhiều tỉnh Việt Nam, ưa đất ẩm, vườn nhà, bờ ruộng hoặc dưới tán cây."
            PlantGroup.AGRICULTURE, PlantGroup.VEGETABLE, PlantGroup.FRUIT ->
                "$name được trồng phổ biến ở đồng bằng và vùng chuyên canh Việt Nam."
            PlantGroup.EDUCATION -> "$name thường gặp trong sân trường, công viên, vườn nhà — dùng để học cấu tạo cây."
            PlantGroup.TOXIC, PlantGroup.MUSHROOM ->
                "Cần thận trọng khi gặp $name ngoài thực tế — không chạm, không ăn."
            else -> "Nguồn gốc và phân bố của $name: phổ biến ở vùng nhiệt đới, dễ gặp tại Việt Nam."
        }
    }
}
