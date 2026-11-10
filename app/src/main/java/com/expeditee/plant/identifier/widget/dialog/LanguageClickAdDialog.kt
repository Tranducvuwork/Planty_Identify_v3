package com.expeditee.plant.identifier.widget.dialog

import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogLanguageClickAdBinding
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.makeGone
import com.expeditee.plant.identifier.extension.onDebounceClick
import androidx.core.graphics.drawable.toDrawable

class LanguageClickAdDialog(
    context: Context,
    private val selectedLanguageName: String,
    private val onApplyClick: () -> Unit,
    private val onDismissed: (wasApplied: Boolean) -> Unit,
) : BaseDialog<DialogLanguageClickAdBinding>(context) {

    private var wasApplied = false

    override fun getLayoutDialog(): Int = R.layout.dialog_language_click_ad

    override fun initViews() {
        binding.tvMessage.text = context.getString(
            R.string.change_language_message,
            selectedLanguageName
        )

        setOnDismissListener { onDismissed(wasApplied) }
    }

    override fun onClickViews() {
        binding.ivClose.onDebounceClick {
            dismiss()
        }
        binding.btnApplyLanguage.onDebounceClick {
            wasApplied = true
            dismiss()
            onApplyClick()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialogWidth = (context.resources.displayMetrics.widthPixels * 0.86).toInt()
        window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                dialogWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    fun populateNativeAd(nativeAd: ApNativeAd?) {
        if (nativeAd == null) {
            binding.nativeLanguageClickView.removeAllViews()
            binding.nativeLanguageClickView.makeGone()
            return
        }
        binding.nativeLanguageClickView.beVisible()
        binding.nativeLanguageClickView.populateUnifiedView(
            nativeAd,
            com.dqh.lmtech.ads.R.layout.ads_native_medium,
        )
    }

    fun isAdShown(): Boolean {
        return binding.nativeLanguageClickView.populated
    }
}
