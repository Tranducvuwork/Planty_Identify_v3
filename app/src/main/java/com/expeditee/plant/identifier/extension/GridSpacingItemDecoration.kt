package com.expeditee.plant.identifier.extension

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        outRect.left = spacing / 2
        outRect.right = spacing / 2
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}