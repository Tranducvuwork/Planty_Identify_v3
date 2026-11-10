package com.expeditee.plant.identifier.presenter.processing

import android.app.Application
import androidx.core.net.toUri
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.model.CameraMode
import com.expeditee.plant.identifier.model.DiagnosedDisease
import com.expeditee.plant.identifier.model.ProcessingErrorType
import com.expeditee.plant.identifier.model.ScanProcessingResult
import com.expeditee.plant.identifier.network.PlantNotIdentifiedException
import com.expeditee.plant.identifier.services.ScanProcessingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.domain.repository.ReminderRepository
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ScanProcessingViewModel(
    application: Application,
    private val scanProcessingService: ScanProcessingService,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) :
    BaseViewModel<ScanProcessingState>(application) {
    companion object {
        const val DEFAULT_REMIND_TIME = "09:30"

        internal fun mapResultToState(typeMode: Int, result: ScanProcessingResult): ScanProcessingState {
            return when (result) {
                is ScanProcessingResult.Success -> {
                    val (titleRes, messageRes) = when (typeMode) {
                        CameraMode.TYPE_DIAGNOSE ->
                            R.string.diagnose_complete_title to R.string.diagnose_complete_message
                        else ->
                            R.string.identify_complete_title to R.string.identify_complete_message
                    }
                    ScanProcessingState.Completed(
                        titleRes = titleRes,
                        messageRes = messageRes,
                        highlightName = result.identifiedPlant?.scientificName ?: result.label,
                        confidencePercent = ((result.identifiedPlant?.confidence ?: result.confidence?.toDouble()
                        ?: 0.0) * 100.0).toInt().coerceIn(0, 100),
                        source = result.metadata.mode?.name,
                        resolutionAttemptedAtIdentify = result.metadata.resolutionAttemptedAtIdentify,
                        catalogReadyAtIdentify = result.metadata.catalogReadyAtIdentify,
                        diagnosedDisease = result.diagnosedDisease,
                    )
                }

                is ScanProcessingResult.Error -> {
                    when (result.type) {
                        ProcessingErrorType.NETWORK -> ScanProcessingState.Failed(
                            messageRes = R.string.scan_network_title,
                            subtitleRes = R.string.scan_network_message,
                        )
                        ProcessingErrorType.INVALID_IMAGE -> ScanProcessingState.Failed(R.string.unable_to_load_image_from_gallery)
                        ProcessingErrorType.QUOTA_EXCEEDED -> ScanProcessingState.Failed(R.string.out_of_scans_hint)
                        ProcessingErrorType.SERVER -> ScanProcessingState.Failed(
                            messageRes = R.string.scan_processing_failed,
                            subtitleText = result.userMessage?.takeIf { it.isNotBlank() },
                        )
                        ProcessingErrorType.NOT_IDENTIFIED -> ScanProcessingState.Failed(
                            messageRes = R.string.identify_not_detected_title,
                            subtitleRes = R.string.identify_not_detected_message,
                            subtitleText = result.userMessage
                                ?.takeIf { it.isNotBlank() }
                                ?: (result.throwable as? PlantNotIdentifiedException)
                                    ?.userReason
                                    ?.takeIf { it.isNotBlank() },
                        )
                        ProcessingErrorType.TIMEOUT -> ScanProcessingState.Failed(
                            messageRes = R.string.scan_timeout_title,
                            subtitleRes = R.string.scan_timeout_message,
                        )
                        ProcessingErrorType.UNKNOWN -> ScanProcessingState.Failed(R.string.scan_processing_failed)
                    }
                }
            }
        }
    }


    private val _state = MutableStateFlow<ScanProcessingState>(ScanProcessingState.Loading)
    val state: StateFlow<ScanProcessingState> = _state.asStateFlow()

    override fun onState(state: ScanProcessingState) {
        _state.value = state
    }

    fun startProcessing(imageUri: String, cameraMode: CameraMode, imageSource: String) {
        onState(ScanProcessingState.Loading)
        launchBlock {
            val result = try {
                withTimeout(Constants.SCAN_TIMEOUT_MS) {
                    scanProcessingService.process(
                        imageUri = imageUri.toUri(),
                        cameraMode = cameraMode,
                        imageSource = imageSource,
                    )
                }
            } catch (_: TimeoutCancellationException) {
                ScanProcessingResult.Error(ProcessingErrorType.TIMEOUT)
            }
            val mappedState = mapResultToState(cameraMode.typeMode, result)
            if (mappedState is ScanProcessingState.Completed && result is ScanProcessingResult.Success) {
                val resolvedPlant = result.matchedPlant
                val plantName = resolvedPlant?.name ?: result.identifiedPlant?.commonNames?.firstOrNull() ?: result.label
                
                if (cameraMode.typeMode == CameraMode.TYPE_IDENTIFY) {
                    addDefaultReminder(plantName, imageUri)
                }

                onState(
                    mappedState.copy(
                        resolvedPlantId = resolvedPlant?.id,
                        resolvedPlantName = resolvedPlant?.name,
                        resolvedScientificName = resolvedPlant?.scientificName
                            ?: result.identifiedPlant?.scientificName,
                        resolvedImageUrl = resolvedPlant?.imageUrl,
                            resolvedPreviewDescription = result.identifiedPlant?.description,
                    ),
                )
            } else {
                onState(mappedState)
            }
        }
    }

    private fun addDefaultReminder(plantName: String, imageUri: String) {
        launchBlock(Dispatchers.IO) {
            val remindType = getApplication<Application>().getString(R.string.reminder_watering)
            val repeatType = getApplication<Application>().getString(R.string.reminder_every_day)
            val nextDate = reminderScheduler.calculateNextDate(DEFAULT_REMIND_TIME, repeatType)
            val lastDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val reminder = ReminderScheduleModel(
                plantName = plantName,
                remindType = remindType,
                remindTime = DEFAULT_REMIND_TIME,
                repeatType = repeatType,
                nextDate = nextDate,
                lastDate = lastDate,
                isEnabled = true,
                imagePath = imageUri
            )

            val id = reminderRepository.insertReminder(reminder).toInt()
            reminderScheduler.schedule(reminder.copy(id = id))
        }
    }
}


sealed class ScanProcessingState : BaseViewModel.State {
    data object Loading : ScanProcessingState()
    data class Completed(
        val titleRes: Int,
        val messageRes: Int,
        val highlightName: String? = null,
        val confidencePercent: Int = 0,
        val source: String? = null,
        val resolutionAttemptedAtIdentify: Boolean = false,
        val catalogReadyAtIdentify: Boolean = false,
        val resolvedPlantId: String? = null,
        val resolvedPlantName: String? = null,
        val resolvedScientificName: String? = null,
        val resolvedImageUrl: String? = null,
        val resolvedPreviewDescription: String? = null,
        val diagnosedDisease: DiagnosedDisease? = null,
    ) : ScanProcessingState()
    data class Failed(
        val messageRes: Int,
        val subtitleRes: Int = messageRes,
        val subtitleText: String? = null,
        val isDiagnose: Boolean = false,
        val isHealthy: Boolean = false,
    ) : ScanProcessingState()
}
