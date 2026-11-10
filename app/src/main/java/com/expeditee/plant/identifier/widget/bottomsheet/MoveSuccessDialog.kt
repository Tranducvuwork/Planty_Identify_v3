package com.expeditee.plant.identifier.widget.bottomsheet


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogMoveSuccessBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class MoveSuccessDialog(
    context: Context
) : BaseDialog<DialogMoveSuccessBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_move_success

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val screenWidth = context.resources.displayMetrics.widthPixels
            setLayout((screenWidth * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            attributes?.let {
                it.dimAmount = 0.6f
                it.flags = it.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                attributes = it
            }
        }
    }

    override fun onClickViews() {
        binding.btnGotIt.onDebounceClick { dismiss() }
    }
}