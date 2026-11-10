package com.expeditee.plant.identifier.model

import androidx.annotation.Keep

@Keep
data class ScanCount(
    val freeUsed: Int = 0,
    val rewardUsed: Int = 0,
) {
    companion object {
        const val FREE_LIMIT = 1
        const val MAX_REWARD = 2
    }

    val isFreeAvailable: Boolean get() = freeUsed < FREE_LIMIT
    val isRewardAvailable: Boolean get() = rewardUsed < MAX_REWARD

    val remaining: Int
        get() = (FREE_LIMIT - freeUsed).coerceAtLeast(0) + (MAX_REWARD - rewardUsed).coerceAtLeast(0)

    val limit: Int get() = FREE_LIMIT + MAX_REWARD
}
