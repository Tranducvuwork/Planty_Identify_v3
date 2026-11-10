package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.content.Context
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogThankYouBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThankYouDialog(
    context: Context,
) : BaseDialog<DialogThankYouBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_thank_you

    private var job: Job? = null

    init {
        setOnDismissListener {
            try {
                job?.cancel()
                job = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val params = window?.attributes
        params?.width = (context.resources.displayMetrics.widthPixels * 0.85f).toInt()
        window?.attributes = params
    }

    override fun initViews() {
        super.initViews()
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            delay(2000L)
            withContext(Dispatchers.Main.immediate) {
                dismiss()
            }
        }
    }

    override fun onClickViews() {
        super.onClickViews()
        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }
}

