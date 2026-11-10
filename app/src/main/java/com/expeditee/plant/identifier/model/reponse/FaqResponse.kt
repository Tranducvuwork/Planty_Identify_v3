package com.expeditee.plant.identifier.model.reponse

import com.expeditee.plant.identifier.model.Faq
import com.google.gson.annotations.SerializedName

data class FaqResponseItem(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("question")
    val question: String?,
    @SerializedName("answer")
    val answer: String?,
    @SerializedName("tags")
    val tags: List<String>?,
    @SerializedName("default_image")
    val defaultImage: FaqDefaultImage?
)

data class FaqDefaultImage(
    @SerializedName("url")
    val url: String?
)

fun FaqResponseItem.toFaq(): Faq {
    return Faq(
        id = id ?: 0,
        question = question.orEmpty(),
        answer = answer.orEmpty(),
        tags = tags.orEmpty(),
        imageUrl = defaultImage?.url.orEmpty()
    )
}
