package com.expeditee.plant.identifier.domain.repository

import android.net.Uri
import com.expeditee.plant.identifier.model.ScanProcessingResult

interface PlantDiagnoseRepository {
    suspend fun diagnose(
        imageUri: Uri,
        imageSource: String,
    ): ScanProcessingResult
}
