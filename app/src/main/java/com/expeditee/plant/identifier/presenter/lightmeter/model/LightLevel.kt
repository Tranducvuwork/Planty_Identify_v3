package com.expeditee.plant.identifier.presenter.lightmeter.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.expeditee.plant.identifier.R

enum class LightLevel(
    @StringRes val messageRes: Int,
    @DrawableRes val iconRes: Int,
    val minLux: Int,
    val maxLux: Int
) {
    VERY_LOW(
        R.string.light_very_low,
        R.drawable.ic_warning,
        0,
        99
    ),
    LOW(
        R.string.light_low,
        R.drawable.ic_warning,
        100,
        399
    ),
    OPTIMAL(
        R.string.light_optimal,
        R.drawable.ic_sunlight,
        430,
        1194
    ),
    BRIGHT(
        R.string.light_bright,
        R.drawable.ic_warning,
        1200,
        9999
    ),
    TOO_BRIGHT(
        R.string.light_too_bright,
        R.drawable.ic_warning,
        10000,
        Int.MAX_VALUE
    );

    companion object {
        fun fromLux(lux: Int): LightLevel = when {
            lux < 100 -> VERY_LOW
            lux < 400 -> LOW
            lux < 1200 -> OPTIMAL
            lux < 10000 -> BRIGHT
            else -> TOO_BRIGHT
        }
    }
}