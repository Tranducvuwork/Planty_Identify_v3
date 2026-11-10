package com.expeditee.plant.identifier.presenter.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class CenterLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context) : super(context)
    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout,
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes,
    )

    private lateinit var host: RecyclerView

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (childCount == 0 && state.itemCount > 0) {
            val firstChild = recycler.getViewForPosition(0)
            measureChildWithMargins(firstChild, 0, 0)
            recycler.recycleView(firstChild)
        }
        super.onLayoutChildren(recycler, state)
    }

    override fun measureChildWithMargins(child: View, widthUsed: Int, heightUsed: Int) {
        val position = (child.layoutParams as RecyclerView.LayoutParams).bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) {
            super.measureChildWithMargins(child, widthUsed, heightUsed)
            return
        }
        super.measureChildWithMargins(child, widthUsed, heightUsed)
        if (position != 0 && position != itemCount - 1) return
        when (orientation) {
            RecyclerView.HORIZONTAL -> {
                val hPadding = ((width - child.measuredWidth) / 2).coerceAtLeast(0)
                if (!reverseLayout) {
                    if (position == 0) {
                        host.updatePaddingRelative(
                            start = (hPadding - child.measuredWidth / 2.5).toInt(),
                            end = (hPadding - child.measuredWidth / 2.5).toInt(),
                        )
                    }
                    if (position == itemCount - 1) {
                        host.updatePaddingRelative(end = hPadding, start = hPadding)
                    }
                } else {
                    if (position == 0) {
                        host.updatePaddingRelative(
                            end = (hPadding - child.measuredWidth / 2.5).toInt(),
                            start = (hPadding - child.measuredWidth / 2.5).toInt(),
                        )
                    }
                    if (position == itemCount - 1) {
                        host.updatePaddingRelative(start = hPadding, end = hPadding)
                    }
                }
            }

            RecyclerView.VERTICAL -> {
                val vPadding = ((height - child.measuredHeight) / 2).coerceAtLeast(0)
                if (!reverseLayout) {
                    if (position == 0) host.updatePaddingRelative(top = vPadding)
                    if (position == itemCount - 1) host.updatePaddingRelative(bottom = vPadding)
                } else {
                    if (position == 0) host.updatePaddingRelative(bottom = vPadding)
                    if (position == itemCount - 1) host.updatePaddingRelative(top = vPadding)
                }
            }
        }
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        host = view
        super.onAttachedToWindow(view)
    }
}
