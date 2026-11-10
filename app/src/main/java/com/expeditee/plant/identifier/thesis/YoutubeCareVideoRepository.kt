package com.expeditee.plant.identifier.thesis

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class YoutubeCareVideoRepository(
    private val client: OkHttpClient,
) {
    private val cache = mutableMapOf<String, String>()

    fun lookupVideoId(name: String, scientific: String): String? {
        val key = "${scientific.ifBlank { name }.trim().lowercase()}"
        if (key.isBlank()) {
            Log.w(TAG, "skip lookup: empty plant name")
            return null
        }
        cache[key]?.let { cached ->
            if (CareVideoLookup.trusted(cached) == null) {
                Log.w(TAG, "drop blocked cache key='$key' videoId=$cached")
                cache.remove(key)
            } else {
                Log.i(TAG, "cache hit key='$key' videoId=$cached watch=${CareVideoLookup.watchUrl(cached)}")
                return cached
            }
        }
        val query = CareVideoLookup.searchQuery(name, scientific)
        Log.i(TAG, "innertube search name='$name' scientific='$scientific' query='$query'")
        val json = fetchSearchJson(query) ?: return null
        val hits = CareVideoLookup.parseSearchHits(json)
        hits.take(8).forEachIndexed { index, hit ->
            Log.i(TAG, "candidate[$index] id=${hit.videoId} title=${hit.title}")
        }
        val picked = CareVideoLookup.pickForPlant(name, scientific, hits) ?: hits.firstOrNull()
        if (picked == null) {
            Log.w(TAG, "no youtube hits for query='$query'")
            return null
        }
        cache[key] = picked.videoId
        Log.i(
            TAG,
            "picked id=${picked.videoId} title='${picked.title}' " +
                "watch=${CareVideoLookup.watchUrl(picked.videoId)}",
        )
        return picked.videoId
    }

    private fun fetchSearchJson(query: String): String? {
        val payload = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB")
                        .put("clientVersion", "2.20240101.00.00")
                        .put("hl", "vi")
                        .put("gl", "VN"),
                ),
            )
            .put("query", query)
            .toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
            .header("User-Agent", DESKTOP_UA)
            .header("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.8")
            .post(payload.toRequestBody(JSON))
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                Log.i(TAG, "HTTP ${response.code} innertube bytes=${response.body?.contentLength()}")
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.onFailure {
            Log.w(TAG, "innertube search failed: ${it.message}")
        }.getOrNull()
    }

    companion object {
        private const val TAG = CareVideoLookup.LOG_TAG
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
