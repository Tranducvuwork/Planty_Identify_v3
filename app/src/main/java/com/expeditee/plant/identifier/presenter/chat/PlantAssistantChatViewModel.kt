package com.expeditee.plant.identifier.presenter.chat

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.ResultFlow
import com.expeditee.plant.identifier.repository.PlantAssistantChatRepository
import com.expeditee.plant.identifier.thesis.ChatPestIllustration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Keep
data class QueuedUserMessage(
    val displayMessage: String,
    val imageUri: String?,
)

sealed class PlantAssistantChatIntent : BaseViewModel.State {
    data class SubmitMessage(
        val message: String,
        val displayMessage: String,
        val plantName: String = "",
    ) : PlantAssistantChatIntent()
}

class PlantAssistantChatViewModel(
    application: Application,
    private val chatRepository: PlantAssistantChatRepository
) : BaseViewModel<PlantAssistantChatIntent>(application) {

    private val _replyImages = MutableStateFlow<List<String>>(emptyList())
    val replyImages: StateFlow<List<String>> = _replyImages.asStateFlow()

    private val _requestResult = MutableStateFlow<ResultFlow<String>>(ResultFlow.Initial())
    val requestResult: StateFlow<ResultFlow<String>> = _requestResult.asStateFlow()

    private val _queuedUserMessage = MutableStateFlow<QueuedUserMessage?>(null)
    val queuedUserMessage: StateFlow<QueuedUserMessage?> = _queuedUserMessage.asStateFlow()

    override fun onState(state: PlantAssistantChatIntent) {
        when (state) {
            is PlantAssistantChatIntent.SubmitMessage -> submitUserMessage(state)
        }
    }

    private fun submitUserMessage(state: PlantAssistantChatIntent.SubmitMessage) {
        val message = state.message
        if (message.isBlank()) return

        _queuedUserMessage.value = QueuedUserMessage(
            displayMessage = state.displayMessage,
            imageUri = null,
        )

        launchBlock(Dispatchers.IO) {
            try {
                _requestResult.value = ResultFlow.Loading()

                val localPest = ChatPestIllustration.build(
                    applicationContext,
                    message,
                    state.plantName,
                )
                if (localPest != null) {
                    _replyImages.value = localPest.imageUrls
                    _requestResult.value = ResultFlow.Success(localPest.text)
                    return@launchBlock
                }
                _replyImages.value = emptyList()

                var fullResponse = ""
                chatRepository.sendMessage(message).collect { result ->
                    result.fold(
                        onSuccess = { fullResponse = it },
                        onFailure = {
                            Log.e(TAG, "submitUserMessage", it)
                            _requestResult.value = ResultFlow.Error(it.message ?: applicationContext.getString(R.string.chat_unknown_error))
                        }
                    )
                }

                if (fullResponse.isNotBlank()) {
                    _requestResult.value = ResultFlow.Success(fullResponse)
                } else {
                    _requestResult.value = ResultFlow.Error(applicationContext.getString(R.string.chat_no_response_from_ai))
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitUserMessage", e)
                _requestResult.value = ResultFlow.Error(e.message ?: applicationContext.getString(R.string.chat_unknown_error))
            }
        }
    }

    companion object {
        private const val TAG = "PlantAssistantChatVM"
    }
}
