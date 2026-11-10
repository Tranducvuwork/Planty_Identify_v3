package com.expeditee.plant.identifier.presenter.onboarding

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Onboarding(
    val img: Int,
    val title: Int,
    val subText: Int,
) : Parcelable

