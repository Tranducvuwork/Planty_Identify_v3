package com.expeditee.plant.identifier.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "popular_plants",
    indices = [
        Index(value = ["scientificName"]),
        Index(value = ["commonNames", "scientificName"])
    ]
)
@Keep
data class PopularPlantEntity(
    @PrimaryKey val id: Int,
    val commonNames: List<String>,
    val scientificName: String?,
    val mainImageUrl: String?
)

@Fts4(contentEntity = PopularPlantEntity::class)
@Entity(tableName = "popular_plants_fts")
@Keep
data class PopularPlantFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid")
    val rowid: Int,
    val commonNames: String,
    val scientificName: String?
)