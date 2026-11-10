package com.expeditee.plant.identifier.data.entity

import androidx.annotation.Keep
import com.expeditee.plant.identifier.helper.SiteManager
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "my_plants",
)
@Keep
data class MyPlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val scientificName: String = "",
    val type: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageFile: String? = null,
    val metadata: String? = null,
    val gardenId: String = SiteManager.DEFAULT_SITE_ID,
    val isActive: Boolean = true
)