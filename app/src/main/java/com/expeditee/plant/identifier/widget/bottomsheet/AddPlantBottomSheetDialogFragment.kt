package com.expeditee.plant.identifier.widget.bottomsheet

import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.DialogAddPlantBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class AddPlantBottomSheetDialogFragment(
    private val onCreateSiteClicked: () -> Unit
) : BaseBottomSheetDialogFragment<DialogAddPlantBinding>(DialogAddPlantBinding::inflate) {

    override val TAG: String = "AddPlantBottomSheetDialogFragment"

    private var selectedAll = true

    override fun initViews() {
        updateSelection()
    }

    override fun initObservers() = Unit

    override fun initListeners() {
        with(viewBinding) {
            optionAll.onDebounceClick {
                selectedAll = true
                updateSelection()
            }
            optionMyGarden.onDebounceClick {
                selectedAll = false
                updateSelection()
            }
            optionCreateSite.onDebounceClick {
                dismissAllowingStateLoss()
                onCreateSiteClicked()
            }
            btnAddPlant.onDebounceClick {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun updateSelection() {
        viewBinding.ivAllChecked.setImageResource(
            if (selectedAll) R.drawable.ic_radio_checked_green
            else R.drawable.ic_radio_unchecked_gray
        )
        viewBinding.ivMyGardenChecked.setImageResource(
            if (selectedAll) R.drawable.ic_radio_unchecked_gray
            else R.drawable.ic_radio_checked_green
        )
    }
}