package com.expeditee.plant.identifier.presenter.home.more

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.expeditee.plant.identifier.R

enum class MoreOptionType(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @DrawableRes val background: Int,
    @ColorRes val titleColor: Int,
    @ColorRes val subtitleColor: Int
) {
    LIMITED_OFFER(
        icon = R.drawable.ic_home_nav,
        title = R.string.limited_offer_option,
        subtitle = R.string.limited_offer_option_sub,
        background = R.drawable.bg_more_option_green,
        titleColor = R.color.white,
        subtitleColor = R.color.white
    ),
    PLANT_FAQ(
        icon = R.drawable.ic_explore_nav_more,
        title = R.string.plant_faq,
        subtitle = R.string.answer_your_questions_about_plants,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    CARE_GUIDES(
        icon = R.drawable.ic_more_nav,
        title = R.string.care_guides,
        subtitle = R.string.take_care_of_our_plants_properly,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    DISEASE(
        icon = R.drawable.ic_diagnose_home,
        title = R.string.disease,
        subtitle = R.string.spot_and_manage_plant_health_issues,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    LIGHT_METER(
        icon = R.drawable.ic_camera_home,
        title = R.string.light_meter,
        subtitle = R.string.measure_the_light_where_your_plants_live,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    FOLK_REMEDY(
        icon = R.drawable.ic_explore_nav_more,
        title = R.string.thesis_folk_title,
        subtitle = R.string.thesis_folk_sub,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    PLANT_DIARY(
        icon = R.drawable.ic_more_nav,
        title = R.string.thesis_diary_title,
        subtitle = R.string.thesis_diary_sub,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
    QR_SCAN(
        icon = R.drawable.ic_camera_home,
        title = R.string.thesis_qr_title,
        subtitle = R.string.thesis_qr_sub,
        background = R.drawable.bg_more_option_white,
        titleColor = R.color.primary_green,
        subtitleColor = R.color.gray
    ),
}
