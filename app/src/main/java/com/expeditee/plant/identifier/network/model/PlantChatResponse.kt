package com.expeditee.plant.identifier.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PlantChatData(
    @SerializedName("answer") val answer: String?,
)

@Keep
data class PlantChatResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("language") val language: String? = null,
    @SerializedName("details") val details: String? = null,
    @SerializedName("data") val data: PlantChatData?,
)
