package com.expeditee.plant.identifier.utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.expeditee.plant.identifier.R
import kotlin.let


class ReadMoreTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    AppCompatTextView(context, attrs) {
    private var sourceText: CharSequence? = null
    private var bufferType: BufferType? = null
    private var readMore = true
    private var trimLength: Int
    private var trimCollapsedText: CharSequence
    private var trimExpandedText: CharSequence
    private val viewMoreSpan: ReadMoreClickableSpan
    private var onClickViewMoreSpan: () -> Unit = {}
    private var colorClickableText: Int
    private var lessTextColor: Int? = null
    private var moreTextColor: Int? = null
    private val showTrimExpandedText: Boolean
    private var trimMode: Int
    private var lineEndIndex = 0
    private var trimLines: Int
    private var lastMeasuredWidth = 0
    private var pendingLineTrim = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ReadMoreTextView)
        trimLength = typedArray.getInt(R.styleable.ReadMoreTextView_trimLength, DEFAULT_TRIM_LENGTH)
        val resourceIdTrimCollapsedText = typedArray.getResourceId(
            R.styleable.ReadMoreTextView_trimCollapsedText,
            R.string.read_more
        )
        val resourceIdTrimExpandedText = typedArray.getResourceId(
            R.styleable.ReadMoreTextView_trimExpandedText,
            R.string.show_less
        )
        trimCollapsedText = resources.getString(resourceIdTrimCollapsedText)
        trimExpandedText = resources.getString(resourceIdTrimExpandedText)
        trimLines = typedArray.getInt(R.styleable.ReadMoreTextView_trimLines, DEFAULT_TRIM_LINES)
        colorClickableText = typedArray.getColor(
            R.styleable.ReadMoreTextView_colorClickableText,
            ContextCompat.getColor(context, R.color.primary_green)
        )
        showTrimExpandedText = typedArray.getBoolean(
            R.styleable.ReadMoreTextView_showTrimExpandedText,
            DEFAULT_SHOW_TRIM_EXPANDED_TEXT
        )
        trimMode = typedArray.getInt(R.styleable.ReadMoreTextView_trimMode, TRIM_MODE_LINES)
        typedArray.recycle()
        viewMoreSpan = ReadMoreClickableSpan()
        onGlobalLayoutLineEndIndex()
        applyTrimmedText()
    }

    private fun applyTrimmedText() {
        super.setText(displayableText, bufferType ?: BufferType.NORMAL)
        movementMethod = LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT
    }

    private val displayableText: CharSequence?
        get() = getTrimmedText(sourceText)

    override fun setText(text: CharSequence?, type: BufferType?) {
        this.sourceText = text
        bufferType = type
        if (trimMode == TRIM_MODE_LINES) {
            pendingLineTrim = true
            super.setText(sourceText, bufferType ?: BufferType.NORMAL)
            post { applyLineTrimIfPossible() }
        } else {
            applyTrimmedText()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (trimMode != TRIM_MODE_LINES) return
        val currentWidth = width
        val widthChanged = currentWidth != lastMeasuredWidth
        if (changed || widthChanged || pendingLineTrim) {
            lastMeasuredWidth = currentWidth
            applyLineTrimIfPossible()
        }
    }

    private fun applyLineTrimIfPossible() {
        if (trimMode != TRIM_MODE_LINES) return
        if (width <= 0 || sourceText.isNullOrEmpty()) {
            pendingLineTrim = true
            return
        }
        super.setText(sourceText, bufferType ?: BufferType.NORMAL)
        refreshLineEndIndex()
        applyTrimmedText()
        pendingLineTrim = false
    }

    private fun getTrimmedText(text: CharSequence?): CharSequence? {
        if (trimMode == TRIM_MODE_LENGTH) {
            if (text != null && text.length > trimLength) {
                return if (readMore) {
                    updateCollapsedText()
                } else {
                    updateExpandedText()
                }
            }
        }
        if (trimMode == TRIM_MODE_LINES) {
            if (text != null && lineEndIndex > 0) {
                if (readMore) {
                    if (layout.lineCount > trimLines) {
                        lessTextColor?.let {
                            setColorClickableText(it)
                        }
                        return updateCollapsedText()
                    }
                } else {
                    moreTextColor?.let {
                        setColorClickableText(it)
                    }
                    return updateExpandedText()
                }
            }
        }
        return text
    }

    private fun updateCollapsedText(): CharSequence {
        val text = sourceText ?: return ""
        var trimEndIndex = text.length
        when (trimMode) {
            TRIM_MODE_LINES -> {
                trimEndIndex = lineEndIndex - (ELLIPSIZE.length + trimCollapsedText.length + 1)
                if (trimEndIndex < 0) {
                    trimEndIndex = trimLength + 1
                }
            }

            TRIM_MODE_LENGTH -> trimEndIndex = trimLength + 1
        }
        val s = SpannableStringBuilder(text, 0, trimEndIndex)
            .append(ELLIPSIZE)
            .append(trimCollapsedText)
        return addClickableSpan(s, trimCollapsedText)
    }

    private fun updateExpandedText(): CharSequence? {
        val text = sourceText ?: return null
        if (showTrimExpandedText) {
            val s = SpannableStringBuilder(
                text,
                0,
                text.length
            ).append(context.getString(R.string.space)).append(trimExpandedText)
            return addClickableSpan(s, trimExpandedText)
        }
        return text
    }

    private fun addClickableSpan(s: SpannableStringBuilder, trimText: CharSequence): CharSequence {
        s.setSpan(
            viewMoreSpan,
            s.length - trimText.length,
            s.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return s
    }

    fun setTrimLength(trimLength: Int) {
        this.trimLength = trimLength
        applyTrimmedText()
    }

    private fun setColorClickableText(colorClickableText: Int) {
        try {
            this.colorClickableText = ContextCompat.getColor(context, colorClickableText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setExpandedTextColor(moreTextColor: Int) {
        this.moreTextColor = moreTextColor
    }

    fun setCollapsedTextColor(lessTextColor: Int) {
        this.lessTextColor = lessTextColor
    }

    fun setCollapsedText(trimCollapsedText: CharSequence) {
        this.trimCollapsedText = trimCollapsedText
    }

    fun setExpandedText(trimExpandedText: CharSequence) {
        this.trimExpandedText = trimExpandedText
    }

    fun setTrimMode(trimMode: Int) {
        this.trimMode = trimMode
    }

    fun setTrimLines(trimLines: Int) {
        this.trimLines = trimLines
    }

    fun setOnClickViewMoreSpan(
        onClick: () -> Unit
    ) {
        this.onClickViewMoreSpan = onClick
    }

    private inner class ReadMoreClickableSpan : ClickableSpan() {
        override fun onClick(widget: View) {
            readMore = !readMore
            if (trimMode == TRIM_MODE_LINES) {
                refreshLineEndIndex()
            }
            applyTrimmedText()
            onClickViewMoreSpan()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = colorClickableText
        }
    }

    private fun onGlobalLayoutLineEndIndex() {
        if (trimMode == TRIM_MODE_LINES) {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val obs = viewTreeObserver
                    obs.removeOnGlobalLayoutListener(this)
                    refreshLineEndIndex()
                    applyTrimmedText()
                }
            })
        }
    }

    private fun refreshLineEndIndex() {
        try {
            val localLayout = layout
            if (localLayout == null) {
                lineEndIndex = INVALID_END_INDEX
                return
            }
            lineEndIndex = when (trimLines) {
                0 -> {
                    if (localLayout.lineCount > 0) localLayout.getLineEnd(0) else INVALID_END_INDEX
                }

                in 1..localLayout.lineCount -> {
                    localLayout.getLineEnd(trimLines - 1)
                }

                else -> {
                    INVALID_END_INDEX
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TRIM_MODE_LINES = 0
        private const val TRIM_MODE_LENGTH = 1
        private const val DEFAULT_TRIM_LENGTH = 240
        private const val DEFAULT_TRIM_LINES = 2
        private const val INVALID_END_INDEX = -1
        private const val DEFAULT_SHOW_TRIM_EXPANDED_TEXT = true
        private const val ELLIPSIZE = "... "
    }
}
