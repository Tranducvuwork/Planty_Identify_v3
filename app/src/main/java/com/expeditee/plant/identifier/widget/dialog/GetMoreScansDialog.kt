package com.expeditee.plant.identifier.widget.dialog

import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogGetMoreScansBinding
import androidx.core.graphics.drawable.toDrawable

class GetMoreScansDialog(context: Context) : BaseDialog<DialogGetMoreScansBinding>(context) {
    open fun dimAmount(): Float = 0.6f
    open fun width(): Float = 0.9f
    interface IListener {
        fun onWatchAdClick()
        fun onPremiumClick()
    }

    var listener: IListener? = null

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val dialogWidth = (screenWidth * width()).toInt()

            setLayout(
                dialogWidth,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            attributes?.let { windowParams ->
                windowParams.dimAmount = dimAmount()
                windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                attributes = windowParams
            }
        }
    }

    override fun getLayoutDialog() = R.layout.dialog_get_more_scans

    override fun initViews() {
        binding.btnWatchAd.setOnClickListener {
            listener?.onWatchAdClick()
            dismiss()
        }
        binding.btnPremium.setOnClickListener {
            listener?.onPremiumClick()
            dismiss()
        }
    }
}
