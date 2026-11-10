package com.expeditee.plant.identifier.services.notification

import android.content.Context
import com.expeditee.plant.identifier.R

data class NotificationMessage(
    val description: String,
    val cta: String
)

object HomeNotificationMessages {

    fun getMessage(context: Context, messageIndex: Int): NotificationMessage {
        val descriptionResId = when (messageIndex) {
            1 -> R.string.noti_home_msg1_description
            2 -> R.string.noti_home_msg2_description
            3 -> R.string.noti_home_msg3_description
            4 -> R.string.noti_home_msg4_description
            else -> R.string.noti_home_msg1_description
        }
        
        val ctaResId = when (messageIndex) {
            1 -> R.string.noti_home_msg1_cta
            2 -> R.string.noti_home_msg2_cta
            3 -> R.string.noti_home_msg3_cta
            4 -> R.string.noti_home_msg4_cta
            else -> R.string.noti_home_msg1_cta
        }
        
        return NotificationMessage(
            description = context.getString(descriptionResId),
            cta = context.getString(ctaResId)
        )
    }

    fun getAllMessageIndices(): List<Int> {
        return listOf(1, 2, 3, 4)
    }
}
