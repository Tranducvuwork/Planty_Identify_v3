package com.expeditee.plant.identifier.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Faq(
    val id: Int,
    val question: String,
    val answer: String,
    val tags: List<String>,
    val imageUrl: String
) : Parcelable