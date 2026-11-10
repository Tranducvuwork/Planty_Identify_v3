package com.expeditee.plant.identifier.data.repository

import com.expeditee.plant.identifier.data.dao.ReminderDao
import com.expeditee.plant.identifier.data.mapper.ReminderMapper
import com.expeditee.plant.identifier.domain.repository.ReminderRepository
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val dao: ReminderDao,
    private val mapper: ReminderMapper
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<ReminderScheduleModel>> =
        dao.getAllReminders().map { list -> mapper.mapToDomainList(list) }

    override fun getNextReminder(): Flow<ReminderScheduleModel?> =
        dao.getNextReminder().map { it?.let { mapper.mapToDomain(it) } }

    override suspend fun insertReminder(reminder: ReminderScheduleModel): Long =
        dao.insertReminder(mapper.mapToEntity(reminder))

    override suspend fun updateReminder(reminder: ReminderScheduleModel) =
        dao.updateReminder(mapper.mapToEntity(reminder))

    override suspend fun deleteReminder(reminder: ReminderScheduleModel) =
        dao.deleteReminder(mapper.mapToEntity(reminder))
}
