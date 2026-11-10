package com.expeditee.plant.identifier.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class ReminderScheduleModel(
    val id: Int = 0,
    var plantId: Int = 0,
    var plantName: String = "",
    val remindType: String = "",
    var remindTime: String = "",
    val repeatType: String = "",
    val previousDate: Long = 0,
    val nextDate: Long = 0,
    val lastDate: String? = null,
    val isEnabled: Boolean = true,
    val imagePath: String? = null,
    @DrawableRes val imageRes: Int = 0
) : Serializable
