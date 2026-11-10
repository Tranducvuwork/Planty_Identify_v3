package com.expeditee.plant.identifier.model

import com.expeditee.plant.identifier.helper.SiteManager
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Keep
data class MyPlantModel(
    val id: Int,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val scientificName: String = "",
    val type: String,
    val status: String = MyPlantStatus.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val imageFile: String? = null,
    val metadata: String? = null,
    val gardenId: String = SiteManager.DEFAULT_SITE_ID,
    val isActive: Boolean = true
): Parcelable