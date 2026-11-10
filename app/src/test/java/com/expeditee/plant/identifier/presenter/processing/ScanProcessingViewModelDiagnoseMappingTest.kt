package com.expeditee.plant.identifier.presenter.processing

import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.DiagnosedDisease
import com.expeditee.plant.identifier.model.IdentifyMetadata
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.model.ProcessingErrorType
import com.expeditee.plant.identifier.model.ScanProcessingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanProcessingViewModelDiagnoseMappingTest {

    @Test
    fun `diagnose healthy success maps to completed with zero confidence`() {
        val result = ScanProcessingResult.Success(
            label = "Healthy",
            confidence = 0.0f,
            metadata = IdentifyMetadata(mode = ModeScan.ONLINE),
            diagnosedDisease = DiagnosedDisease(
                isHealthy = true,
                disease = null,
                probability = 0.0,
                isPlant = true,
            ),
        )

        val state = ScanProcessingViewModel.mapResultToState(CameraMode.TYPE_DIAGNOSE, result)

        assertTrue(state is ScanProcessingState.Completed)
        state as ScanProcessingState.Completed
        assertEquals(R.string.diagnose_complete_title, state.titleRes)
        assertEquals(0, state.confidencePercent)
        assertTrue(state.diagnosedDisease?.isHealthy == true)
    }

    @Test
    fun `diagnose not plant success stays as completed result`() {
        val result = ScanProcessingResult.Success(
            label = "Healthy",
            confidence = 0.0f,
            metadata = IdentifyMetadata(mode = ModeScan.ONLINE),
            diagnosedDisease = DiagnosedDisease(
                isHealthy = false,
                disease = null,
                probability = 0.0,
                isPlant = false,
            ),
        )

        val state = ScanProcessingViewModel.mapResultToState(CameraMode.TYPE_DIAGNOSE, result)

        assertTrue(state is ScanProcessingState.Completed)
        state as ScanProcessingState.Completed
        assertTrue(state.diagnosedDisease?.isPlant == false)
        assertEquals(0, state.confidencePercent)
    }

    @Test
    fun `diagnose server error maps to shared processing failure`() {
        val state = ScanProcessingViewModel.mapResultToState(
            CameraMode.TYPE_DIAGNOSE,
            ScanProcessingResult.Error(ProcessingErrorType.SERVER),
        )

        assertTrue(state is ScanProcessingState.Failed)
        state as ScanProcessingState.Failed
        @Test
        fun `scan timeout maps to check network or not found`() {
            val state = ScanProcessingViewModel.mapResultToState(
                CameraMode.TYPE_IDENTIFY,
                ScanProcessingResult.Error(ProcessingErrorType.TIMEOUT),
            )

            assertTrue(state is ScanProcessingState.Failed)
            state as ScanProcessingState.Failed
            assertEquals(R.string.scan_timeout_title, state.messageRes)
            assertEquals(R.string.scan_timeout_message, state.subtitleRes)
        }
    }
}