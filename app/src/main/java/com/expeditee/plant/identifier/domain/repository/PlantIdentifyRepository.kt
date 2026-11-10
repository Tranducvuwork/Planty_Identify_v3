package com.expeditee.plant.identifier.domain.repository

import android.net.Uri
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.model.ScanProcessingResult


interface PlantIdentifyRepository {
    
    suspend fun identify(
        imageUri: Uri,
        imageSource: String,
        requestedMode: ModeScan,
    ): ScanProcessingResult
}
