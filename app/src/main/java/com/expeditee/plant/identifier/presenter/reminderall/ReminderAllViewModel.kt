package com.expeditee.plant.identifier.presenter.reminderall

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.domain.repository.ReminderRepository
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReminderAllViewModel(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    application: Application
) : BaseViewModel<ReminderAllState>(application) {

    private val _reminders = MutableStateFlow<List<ReminderScheduleModel>>(emptyList())
    private val _searchResults = MutableStateFlow<List<ReminderScheduleModel>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery = _currentQuery.asStateFlow()

    init {
        fetchReminders()
    }

    private fun fetchReminders() {
        launchBlock(Dispatchers.IO) {
            repository.getAllReminders().collectLatest {
                _reminders.value = it
                filterReminders(_currentQuery.value)
            }
        }
    }

    private fun filterReminders(query: String) {
        _currentQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = _reminders.value
        } else {
            _searchResults.value = _reminders.value.filter {
                it.plantName.contains(query, ignoreCase = true) ||
                        it.remindType.contains(query, ignoreCase = true)
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

    override fun onState(state: ReminderAllState) {
        when (state) {
            is ReminderAllState.Search -> filterReminders(state.query)
            ReminderAllState.ClearSearch -> filterReminders("")
            is ReminderAllState.UpdateReminder -> updateReminder(state.reminder)
            is ReminderAllState.DeleteReminder -> deleteReminder(state.reminder)
            is ReminderAllState.CompleteReminderAction -> {
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

sealed class ReminderAllState : BaseViewModel.State {
    data class Search(val query: String) : ReminderAllState()
    object ClearSearch : ReminderAllState()
    data class UpdateReminder(val reminder: ReminderScheduleModel) : ReminderAllState()
    data class DeleteReminder(val reminder: ReminderScheduleModel) : ReminderAllState()
    data class CompleteReminderAction(val reminder: ReminderScheduleModel) : ReminderAllState()
}
