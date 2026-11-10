package com.expeditee.plant.identifier.widget.dialog

import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialogFragment
import com.expeditee.plant.identifier.databinding.DialogLightMeterTipsBinding
import com.expeditee.plant.identifier.extension.loadSpan
import com.expeditee.plant.identifier.extension.onClickWithScale

class LightMeterTipsDialog :
    BaseDialogFragment<DialogLightMeterTipsBinding>(DialogLightMeterTipsBinding::inflate) {

    override val TAG: String = "LightMeterTipsDialog"

    override fun initViews() = Unit

    override fun initListeners() {
        with(viewBinding) {
            textLightMeterGuideDescription1.loadSpan {
                appendText("• ")
                appendText(getString(R.string.hold_your_phone_next_to_the_plant_and_point_the_front_camera_toward_the_light) + " ")
            }
            textLightMeterGuideDescription2.loadSpan {
                appendText("• ")
                appendText(getString(R.string.second) + " ")
            }
        }
        viewBinding.btnGotIt.onClickWithScale {
            dismissAllowingStateLoss()
        }
    }
}