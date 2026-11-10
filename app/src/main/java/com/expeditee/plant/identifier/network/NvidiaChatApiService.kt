package com.expeditee.plant.identifier.network

import com.expeditee.plant.identifier.network.model.NvidiaChatRequest
import com.expeditee.plant.identifier.network.model.NvidiaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface NvidiaChatApiService {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: NvidiaChatRequest): NvidiaChatResponse
}
