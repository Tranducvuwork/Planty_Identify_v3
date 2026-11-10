package com.expeditee.plant.identifier.presenter.home.more

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Keep

@Keep
data class MoreOptionItem(
    val type: MoreOptionType,
    @DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
    @DrawableRes val backgroundDrawable: Int,
    @ColorRes val titleColor: Int,
    @ColorRes val subtitleColor: Int,
    val onClick: () -> Unit = {}
)
