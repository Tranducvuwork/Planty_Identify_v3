package com.expeditee.plant.identifier.presenter.lightmeter

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.expeditee.plant.identifier.R

class LightMeterBarView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {

    private var ratio: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }
    private lateinit var line: View

    init {
        LayoutInflater.from(ctx).inflate(R.layout.layout_light_meter_bar, this, true)
        initView(ctx, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        line = findViewById(R.id.view_light_meter_bar_line)

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.LightMeterBarView)
            ratio = ta.getFloat(R.styleable.LightMeterBarView_ratio, 0f)
            ta.recycle()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updatePosition()
    }


    fun updateRatio(ratio: Float) {
        this.ratio = ratio
        updatePosition()
    }

    private fun updatePosition() {
        line.updateLayoutParams<ConstraintLayout.LayoutParams> {
            horizontalBias = ratio
        }
    }
}