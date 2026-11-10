package com.expeditee.plant.identifier.utils

import android.content.Context
import android.util.TypedValue

object ViewUtils {
    
    fun getPxFromDp(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}

fun Context.getPxFromDp(dp: Float): Int {
    return ViewUtils.getPxFromDp(this, dp)
}

