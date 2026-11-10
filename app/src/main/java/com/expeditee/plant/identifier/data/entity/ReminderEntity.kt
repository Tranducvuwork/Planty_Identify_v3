package com.expeditee.plant.identifier.data.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int = 0,
    val plantName: String = "",
    val remindType: String = "",
    val remindTime: String = "",
    val repeatType: String = "",
    val previousDate: Long = 0,
    val nextDate: Long = 0,
    val lastDate: String? = null,
    val isEnabled: Boolean = true,
    val imagePath: String? = null
)
