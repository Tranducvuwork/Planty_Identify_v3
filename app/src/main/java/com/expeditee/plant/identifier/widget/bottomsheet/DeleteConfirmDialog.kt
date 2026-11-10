package com.expeditee.plant.identifier.widget.bottomsheet


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogDeleteConfirmBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class DeleteConfirmDialog(
    context: Context, private val onDeleteConfirmed: () -> Unit
) : BaseDialog<DialogDeleteConfirmBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_delete_confirm

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
        binding.btnCancel.onDebounceClick { dismiss() }
        binding.btnDelete.onDebounceClick {
            dismiss()
            onDeleteConfirmed()
        }
    }
}