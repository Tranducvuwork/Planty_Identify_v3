package com.expeditee.plant.identifier.admob.handler

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

interface RetryHandler {
    val maxRetryAttempts: Int
    suspend fun retry(placement: String, block: suspend CoroutineScope.() -> Unit): Boolean
}

class AdmobRetryHandler(
    override val maxRetryAttempts: Int,
    private val retryTimeout: Long = 10000L,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RetryHandler {

    companion object {

        private const val TAG = "AdRetryHandler"
        private const val INITIAL_DELAY = 2000L
    }

    override suspend fun retry(
        placement: String,
        block: suspend CoroutineScope.() -> Unit
    ) = withContext(coroutineDispatcher) {
        repeat(maxRetryAttempts) { retryAttempts ->
            try {
                withTimeout(retryTimeout) {
                    block.invoke(this)
                }
                Log.d(TAG, "Ad loaded successfully for $placement")
                return@withContext true
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Ad load timeout for $placement, attempt ${retryAttempts + 1}")
                if (retryAttempts < maxRetryAttempts) {
                    delay(INITIAL_DELAY)
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error loading ad for $placement, attempt ${retryAttempts + 1}: ${e.message}"
                )
                if (retryAttempts < maxRetryAttempts) {
                    delay(INITIAL_DELAY)
                }
            }
        }
        Log.e(TAG, "Failed to load ad for $placement after $maxRetryAttempts attempts")
        return@withContext false
    }
}
