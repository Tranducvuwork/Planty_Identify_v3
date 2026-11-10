package com.expeditee.plant.identifier.common

/**
 * Thesis/local build: ads and premium paywall are fully disabled.
 * All premium-gated features behave as unlocked.
 */
object AppMonetization {
    const val ADS_ENABLED = false
    const val PREMIUM_ENABLED = false

    /** Always unlocked when premium is disabled. */
    fun isUnlocked(): Boolean = true
}
