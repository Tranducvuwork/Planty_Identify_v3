package com.expeditee.plant.identifier.widget.popup

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.AbstractView
import com.expeditee.plant.identifier.databinding.MyGardentPopUpBinding
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.model.MyPlantModel

class MyGardenPopUp(context: Context, attrs: AttributeSet?) : AbstractView(context, attrs) {

    interface OnItemClickListener {
        fun onMove(item: MyPlantModel)
        fun onDelete(item: MyPlantModel)
    }

    private var currentItem: MyPlantModel? = null
    var listener: OnItemClickListener? = null

    override fun layoutId() = R.layout.my_gardent_pop_up

    override fun viewBinding() = binding as MyGardentPopUpBinding

    override fun viewInitialized() {
        viewBinding().apply {
            llMove.onDebounceClick {
                currentItem?.let { listener?.onMove(it) }
            }
            llDelete.onDebounceClick {
                currentItem?.let { listener?.onDelete(it) }
            }
        }
    }

    fun setItem(item: MyPlantModel) {
        currentItem = item
    }

    companion object {
        fun show(
            context: Context,
            anchor: View,
            item: MyPlantModel,
            callback: (MenuAction, MyPlantModel) -> Unit
        ) {
            val popupView = MyGardenPopUp(context, null)
            popupView.setItem(item)

            val popup = PopupWindow(
                popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                isFocusable = true
                elevation = 8f
            }

            popupView.listener = object : OnItemClickListener {
                override fun onMove(item: MyPlantModel) {
                    callback(MenuAction.MOVE, item)
                    popup.dismiss()
                }

                override fun onDelete(item: MyPlantModel) {
                    callback(MenuAction.DELETE, item)
                    popup.dismiss()
                }
            }

            popupView.measure(
                MeasureSpec.UNSPECIFIED,
                MeasureSpec.UNSPECIFIED
            )

            val offsetDp = 17
            val offsetPx = (offsetDp * context.resources.displayMetrics.density).toInt()

            val visibleFrame = Rect()
            anchor.getWindowVisibleDisplayFrame(visibleFrame)
            val anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)
            val anchorTop = anchorLocation[1]
            val anchorBottom = anchorTop + anchor.height

            val recyclerBottom = findParentRecyclerView(anchor)?.let { rv ->
                val location = IntArray(2)
                rv.getLocationOnScreen(location)
                location[1] + rv.height
            } ?: visibleFrame.bottom

            val effectiveBottom = minOf(visibleFrame.bottom, recyclerBottom)
            val spaceBelow = effectiveBottom - anchorBottom
            val spaceAbove = anchorTop - visibleFrame.top

            val shouldShowAbove = spaceBelow < popupView.measuredHeight && spaceAbove > spaceBelow
            val yOffset = if (shouldShowAbove) {
                -(anchor.height + popupView.measuredHeight)
            } else {
                0
            }

            popup.showAsDropDown(
                anchor,
                -popupView.measuredWidth + anchor.width - offsetPx,
                yOffset
            )
        }

        private fun findParentRecyclerView(anchor: View): RecyclerView? {
            var parent: ViewParent? = anchor.parent
            while (parent != null) {
                if (parent is RecyclerView) return parent
                parent = parent.parent
            }
            return null
        }
    }


    enum class MenuAction {
        MOVE, DELETE
    }
}