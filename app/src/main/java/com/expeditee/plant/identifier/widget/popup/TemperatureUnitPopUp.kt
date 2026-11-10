package com.expeditee.plant.identifier.widget.popup

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.AbstractView
import com.expeditee.plant.identifier.databinding.TemperatureUnitPopUpBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.onDebounceClick

class TemperatureUnitPopUp(context: Context, attrs: AttributeSet?) : AbstractView(context, attrs) {

    private var currentUnit: String = UNIT_CELSIUS
    private var listener: ((String) -> Unit)? = null

    override fun layoutId() = R.layout.temperature_unit_pop_up

    override fun viewBinding() = binding as TemperatureUnitPopUpBinding

    override fun viewInitialized() {
        viewBinding().apply {
            llCelsius.onClickWithScale {
                listener?.invoke(UNIT_CELSIUS)
            }
            llFahrenheit.onClickWithScale {
                listener?.invoke(UNIT_FAHRENHEIT)
            }
        }
    }

    private fun bind(unit: String) {
        currentUnit = if (unit.equals(UNIT_FAHRENHEIT, true)) UNIT_FAHRENHEIT else UNIT_CELSIUS
        val isCelsiusSelected = currentUnit == UNIT_CELSIUS
        viewBinding().apply {
            ivCelsius.setImageResource(if (isCelsiusSelected) R.drawable.ic_radio_checked_green else R.drawable.ic_radio_unchecked_gray)
            ivFahrenheit.setImageResource(if (isCelsiusSelected) R.drawable.ic_radio_unchecked_gray else R.drawable.ic_radio_checked_green)
            tvCelsius.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isCelsiusSelected) R.color.primary_green else R.color.gray_700
                )
            )
            tvFahrenheit.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isCelsiusSelected) R.color.gray_700 else R.color.primary_green
                )
            )
        }
    }

    companion object {
        private const val UNIT_CELSIUS = "celsius"
        private const val UNIT_FAHRENHEIT = "fahrenheit"

        fun show(
            context: Context,
            anchor: View,
            currentUnit: String,
            onUnitSelected: (String) -> Unit,
        ) {
            val popupView = TemperatureUnitPopUp(context, null).apply {
                bind(currentUnit)
            }

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

            popupView.listener = { selectedUnit ->
                onUnitSelected(selectedUnit)
                popup.dismiss()
            }

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val visibleFrame = Rect()
            anchor.getWindowVisibleDisplayFrame(visibleFrame)

            val anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)
            val anchorBottom = anchorLocation[1] + anchor.height
            val spaceBelow = visibleFrame.bottom - anchorBottom
            val shouldShowAbove = spaceBelow < popupView.measuredHeight
            val yOffset = if (shouldShowAbove) -(anchor.height + popupView.measuredHeight) else 0

            popup.showAsDropDown(
                anchor,
                anchor.width - popupView.measuredWidth,
                yOffset
            )
        }
    }
}
