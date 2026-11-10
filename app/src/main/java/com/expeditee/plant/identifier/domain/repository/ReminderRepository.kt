package com.expeditee.plant.identifier.domain.repository

import com.expeditee.plant.identifier.model.ReminderScheduleModel
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<ReminderScheduleModel>>
    fun getNextReminder(): Flow<ReminderScheduleModel?>
    suspend fun insertReminder(reminder: ReminderScheduleModel): Long
    suspend fun updateReminder(reminder: ReminderScheduleModel)
    suspend fun deleteReminder(reminder: ReminderScheduleModel)
}
