package com.expeditee.plant.identifier.services

import android.net.Uri
import android.util.Log
import com.expeditee.plant.identifier.domain.repository.PlantDiagnoseRepository
import com.expeditee.plant.identifier.domain.repository.PlantIdentifyRepository
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.ProcessingErrorType
import com.expeditee.plant.identifier.model.ScanProcessingResult

class ScanProcessingService(
    @Suppress("unused") private val remoteConfigManagement: RemoteConfigManagement,
    private val identifyRepository: PlantIdentifyRepository,
    private val diagnoseRepository: PlantDiagnoseRepository,
    private val offlineModeService: OfflineModeService,
) {
    companion object {
        private const val TAG = "TAG123123"
    }

    suspend fun process(
        imageUri: Uri,
        cameraMode: CameraMode,
        imageSource: String,
    ): ScanProcessingResult {
        Log.d(
            TAG,
            "process start mode=${cameraMode.typeMode} imageUri=$imageUri imageSource=$imageSource"
        )
        return when (cameraMode.typeMode) {
            CameraMode.TYPE_DIAGNOSE -> {
                Log.d(
                    TAG,
                    "diagnose delegating to repository plantApiBase=${com.expeditee.plant.identifier.common.Constants.PLANT_API_BASE_URL}",
                )
                diagnoseRepository.diagnose(imageUri, imageSource)
            }

            else -> {
                val selectedMode = offlineModeService.scanModeFlow.value
                Log.d(TAG, "identify mode=$selectedMode")
                identifyRepository.identify(imageUri, imageSource, selectedMode)
            }
        }
    }
}
