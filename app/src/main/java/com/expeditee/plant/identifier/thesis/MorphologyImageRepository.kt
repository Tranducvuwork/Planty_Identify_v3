package com.expeditee.plant.identifier.thesis

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Resolves morphology illustrations for any species via Wikimedia Commons search
 * keyed by scientific name + plant part (root / stem / leaf / flower / fruit).
 */
class MorphologyImageRepository(
    private val client: OkHttpClient,
) {
    private val cache = mutableMapOf<String, MorphologyPartImages>()
    private val mutex = Mutex()

    suspend fun load(scientificName: String): MorphologyPartImages = withContext(Dispatchers.IO) {
        val scientific = scientificName.trim()
        if (scientific.isBlank()) return@withContext MorphologyPartImages()
        val cacheKey = scientific.lowercase()
        mutex.withLock { cache[cacheKey] }?.let { return@withContext it }

        Log.i(TAG, "resolve morphology for '$scientific'")
        val images = coroutineScope {
            MorphologyPart.entries.map { part ->
                async { part to lookupPart(scientific, part) }
            }.awaitAll().fold(MorphologyPartImages()) { acc, (part, url) ->
                acc.with(part, url)
            }
        }
        mutex.withLock { cache[cacheKey] = images }
        Log.i(
            TAG,
            "done '$scientific' root=${images.root != null} stem=${images.stem != null} " +
                "leaf=${images.leaf != null} flower=${images.flower != null} fruit=${images.fruit != null}",
        )
        images
    }

    private fun lookupPart(scientific: String, part: MorphologyPart): String? {
        val queries = MorphologyImageLookup.searchQueries(scientific, part)
        for (query in queries) {
            val json = fetchSearchJson(query) ?: continue
            val candidates = MorphologyImageLookup.parseCandidates(json)
            val picked = MorphologyImageLookup.pickBest(scientific, part, candidates)
            if (picked != null) {
                Log.i(TAG, "$part query='$query' -> ${picked.title}")
                return picked.url
            }
        }
        Log.w(TAG, "$part no match for '$scientific'")
        return null
    }

    private fun fetchSearchJson(query: String): String? {
        val url = "https://commons.wikimedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", "filetype:bitmap $query")
            .addQueryParameter("gsrnamespace", "6")
            .addQueryParameter("gsrlimit", "12")
            .addQueryParameter("prop", "imageinfo")
            .addQueryParameter("iiprop", "url|mime|size")
            .addQueryParameter("iiurlwidth", "640")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for query='$query'")
                    return@use null
                }
                response.body?.string()
            }
        }.onFailure {
            Log.w(TAG, "commons search failed: ${it.message}")
        }.getOrNull()
    }

    companion object {
        private const val TAG = "MorphologyImage"
        private const val USER_AGENT =
            "PlantyIdentifyThesis/1.0 (Android; educational plant morphology; contact=thesis)"
    }
}
