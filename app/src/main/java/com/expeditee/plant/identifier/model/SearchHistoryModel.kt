package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class SearchHistoryModel(
    val id: Long,
    val query: String,
    val timestamp: Long
)