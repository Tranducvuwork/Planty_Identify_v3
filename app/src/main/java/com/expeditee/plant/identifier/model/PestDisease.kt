package com.expeditee.plant.identifier.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class PestDisease(
    val id: Int,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val otherName: List<String>,
    val description: List<Section>,
    val solution: List<Section>,
    val host: List<String>,
    val images: List<Image>
) : Serializable {
    data class Section(
        val subtitle: String,
        val description: String
    ) : Serializable

    data class Image(
        val originalUrl: String,
        val regularUrl: String,
        val mediumUrl: String,
        val smallUrl: String,
        val thumbnail: String
    ) : Serializable

    fun displayName(): String {
        return commonName.ifBlank { scientificName }
    }

    fun firstSummary(): String {
        return description.firstOrNull()?.description.orEmpty()
    }

    fun firstImage(): String {
        return images.firstOrNull()?.regularUrl
            ?: images.firstOrNull()?.smallUrl
            ?: images.firstOrNull()?.thumbnail
            ?: ""
    }
}
