package com.expeditee.plant.identifier.presenter.reminder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.ReminderRepository
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    application: Application
) : BaseViewModel<ReminderState>(application) {
    companion object {
        const val DEFAULT_TIME = "09:30"
    }

    private val _reminders = MutableStateFlow<List<ReminderScheduleModel>>(emptyList())
    val reminders = _reminders.asStateFlow()

    val isEmpty = reminders
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    private val _nextReminder = MutableStateFlow<ReminderScheduleModel?>(null)
    val nextReminder = _nextReminder.asStateFlow()

    private val _selectedPlantId = MutableStateFlow(0)
    val selectedPlantId = _selectedPlantId.asStateFlow()

    private val _selectedPlantName = MutableStateFlow("")
    val selectedPlantName = _selectedPlantName.asStateFlow()

    private val _remindType = MutableStateFlow("")
    val remindType = _remindType.asStateFlow()

    private val _repeatType = MutableStateFlow("")
    val repeatType = _repeatType.asStateFlow()

    private val _remindTime = MutableStateFlow(DEFAULT_TIME)
    val remindTime = _remindTime.asStateFlow()

    private val _lastDate = MutableStateFlow("")
    val lastDate = _lastDate.asStateFlow()

    val isDataValid: Boolean
        get() = _selectedPlantName.value.isNotBlank() &&
                _repeatType.value.isNotBlank() &&
                _remindType.value.isNotBlank() &&
                _remindTime.value.matches(Regex("^\\d{2}:\\d{2}$"))

    init {
        fetchReminders()
        fetchNextReminder()
    }

    private fun fetchReminders() {
        launchBlock(Dispatchers.IO) {
            repository.getAllReminders().collectLatest {
                _reminders.value = it
            }
        }
    }

    private fun fetchNextReminder() {
        launchBlock(Dispatchers.IO) {
            repository.getNextReminder().collectLatest {
                _nextReminder.value = it
            }
        }
    }

    private fun saveReminder(reminder: ReminderScheduleModel) {
        launchBlock(Dispatchers.IO) {
            val id = repository.insertReminder(reminder).toInt()
            val updatedReminder = reminder.copy(id = id)
            if (updatedReminder.isEnabled) {
                scheduler.schedule(updatedReminder)
            } else {
                scheduler.cancel(updatedReminder.id)
            }
        }
    }

    private fun updateReminder(reminder: ReminderScheduleModel) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            if (reminder.isEnabled) {
                scheduler.schedule(reminder)
            } else {
                scheduler.cancel(reminder.id)
            }
        }
    }

    private fun deleteReminder(reminder: ReminderScheduleModel) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            scheduler.cancel(reminder.id)
        }
    }

    private fun calculateNextDate(remindTime: String, repeatType: String): Long {
        return scheduler.calculateNextDate(remindTime, repeatType)
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun initReminderEditor(reminder: ReminderScheduleModel?) {
        _selectedPlantId.value = reminder?.plantId ?: 0
        _selectedPlantName.value = reminder?.plantName.orEmpty()
        _remindType.value = reminder?.remindType.takeUnless { it.isNullOrBlank() }
            ?: getApplication<Application>().getString(R.string.reminder_watering)
        _repeatType.value = reminder?.repeatType.takeUnless { it.isNullOrBlank() }
            ?: getApplication<Application>().getString(R.string.reminder_every_day)
        _remindTime.value = reminder?.remindTime.takeUnless { it.isNullOrBlank() } ?: DEFAULT_TIME
        _lastDate.value = reminder?.lastDate?.takeIf { it.isNotBlank() } ?: getTodayDate()
    }

    private fun selectPlant(plantId: Int, plantName: String) {
        _selectedPlantId.value = plantId
        _selectedPlantName.value = plantName
    }

    private fun setRemindType(type: String) {
        _remindType.value = type
    }

    private fun setRepeatType(type: String) {
        _repeatType.value = type
    }

    private fun setRemindTime(time: String) {
        _remindTime.value = time
    }

    private fun setLastDate(value: String) {
        _lastDate.value = value
    }

    private fun updateEditorFields(
        plantId: Int,
        plantName: String,
        remindType: String,
        repeatType: String,
        remindTime: String,
        lastDate: String
    ) {
        _selectedPlantId.value = plantId
        _selectedPlantName.value = plantName
        _remindType.value = remindType
        _repeatType.value = repeatType
        _remindTime.value = remindTime
        _lastDate.value = lastDate
    }

    private fun buildReminderForSave(baseReminder: ReminderScheduleModel?): ReminderScheduleModel {
        val nextDate = calculateNextDate(
            remindTime = _remindTime.value,
            repeatType = _repeatType.value
        )
        val lastDate = _lastDate.value.ifBlank { getTodayDate() }

        return (baseReminder ?: ReminderScheduleModel()).copy(
            plantId = _selectedPlantId.value,
            plantName = _selectedPlantName.value,
            remindType = _remindType.value,
            remindTime = _remindTime.value,
            repeatType = _repeatType.value,
            nextDate = nextDate,
            lastDate = lastDate,
            isEnabled = true
        )
    }


    override fun onState(state: ReminderState) {
        when (state) {
            is ReminderState.SaveReminder -> saveReminder(state.reminder)
            is ReminderState.UpdateReminder -> updateReminder(state.reminder)
            is ReminderState.DeleteReminder -> deleteReminder(state.reminder)
            is ReminderState.InitReminderEditor -> initReminderEditor(state.reminder)
            is ReminderState.SelectPlant -> selectPlant(state.plantId, state.plantName)
            is ReminderState.SetRemindType -> setRemindType(state.type)
            is ReminderState.SetRepeatType -> setRepeatType(state.type)
            is ReminderState.SetRemindTime -> setRemindTime(state.time)
            is ReminderState.SetLastDate -> setLastDate(state.value)
            is ReminderState.UpdateEditorFields -> updateEditorFields(
                state.plantId,
                state.plantName,
                state.remindType,
                state.repeatType,
                state.remindTime,
                state.lastDate
            )

            is ReminderState.SaveCurrentEditor -> {
                val payload = buildReminderForSave(state.baseReminder)
                if (payload.id == 0) {
                    saveReminder(payload)
                } else {
                    updateReminder(payload)
                }
            }

            is ReminderState.CompleteReminderAction -> {
                val reminder = state.reminder
                val updatedReminder = reminder.copy(
                    previousDate = System.currentTimeMillis(),
                    nextDate = calculateNextDate(reminder.remindTime, reminder.repeatType)
                )
                saveReminder(updatedReminder)
            }
        }
    }
}

sealed class ReminderState : BaseViewModel.State {
    data class SaveReminder(val reminder: ReminderScheduleModel) : ReminderState()
    data class UpdateReminder(val reminder: ReminderScheduleModel) : ReminderState()
    data class DeleteReminder(val reminder: ReminderScheduleModel) : ReminderState()
    data class InitReminderEditor(val reminder: ReminderScheduleModel?) : ReminderState()
    data class SelectPlant(val plantId: Int, val plantName: String) : ReminderState()
    data class SetRemindType(val type: String) : ReminderState()
    data class SetRepeatType(val type: String) : ReminderState()
    data class SetRemindTime(val time: String) : ReminderState()
    data class SetLastDate(val value: String) : ReminderState()
    data class UpdateEditorFields(
        val plantId: Int,
        val plantName: String,
        val remindType: String,
        val repeatType: String,
        val remindTime: String,
        val lastDate: String
    ) : ReminderState()

    data class SaveCurrentEditor(val baseReminder: ReminderScheduleModel?) : ReminderState()
    data class CompleteReminderAction(val reminder: ReminderScheduleModel) : ReminderState()
}
