package com.expeditee.plant.identifier.extension

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.ReplacementSpan
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.set
import androidx.core.widget.doOnTextChanged
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.helper.text.MySpanBuilder
import com.google.android.gms.common.internal.Preconditions.checkMainThread
import io.noties.markwon.core.spans.CustomTypefaceSpan
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import androidx.core.graphics.toColorInt

fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow {
        checkMainThread()
        val listener = doOnTextChanged { text, _, _, _ -> trySend(text) }
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}

fun TextView.loadSpan(builderActions: MySpanBuilder.() -> Unit) {
    val builder = MySpanBuilder()
    builder.builderActions()
    builder.applyTo(this)
}

fun EditText.setOnDoneAction(action: () -> Unit) {
    imeOptions = EditorInfo.IME_ACTION_DONE
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            action.invoke()
        }
        false
    }
}

fun TextView.setScientificTextCustom(
    prefix: String,
    suffix: String,
    prefixFontRes: Int = R.font.montserrat_medium,
    suffixFontRes: Int = R.font.montserrat_light,
    prefixColor: Int = Color.BLACK,
    spacingDp: Int = 3,
    suffixColor: Int = "#888888".toColorInt()
) {
    val spacingPx = (spacingDp * context.resources.displayMetrics.density).toInt()

    val spacingString = " ".repeat(2)
    val fullText = "$prefix$spacingString$suffix"

    val spannable = SpannableString(fullText)

    val prefixTypeface = getFontTypeface(prefixFontRes)
    val suffixTypeface = getFontTypeface(suffixFontRes)

    spannable.setSpan(
        CustomTypefaceSpan(prefixTypeface),
        0,
        prefix.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    spannable.setSpan(
        ForegroundColorSpan(prefixColor),
        0,
        prefix.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    spannable.setSpan(
        MarginSpan(leftMarginPx = spacingPx),
        prefix.length,
        prefix.length + spacingString.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    spannable.setSpan(
        CustomTypefaceSpan(suffixTypeface),
        prefix.length + spacingString.length,
        fullText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    spannable.setSpan(
        ForegroundColorSpan(suffixColor),
        prefix.length + spacingString.length,
        fullText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    this.text = spannable
}

private fun TextView.getFontTypeface(fontRes: Int): Typeface {
    return try {
        ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
}


class MarginSpan(
    private val leftMarginPx: Int = 0,
    private val rightMarginPx: Int = 0
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val contentWidth = paint.measureText(text, start, end).toInt()
        return leftMarginPx + contentWidth + rightMarginPx
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        // Draw text with left margin offset
        canvas.drawText(
            text,
            start,
            end,
            x + leftMarginPx,
            y.toFloat(),
            paint
        )
    }
}
