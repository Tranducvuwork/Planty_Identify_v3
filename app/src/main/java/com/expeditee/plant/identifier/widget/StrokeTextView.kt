package com.expeditee.plant.identifier.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.expeditee.plant.identifier.R

class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeColor: Int = Color.parseColor("#49AF2E")
    private var strokeStartColor: Int = strokeColor
    private var strokeEndColor: Int = strokeColor
    private var strokeWidthPx: Float = 0f
    private var fillColor: Int = currentTextColor
    private var shadowColorValue: Int = Color.parseColor("#8049AF2E")
    private var shadowRadiusPx: Float = 0f
    private var shadowDxPx: Float = 0f
    private var shadowDyPx: Float = 0f

    init {
        context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView).apply {
            strokeColor = getColor(R.styleable.StrokeTextView_strokeColor, strokeColor)
            strokeStartColor = getColor(R.styleable.StrokeTextView_strokeStartColor, strokeColor)
            strokeEndColor = getColor(R.styleable.StrokeTextView_strokeEndColor, strokeColor)
            strokeWidthPx = getDimension(R.styleable.StrokeTextView_strokeWidth, 0f)
            fillColor = getColor(R.styleable.StrokeTextView_fillColor, currentTextColor)
            shadowColorValue = getColor(R.styleable.StrokeTextView_shadowColor, shadowColorValue)
            shadowRadiusPx = getDimension(R.styleable.StrokeTextView_shadowRadius, 0f)
            shadowDxPx = getDimension(R.styleable.StrokeTextView_shadowDx, 0f)
            shadowDyPx = getDimension(R.styleable.StrokeTextView_shadowDy, 0f)
            recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val originalColor = paint.color
        val originalShader = paint.shader
        if (strokeWidthPx > 0f) {
            val originalStyle = paint.style
            val originalStrokeWidth = paint.strokeWidth

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidthPx
            paint.shader = createStrokeShader()
            paint.color = strokeColor
            super.onDraw(canvas)

            paint.style = Paint.Style.FILL
            paint.strokeWidth = originalStrokeWidth
            paint.shader = null
            paint.color = fillColor
            setShadowLayer(shadowRadiusPx, shadowDxPx, shadowDyPx, shadowColorValue)
            super.onDraw(canvas)

            paint.shader = originalShader
            paint.color = originalColor
            paint.style = originalStyle
        } else {
            paint.shader = null
            paint.color = fillColor
            setShadowLayer(shadowRadiusPx, shadowDxPx, shadowDyPx, shadowColorValue)
            super.onDraw(canvas)
            paint.shader = originalShader
            paint.color = originalColor
        }
    }

    private fun createStrokeShader(): Shader? {
        if (height <= 0) return null
        return LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            strokeStartColor,
            strokeEndColor,
            Shader.TileMode.CLAMP
        )
    }
}
