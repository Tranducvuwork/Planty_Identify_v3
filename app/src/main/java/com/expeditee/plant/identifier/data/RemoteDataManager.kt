package com.expeditee.plant.identifier.data

import android.content.Context
import android.util.Log
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class RemoteDataManager(
    private val context: Context,
    private val remoteConfig: RemoteConfigManagement
) {
    companion object {
        private const val CACHE_DURATION_HOURS = 24L
        private const val TAG = "RemoteDataManager"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "github_data")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    suspend fun loadJsonData(fileName: String, useCache: Boolean = true): String? =
        loadJsonData(fileName, remoteConfig.baseUrlData.orEmpty(), useCache)

    suspend fun loadJsonData(
        fileName: String,
        baseUrl: String?,
        useCache: Boolean = true,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val safeBaseUrl = baseUrl ?: remoteConfig.baseUrlData
            if (safeBaseUrl.isBlank()) {
                Log.e(TAG, "loadJsonData error: baseUrl is empty")
                return@withContext null
            }
            
            val cacheKey = "${safeBaseUrl.hashCode()}_$fileName"
            val cacheFile = File(cacheDir, cacheKey)

            if (useCache && cacheFile.exists() && isCacheValid(cacheFile)) {
                val cachedData = cacheFile.readText()
                if (cachedData.isNotBlank()) {
                    Log.d(TAG, "loadJsonData cache_hit file=$fileName size=${cachedData.length}")
                    return@withContext cachedData
                }
            }

            val base = safeBaseUrl.trimEnd('/')
            val path = fileName.trimStart('/')
            val url = "$base/$path"

            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val isOptionalLocalizedMiss = response.code == 404 && (
                        fileName.startsWith("detailed_plants_light_") ||
                            fileName.startsWith("json-") ||
                            fileName.startsWith("care_guides_") ||
                            fileName.startsWith("faq_data_") ||
                            fileName.startsWith("pest_disease_list_")
                        )
                    val message = "loadJsonData http_error file=$fileName code=${response.code} message=${response.message}"
                    if (isOptionalLocalizedMiss) {
                        Log.w(TAG, "optional localized miss $message")
                    } else {
                        Log.e(TAG, message)
                    }
                    return@withContext null
                }

                val jsonString = response.body?.string() ?: return@withContext null

                try {
                    cacheFile.parentFile?.mkdirs()
                    FileOutputStream(cacheFile).use { it.write(jsonString.toByteArray()) }
                } catch (e: Exception) {
                    Log.w(TAG, "loadJsonData cache_write_failed file=$fileName error=${e.message}")
                }

                jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadJsonData exception file=$fileName error=${e.message}", e)
            null
        }
    }

    private fun buildRequest(url: String): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "PlantScanV2/1.0")
            .addHeader("Accept", "application/vnd.github.v3.raw")

        val token = remoteConfig.token.trim()
        val isGitHubHost = url.contains("github.com") || url.contains("githubusercontent.com")
        if (token.isNotEmpty() && isGitHubHost) {
            builder.addHeader("Authorization", "token $token")
        }

        return builder.build()
    }

    private fun isCacheValid(file: File): Boolean {
        val lastModified = file.lastModified()
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - lastModified
        val cacheAgeHours = cacheAge / (1000 * 60 * 60)
        return cacheAgeHours < CACHE_DURATION_HOURS
    }

    fun clearCache() {
        runCatching {
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
        }
    }

    fun invalidateCacheFile(fileName: String, baseUrl: String? = null) {
        val safeBaseUrl = baseUrl ?: remoteConfig.baseUrlData
        if (safeBaseUrl.isBlank()) return

        runCatching {
            val cacheKey = "${safeBaseUrl.hashCode()}_$fileName"
            val cacheFile = File(cacheDir, cacheKey)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }

    fun clearMultilingualCacheExcept(languageCode: String) {
        if (!cacheDir.exists()) return

        val keepNonEnglish = languageCode != "en"
        cacheDir.walkBottomUp().forEach { file ->
            if (file == cacheDir) return@forEach

            val relativePath = file.relativeTo(cacheDir).invariantSeparatorsPath
            val shouldKeep = when {
                relativePath.contains("_json-") -> keepNonEnglish && relativePath.contains("_json-$languageCode/")
                relativePath.contains("_json/") -> true
                relativePath.matches(Regex(""".*_detailed_plants_[^/]+\.json$""")) ->
                    keepNonEnglish && relativePath.endsWith("_detailed_plants_${languageCode}.json")
                relativePath.matches(Regex(""".*_detailed_plants_light_[^/]+\.json$""")) ->
                    keepNonEnglish && relativePath.endsWith("_detailed_plants_light_${languageCode}.json")
                relativePath.endsWith("_detailed_plants_light.json") -> true
                relativePath.matches(Regex(""".*_faq_data_[^/]+\.json$""")) ->
                    keepNonEnglish && relativePath.endsWith("_faq_data_${languageCode}.json")
                relativePath.endsWith("_faq_data.json") -> true
                relativePath.matches(Regex(""".*_pest_disease_list_[^/]+\.json$""")) ->
                    keepNonEnglish && relativePath.endsWith("_pest_disease_list_${languageCode}.json")
                relativePath.endsWith("_pest_disease_list.json") -> true
                relativePath.matches(Regex(""".*_care_guides_[^/]+\.json$""")) ->
                    keepNonEnglish && relativePath.endsWith("_care_guides_${languageCode}.json")
                relativePath.endsWith("_care_guides.json") -> true
                else -> true
            }

            if (!shouldKeep) {
                runCatching {
                    if (file.isDirectory) {
                        if (file.listFiles().isNullOrEmpty()) {
                            file.delete()
                        }
                    } else {
                        file.delete()
                    }
                }
            }
        }
    }
}
