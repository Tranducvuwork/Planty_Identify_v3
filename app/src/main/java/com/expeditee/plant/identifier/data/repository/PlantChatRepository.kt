package com.expeditee.plant.identifier.data.repository

import android.content.Context
import android.util.Log
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.network.PlantBackendApiService
import com.expeditee.plant.identifier.network.model.PlantChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PlantChatRepository(
    private val apiService: PlantBackendApiService,
    private val context: Context,
) {

    private val gson = Gson()

    suspend fun sendChatMessage(message: String, language: String): Result<String> =
        withContext(Dispatchers.IO) {
            val trimmed = message.trim()
            Log.d(TAG, "sendChatMessage: lang=$language msgLen=${trimmed.length}")

            if (trimmed.isBlank()) {
                return@withContext Result.failure(Exception(context.getString(R.string.chat_unknown_error)))
            }

            try {
                val response = apiService.sendChat(message = trimmed, language = language)
                mapResponse(response)
            } catch (e: HttpException) {
                handleHttpError(e)?.let { return@withContext it }
                Log.e(TAG, "sendChatMessage HTTP ${e.code()}", e)
                Result.failure(Exception(context.getString(R.string.chat_unknown_error)))
            } catch (e: Exception) {
                Log.e(TAG, "sendChatMessage error", e)
                Result.failure(toUserError(e))
            }
        }

    private fun mapResponse(response: PlantChatResponse): Result<String> {
        if (!response.status.equals("success", ignoreCase = true)) {
            val msg = response.message?.trim()
            return if (msg.isNullOrBlank()) {
                Result.failure(Exception(context.getString(R.string.chat_unknown_error)))
            } else {
                Result.success(msg)
            }
        }

        val answer = response.data?.answer?.trim()
        return if (answer.isNullOrBlank()) {
            Result.failure(Exception(context.getString(R.string.chat_no_response_from_ai)))
        } else {
            Result.success(answer)
        }
    }

    private fun handleHttpError(e: HttpException): Result<String>? {
        val body = e.response()?.errorBody()?.string()?.trim().orEmpty()
        if (body.isBlank()) return null
        val parsed = runCatching { gson.fromJson(body, PlantChatResponse::class.java) }.getOrNull()
        if (parsed != null) {
            val msg = parsed.message?.trim()
            if (!msg.isNullOrBlank()) return Result.success(msg)
        }
        return null
    }

    private fun toUserError(e: Throwable): Exception {
        val msg = when {
            isNetworkError(e) -> R.string.no_internet
            else -> null
        }
        val text = msg?.let { context.getString(it) }
            ?: (e.message ?: context.getString(R.string.chat_unknown_error))
        return Exception(text, e)
    }

    private fun isNetworkError(e: Throwable): Boolean =
        findCause<ConnectException>(e) != null ||
            findCause<SocketTimeoutException>(e) != null ||
            findCause<UnknownHostException>(e) != null ||
            findCause<IOException>(e) != null

    private inline fun <reified T : Throwable> findCause(e: Throwable): T? {
        var current: Throwable? = e
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    companion object {
        private const val TAG = "PlantChatRepo"
    }
}
