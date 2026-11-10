package com.expeditee.plant.identifier.data.repository

import android.util.Log
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.network.NvidiaChatApiService
import com.expeditee.plant.identifier.network.model.NvidiaChatMessage
import com.expeditee.plant.identifier.network.model.NvidiaChatRequest
import com.expeditee.plant.identifier.network.model.NvidiaChatTemplateKwargs
import com.expeditee.plant.identifier.network.model.PlantScanResult

class NvidiaVietnameseNameRepository(
    private val apiService: NvidiaChatApiService,
) {
    private val cache = mutableMapOf<String, String>()

    suspend fun fillIfMissing(result: PlantScanResult): PlantScanResult {
        if (!VietnameseNameLookup.needsRemoteLookup(result.commonNames, result.scientificName)) {
            return result
        }
        val name = lookupName(result.scientificName, result.commonNames) ?: return result
        return result.copy(
            commonNames = listOf(name) + result.commonNames.filter {
                it.isNotBlank() &&
                    it != name &&
                    it != VietnameseNameLookup.UNKNOWN_PLACEHOLDER
            },
        )
    }

    suspend fun enrichFacts(result: PlantScanResult): PlantScanResult {
        if (!PlantFactsLookup.needsFacts(result.uses, result.description)) return result
        val facts = lookupFacts(result) ?: return result
        val care = result.care
        return result.copy(
            description = result.description?.takeIf {
                it.isNotBlank() && !PlantFactsLookup.needsFacts(emptyList(), it)
            } ?: facts.description ?: result.description,
            uses = facts.uses.ifEmpty { result.uses },
            origin = result.origin?.takeIf { it.isNotBlank() } ?: facts.origin ?: result.origin,
            care = care?.copy(
                planting = care.planting?.takeIf { it.isNotBlank() } ?: facts.planting ?: care.planting,
                care = care.care?.takeIf { it.isNotBlank() } ?: facts.care ?: care.care,
            ) ?: if (!facts.planting.isNullOrBlank() || !facts.care.isNullOrBlank()) {
                com.expeditee.plant.identifier.network.model.CareData(
                    planting = facts.planting,
                    care = facts.care,
                )
            } else {
                null
            },
        )
    }

    private suspend fun lookupName(scientificName: String, commonNames: List<String>): String? {
        val key = scientificName.trim().lowercase()
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        if (BuildConfig.NVIDIA_API_KEY.isBlank()) {
            Log.w(TAG, "skip lookup: NVIDIA_API_KEY is empty")
            return null
        }
        val response = runCatching {
            apiService.chatCompletions(
                NvidiaChatRequest(
                    model = BuildConfig.NVIDIA_MODEL,
                    messages = listOf(
                        NvidiaChatMessage(
                            role = "user",
                            content = VietnameseNameLookup.prompt(scientificName, commonNames),
                        ),
                    ),
                    temperature = 0.2,
                    topP = 0.95,
                    maxTokens = 64,
                    stream = false,
                    chatTemplateKwargs = NvidiaChatTemplateKwargs(enableThinking = false),
                ),
            )
        }.onFailure {
            Log.w(TAG, "lookup failed for '$scientificName': ${it.message}")
        }.getOrNull() ?: return null

        val message = response.choices?.firstOrNull()?.message
        val parsed = VietnameseNameLookup.parseModelAnswer(message?.content)
            ?: VietnameseNameLookup.parseModelAnswer(message?.reasoningContent)
        if (parsed == null) {
            Log.w(TAG, "lookup empty for '$scientificName' content='${message?.content}'")
            return null
        }
        cache[key] = parsed
        Log.i(TAG, "lookup '$scientificName' -> '$parsed'")
        return parsed
    }

    private suspend fun lookupFacts(result: PlantScanResult): PlantFacts? {
        if (BuildConfig.NVIDIA_API_KEY.isBlank()) return null
        val common = result.commonNames.firstOrNull().orEmpty().ifBlank { result.scientificName }
        val response = runCatching {
            apiService.chatCompletions(
                NvidiaChatRequest(
                    model = BuildConfig.NVIDIA_MODEL,
                    messages = listOf(
                        NvidiaChatMessage(
                            role = "user",
                            content = PlantFactsLookup.prompt(result.scientificName, common),
                        ),
                    ),
                    temperature = 0.2,
                    topP = 0.95,
                    maxTokens = 700,
                    stream = false,
                    chatTemplateKwargs = NvidiaChatTemplateKwargs(enableThinking = false),
                ),
            )
        }.onFailure {
            Log.w(TAG, "facts failed for '${result.scientificName}': ${it.message}")
        }.getOrNull() ?: return null
        val message = response.choices?.firstOrNull()?.message
        val facts = PlantFactsLookup.parseFacts(message?.content)
            ?: PlantFactsLookup.parseFacts(message?.reasoningContent)
        if (facts == null) {
            Log.w(TAG, "facts empty for '${result.scientificName}'")
        }
        return facts
    }

    companion object {
        private const val TAG = "NvidiaVnNameRepo"
    }
}
