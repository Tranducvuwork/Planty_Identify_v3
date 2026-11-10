package com.expeditee.plant.identifier.network.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
data class PlantScanData(
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("commonNames") val commonNames: List<String>?,
    @SerializedName("confidence") val confidence: Int?,
    @SerializedName("description") val description: String?,
    @SerializedName("plantId") val plantId: String? = null,
    @SerializedName("qrCode") val qrCode: String? = null,
    @SerializedName("isMedicinal") val isMedicinal: Boolean? = null,
    @SerializedName("medicinalLabel") val medicinalLabel: String? = null,
    @SerializedName("family") val family: String? = null,
    @SerializedName("plantGroup") val plantGroup: String? = null,
    @SerializedName("morphology") val morphology: String? = null,
    @SerializedName("origin") val origin: String? = null,
    @SerializedName("uses") val uses: List<String>? = null,
    @SerializedName("toxicity") val toxicity: ToxicityData? = null,
    @SerializedName("care") val care: CareData? = null,
    @SerializedName("commonDiseases") val commonDiseases: List<CommonDiseaseData>? = null,
    @SerializedName("folkRemedies") val folkRemedies: List<FolkRemedyData>? = null,
    @SerializedName("medicalDisclaimer") val medicalDisclaimer: String? = null,
    @SerializedName("detailAvailable") val detailAvailable: Boolean? = null,
    @SerializedName("detailHint") val detailHint: String? = null,
    @SerializedName(value = "imageUrl", alternate = ["image_url"]) val imageUrl: String? = null,
)

@Keep
@Parcelize
data class ToxicityData(
    @SerializedName("is_toxic") val isToxic: Boolean? = null,
    @SerializedName("is_fungus") val isFungus: Boolean? = null,
    @SerializedName("toxic_sap") val toxicSap: Boolean? = null,
    @SerializedName("warning_level") val warningLevel: String? = null,
    @SerializedName("warning_message") val warningMessage: String? = null,
    @SerializedName("first_aid") val firstAid: String? = null,
    @SerializedName("play_alert_sound") val playAlertSound: Boolean? = null,
    @SerializedName("alert_sound_url") val alertSoundUrl: String? = null,
) : Parcelable

@Keep
@Parcelize
data class CareData(
    @SerializedName("planting") val planting: String? = null,
    @SerializedName("care") val care: String? = null,
    @SerializedName("best_season") val bestSeason: String? = null,
    @SerializedName("water") val water: String? = null,
    @SerializedName("fertilizer") val fertilizer: String? = null,
    @SerializedName("light") val light: String? = null,
    @SerializedName("citations") val citations: List<CitationData>? = null,
    @SerializedName("citation_note") val citationNote: String? = null,
) : Parcelable

@Keep
@Parcelize
data class CitationData(
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("video_id") val videoId: String? = null,
) : Parcelable

@Keep
@Parcelize
data class CommonDiseaseData(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("eco_treatment") val ecoTreatment: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
) : Parcelable

@Keep
@Parcelize
data class FolkRemedyData(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("ailment") val ailment: String? = null,
    @SerializedName("ingredients") val ingredients: List<String>? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("plants") val plants: List<String>? = null,
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("source_title") val sourceTitle: String? = null,
    @SerializedName("caution") val caution: String? = null,
) : Parcelable

@Keep
data class PlantScanApiResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: PlantScanData?,
)

@Keep
data class PlantListItemData(
    @SerializedName("plantId") val plantId: String?,
    @SerializedName("qrCode") val qrCode: String?,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("commonNames") val commonNames: List<String>?,
    @SerializedName("isMedicinal") val isMedicinal: Boolean?,
    @SerializedName("medicinalLabel") val medicinalLabel: String?,
    @SerializedName("warningLevel") val warningLevel: String?,
    @SerializedName(value = "imageUrl", alternate = ["image_url"]) val imageUrl: String? = null,
)

@Keep
data class PlantListApiResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: List<PlantListItemData>?,
)

@Keep
data class FolkRemediesApiResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("disclaimer") val disclaimer: String?,
    @SerializedName("data") val data: List<FolkRemedyData>?,
)

@Keep
@Parcelize
data class PlantScanResult(
    val scientificName: String,
    val commonNames: List<String> = emptyList(),
    val confidence: Double,
    val description: String? = null,
    val plantId: String? = null,
    val qrCode: String? = null,
    val isMedicinal: Boolean = false,
    val medicinalLabel: String? = null,
    val family: String? = null,
    val plantGroup: String? = null,
    val morphology: String? = null,
    val origin: String? = null,
    val uses: List<String> = emptyList(),
    val toxicity: ToxicityData? = null,
    val care: CareData? = null,
    val commonDiseases: List<CommonDiseaseData> = emptyList(),
    val folkRemedies: List<FolkRemedyData> = emptyList(),
    val medicalDisclaimer: String? = null,
    val detailAvailable: Boolean = false,
    val imageUrl: String? = null,
) : Parcelable {
    val hasThesisDetail: Boolean
        get() = detailAvailable ||
            isMedicinal ||
            toxicity?.playAlertSound == true ||
            uses.isNotEmpty() ||
            care != null ||
            commonDiseases.isNotEmpty()
}
