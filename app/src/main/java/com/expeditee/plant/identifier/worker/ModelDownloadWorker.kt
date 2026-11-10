package com.expeditee.plant.identifier.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    companion object {
        private const val LOG_TAG = "TAG123213"
    }

    private val okHttpClient: OkHttpClient by inject()
    private val remoteConfigManagement: RemoteConfigManagement by inject()

    override suspend fun doWork(): Result {
        val overallStartTime = System.currentTimeMillis()
        Log.d(LOG_TAG, "==============================================")
        Log.d(LOG_TAG, "ModelDownloadWorker: STARTING SYNC PROCESS")
        Log.d(LOG_TAG, "==============================================")
        
        val maxRetries = 50
        var retries = 0
        while (!remoteConfigManagement.isConfigFetched && retries < maxRetries) {
            kotlinx.coroutines.delay(100)
            retries++
        }
        
        if (!remoteConfigManagement.isConfigFetched) {
            Log.e(LOG_TAG, "ModelDownloadWorker config not fetched after max retries")
            return Result.retry()
        }
        
        val baseUrl = remoteConfigManagement.baseUrlData.trimEnd('/')
        if (baseUrl.isBlank()) {
            Log.e(LOG_TAG, "ModelDownloadWorker baseUrl is empty")
            return Result.retry()
        }

        val modelFile = File(applicationContext.filesDir, Constants.IDENTIFY_MODEL_SUB_PATH)
        val taxonomyFile = File(applicationContext.filesDir, Constants.TAXONOMY_SUB_PATH)
        
        modelFile.parentFile?.mkdirs()
        taxonomyFile.parentFile?.mkdirs()

        // 1. Kiểm tra Model
        val modelUrl = "$baseUrl/${Constants.IDENTIFY_MODEL_FILE_NAME}"
        Log.d(LOG_TAG, ">>> [1/2] Checking Model AI: ${modelFile.name}")
        if (!isValidTfliteModel(modelFile)) {
            Log.w(LOG_TAG, "Model invalid/missing. Initializing download...")
            if (!downloadWithRetry(modelUrl, modelFile)) {
                Log.e(LOG_TAG, "Failed to download Model AI.")
                return Result.retry()
            }
        } else {
            Log.i(LOG_TAG, "Model AI is READY: ${String.format("%.2f", modelFile.length() / 1024.0 / 1024.0)} MB")
        }

        // 2. Kiểm tra Taxonomy
        val taxonomyUrl = "$baseUrl/${Constants.TAXONOMY_FILE_NAME}"
        Log.d(LOG_TAG, ">>> [2/2] Checking Taxonomy: ${taxonomyFile.name}")
        if (!taxonomyFile.exists() || taxonomyFile.length() < 100) {
            Log.w(LOG_TAG, "Taxonomy missing. Initializing download...")
            if (!downloadWithRetry(taxonomyUrl, taxonomyFile)) {
                Log.e(LOG_TAG, "Failed to download Taxonomy.")
                return Result.retry()
            }
        } else {
            Log.i(LOG_TAG, "Taxonomy is READY: ${String.format("%.2f", taxonomyFile.length() / 1024.0)} KB")
        }

        val totalTime = System.currentTimeMillis() - overallStartTime
        Log.d(LOG_TAG, "==============================================")
        Log.d(LOG_TAG, "ModelDownloadWorker: ALL ASSETS SYNCHRONIZED")
        Log.d(LOG_TAG, "TOTAL PROCESS TIME: ${totalTime}ms")
        Log.d(LOG_TAG, "==============================================")
        return Result.success()
    }

    private fun downloadWithRetry(url: String, targetFile: File): Boolean {
        val tempFile = File(targetFile.absolutePath + ".tmp")
        val startTime = System.currentTimeMillis()
        Log.d(LOG_TAG, "Starting Download: $url")
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantScan-Android-Worker")
                .apply {
                    val token = remoteConfigManagement.token.trim()
                    if (token.isNotBlank()) {
                        Log.d(LOG_TAG, "Using GitHub Token for authorization")
                        header("Authorization", "token $token")
                    }
                    header("Accept", "application/vnd.github.v3.raw")
                }.build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(LOG_TAG, "HTTP Response: ${response.code} ${response.message}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(512).string()
                    Log.e(LOG_TAG, "Server returned error: $errorBody")
                    return false
                }
                
                val body = response.body ?: return false
                val contentLength = body.contentLength()
                Log.d(LOG_TAG, "Content Length: ${contentLength / 1024} KB")

                tempFile.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }

                val downloadTime = System.currentTimeMillis() - startTime
                Log.d(LOG_TAG, "Download completed in ${downloadTime}ms. Verifying file...")

                if (targetFile.name.endsWith(".tflite")) {
                    if (isValidTfliteModel(tempFile)) {
                        if (tempFile.renameTo(targetFile)) {
                            Log.i(LOG_TAG, "Model successfully verified and saved to: ${targetFile.absolutePath}")
                            return true
                        }
                    } else {
                        Log.e(LOG_TAG, "CRITICAL: Downloaded TFLite file failed validation (possible corrupt stream).")
                        tempFile.delete()
                        return false
                    }
                } else {
                    tempFile.renameTo(targetFile)
                    Log.i(LOG_TAG, "File saved successfully: ${targetFile.name}")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Download Interrupted: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            return false
        }
        return false
    }

    private fun isValidTfliteModel(file: File): Boolean {
        if (!file.exists()) return false
        if (file.length() < 1024) {
            Log.w(LOG_TAG, "Validation failed: File too small (${file.length()} bytes)")
            return false
        }
        
        return try {
            FileInputStream(file).use { inputStream ->
                val fileChannel = inputStream.channel
                val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
                val interpreter = Interpreter(modelBuffer)
                interpreter.close()
                Log.d(LOG_TAG, "Validation SUCCESS: TFLite Interpreter initialized correctly.")
            }
            true
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Validation ERROR for ${file.name}: ${error.message}")
            false
        }
    }
}
