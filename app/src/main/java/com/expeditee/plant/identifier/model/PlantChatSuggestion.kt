package com.expeditee.plant.identifier.model

import android.content.Context
import androidx.annotation.Keep
import com.expeditee.plant.identifier.R

@Keep
data class PlantChatSuggestion(
    val id: Int,
    val title: String,
    val messageToSend: String
) {
    companion object {
        fun randomStarterSuggestions(context: Context): List<PlantChatSuggestion> {
            val allSuggestions = listOf(
                PlantChatSuggestion(1, context.getString(R.string.chat_suggestion_yellow_leaves), context.getString(R.string.chat_suggestion_yellow_leaves)),
                PlantChatSuggestion(2, context.getString(R.string.chat_suggestion_watering_frequency), context.getString(R.string.chat_suggestion_watering_frequency)),
                PlantChatSuggestion(3, context.getString(R.string.chat_suggestion_plant_problem), context.getString(R.string.chat_suggestion_plant_problem)),
                PlantChatSuggestion(4, context.getString(R.string.chat_suggestion_plant_growth), context.getString(R.string.chat_suggestion_plant_growth)),
                PlantChatSuggestion(5, context.getString(R.string.chat_suggestion_soil_type), context.getString(R.string.chat_suggestion_soil_type)),
                PlantChatSuggestion(6, context.getString(R.string.chat_suggestion_sunlight_needs), context.getString(R.string.chat_suggestion_sunlight_needs)),
                PlantChatSuggestion(7, context.getString(R.string.chat_suggestion_drooping_leaves), context.getString(R.string.chat_suggestion_drooping_leaves)),
                PlantChatSuggestion(8, context.getString(R.string.chat_suggestion_fertilizing), context.getString(R.string.chat_suggestion_fertilizing))
            )
            return allSuggestions.shuffled().take(4)
        }
    }
}
