package com.expeditee.plant.identifier.widget.bottomsheet


import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.DialogSelectPlantBinding
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.helper.SiteManager

class SelectPlantBottomSheetDialog : BaseBottomSheetDialogFragment<DialogSelectPlantBinding>(
    DialogSelectPlantBinding::inflate
) {
    override val TAG: String = "SelectPlantBottomSheetDialog"
    private var gardenId: String = SiteManager.DEFAULT_SITE_ID
    private var onIdentifyByPhotoClicked: ((String) -> Unit)? = null
    private var onSearchByNameClicked: ((String) -> Unit)? = null

    override fun initViews() {
    }

    override fun initObservers() {
    }

    override fun initListeners() {
        viewBinding.llIdentify.onDebounceClick {
            onIdentifyByPhotoClicked?.invoke(gardenId)
            dismiss()
        }

        viewBinding.llSearch.onDebounceClick {
            onSearchByNameClicked?.invoke(gardenId)
            dismiss()
        }
    }


    fun setOnIdentifyByPhotoListener(listener: (String) -> Unit) = apply {
        this.onIdentifyByPhotoClicked = listener
    }

    fun setOnSearchByNameListener(listener: (String) -> Unit) = apply {
        this.onSearchByNameClicked = listener
    }

    companion object {
        fun newInstance(gardenId: String = SiteManager.DEFAULT_SITE_ID): SelectPlantBottomSheetDialog {
            return SelectPlantBottomSheetDialog().apply {
                this.gardenId = gardenId
            }
        }
    }
}