package com.expeditee.plant.identifier.data.dao

import androidx.room.*
import com.expeditee.plant.identifier.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY nextDate ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY nextDate ASC LIMIT 1")
    fun getNextReminder(): Flow<ReminderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderEntity?
}
