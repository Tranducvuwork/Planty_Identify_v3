package com.expeditee.plant.identifier.model

import androidx.annotation.Keep
import com.expeditee.plant.identifier.R
import java.io.Serializable

@Keep
data class CameraMode(
    val typeMode: Int,
    val imgMode: Int,
    val isSelected: Boolean = false,
) : Serializable {

    companion object {
        const val TYPE_IDENTIFY = 0
        const val TYPE_DIAGNOSE = 1

        val DIAGNOSE = CameraMode(
            typeMode = TYPE_DIAGNOSE,
            imgMode = R.drawable.ic_diagnose_camera,
            isSelected = true,
        )

        val IDENTIFY = CameraMode(
            typeMode = TYPE_IDENTIFY,
            imgMode = R.drawable.ic_capture_mode,
            isSelected = false,
        )

        val ALL_MODES = listOf(DIAGNOSE, IDENTIFY)
    }
}
