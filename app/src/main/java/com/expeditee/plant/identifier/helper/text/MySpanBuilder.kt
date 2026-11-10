package com.expeditee.plant.identifier.helper.text

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.widget.TextView

class MySpanBuilder(text: String? = null) {
    var spannedText: SpannableStringBuilder = if (text != null) {
        SpannableStringBuilder(text)
    } else {
        SpannableStringBuilder("")
    }
    private var lastSpanIndex = 0
    private var enableClickSpan = false

    fun appendText(text: String?): MySpanBuilder {
        lastSpanIndex = spannedText.length
        spannedText.append(text)
        return this
    }

    fun withSpan(span: Any?): MySpanBuilder {
        if (!enableClickSpan && span is ClickableSpan) {
            enableClickSpan = true
        }
        val endIndex = spannedText.length
        if (spannedText.isNotEmpty()) {
            spannedText.setSpan(span, lastSpanIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return this
    }

    fun length(): Int {
        return spannedText.length
    }

    fun clear() {
        spannedText.clearSpans()
        spannedText.clear()
    }


    fun applyTo(textView: TextView) {
        with(textView) {
            if (enableClickSpan) movementMethod = LinkMovementMethod.getInstance()
            text = spannedText
        }
    }
}
