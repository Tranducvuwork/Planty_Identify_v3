package com.expeditee.plant.identifier.presenter.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

class FocusCircleView(
    context: Context,
    attributeSet: AttributeSet,
) : View(context, attributeSet) {
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var storedFocus: RectF? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideFocusRunnable: Runnable? = null

    var focusCircle: RectF?
        get() = storedFocus
        set(value) {
            storedFocus = value
            if (value != null) scheduleHide()
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        storedFocus?.let { rect ->
            val radius = rect.width() / 1.2f
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
        }
    }

    private fun scheduleHide() {
        hideFocusRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable {
            storedFocus = null
            invalidate()
        }
        hideFocusRunnable = r
        mainHandler.postDelayed(r, 1000)
    }

    override fun onDetachedFromWindow() {
        hideFocusRunnable?.let { mainHandler.removeCallbacks(it) }
        hideFocusRunnable = null
        super.onDetachedFromWindow()
    }
}
