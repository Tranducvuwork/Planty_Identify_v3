package com.expeditee.plant.identifier.widget.bottomsheet

import android.text.Editable
import android.text.TextWatcher
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.DialogCreateSiteBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class CreateSiteBottomSheetDialogFragment(
    private val onCreated: (String) -> Unit
) : BaseBottomSheetDialogFragment<DialogCreateSiteBinding>(DialogCreateSiteBinding::inflate) {

    override val TAG: String = "CreateSiteBottomSheetDialogFragment"

    override fun initViews() {
        updateCreateState()
    }

    override fun initObservers() = Unit

    override fun initListeners() {
        with(viewBinding) {
            etSiteName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    updateCreateState()
                }
            })
            btnCancel.onDebounceClick { dismissAllowingStateLoss() }
            btnCreate.onDebounceClick {
                val siteName = etSiteName.text?.toString()?.trim().orEmpty()
                if (siteName.isBlank()) return@onDebounceClick
                dismissAllowingStateLoss()
                onCreated(siteName)
            }
        }
    }

    private fun updateCreateState() {
        val enabled = viewBinding.etSiteName.text?.toString()?.trim()?.isNotEmpty() == true
        viewBinding.btnCreate.isEnabled = enabled
        viewBinding.btnCreate.setBackgroundColor(
            viewBinding.root.context.getColor(
                if (enabled) R.color.result_green else R.color.result_button_disabled
            )
        )
    }
}