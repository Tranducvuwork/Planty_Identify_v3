package com.expeditee.plant.identifier.repository

import android.content.Context
import com.expeditee.plant.identifier.data.repository.PlantChatRepository
import com.expeditee.plant.identifier.helper.AppConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PlantAssistantChatRepository(
    private val context: Context,
    private val chatRepository: PlantChatRepository,
    private val appConfigSettings: AppConfigSettings,
) {

    fun sendMessage(userMessage: String): Flow<Result<String>> = flow {
        val language = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() } ?: "en"
        val result = chatRepository.sendChatMessage(userMessage, language)
        emit(result)
    }.flowOn(Dispatchers.IO)
}
