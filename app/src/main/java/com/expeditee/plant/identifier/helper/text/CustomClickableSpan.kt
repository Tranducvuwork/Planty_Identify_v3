package com.expeditee.plant.identifier.helper.text

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class CustomClickableSpan(
    private val textColor: Int,
    private val action: () -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) {
        action.invoke()
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.color = textColor
        ds.isUnderlineText = false
    }
}
