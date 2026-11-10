package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.expeditee.plant.identifier.R

class RatingStar(val viewGroup: ViewGroup) : View.OnClickListener {
    private val rateBars: MutableList<View> = mutableListOf()
    private var onRateListener: OnRateListener? = null
    private var rateIndex = -1

    override fun onClick(view: View) {
        val id = view.id
        for (i in 0 until rateBars.size) {
            rateBars[i].isSelected = i <= id
        }
        onRateListener?.onRate(id + 1)
        rateIndex = id
    }

    fun setOnRateListener(onRateListenerVar: OnRateListener?) {
        onRateListener = onRateListenerVar
    }

    interface OnRateListener {
        fun onRate(star: Int)
    }

    init {
        for (i in 0 until viewGroup.childCount) {
            val childAt = viewGroup.getChildAt(i)
            if ("rate" == childAt.tag) {
                if (childAt is ImageView) {
                    childAt.setImageResource(R.drawable.bg_star_rate_app)
                }
                childAt.id = rateBars.size
                childAt.isSelected = false
                childAt.setOnClickListener(this)
                rateBars.add(childAt)
            }
        }
    }
}

