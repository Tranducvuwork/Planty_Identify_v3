package com.expeditee.plant.identifier.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class NvidiaChatRequest(
    val model: String,
    val messages: List<NvidiaChatMessage>,
    val temperature: Double,
    @SerializedName("top_p") val topP: Double,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean,
    @SerializedName("chat_template_kwargs") val chatTemplateKwargs: NvidiaChatTemplateKwargs,
    @SerializedName("reasoning_budget") val reasoningBudget: Int? = null,
)

@Keep
data class NvidiaChatMessage(
    val role: String,
    val content: String,
)

@Keep
data class NvidiaChatTemplateKwargs(
    @SerializedName("enable_thinking") val enableThinking: Boolean,
)

@Keep
data class NvidiaChatResponse(
    val choices: List<NvidiaChatChoice>? = null,
)

@Keep
data class NvidiaChatChoice(
    val message: NvidiaChatMessageResponse? = null,
)

@Keep
data class NvidiaChatMessageResponse(
    val content: String? = null,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
)
