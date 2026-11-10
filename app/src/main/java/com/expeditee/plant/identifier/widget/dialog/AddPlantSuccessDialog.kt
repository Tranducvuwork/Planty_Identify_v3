package com.expeditee.plant.identifier.widget.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogAddPlantSuccessBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class AddPlantSuccessDialog(
    context: Context,
    private val iconDrawableRes: Int = R.drawable.bg_tree_notification_old,
    private val onDismissListener: (() -> Unit)? = null
) : BaseDialog<DialogAddPlantSuccessBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_add_plant_success

    override fun onStart() {
        super.onStart()
        binding.ivSuccessIcon.setImageResource(iconDrawableRes)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val dialogWidth = (screenWidth * width()).toInt()

            setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            attributes = attributes?.apply {
                dimAmount = dimAmount()
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
        }
    }

    override fun onClickViews() {
        binding.btnGotIt.onDebounceClick {
            onDismissListener?.invoke()
            dismiss()
        }
    }

    open fun dimAmount(): Float = 0.6f
    open fun width(): Float = 0.9f

    companion object {
        fun createForExplore(
            context: Context,
            onDismiss: (() -> Unit)? = null
        ): AddPlantSuccessDialog {
            return AddPlantSuccessDialog(
                context,
                iconDrawableRes = R.drawable.bg_tree_hapyy,
                onDismissListener = onDismiss
            )
        }
    }
}
