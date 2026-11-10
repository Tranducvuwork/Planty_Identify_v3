package com.expeditee.plant.identifier.data


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expeditee.plant.identifier.data.converter.StringListConverter
import com.expeditee.plant.identifier.data.dao.MyPlantDao
import com.expeditee.plant.identifier.data.dao.PopularPlantDao
import com.expeditee.plant.identifier.data.dao.ReminderDao
import com.expeditee.plant.identifier.data.entity.MyPlantEntity
import com.expeditee.plant.identifier.data.entity.PopularPlantEntity
import com.expeditee.plant.identifier.data.entity.PopularPlantFtsEntity
import com.expeditee.plant.identifier.data.entity.ReminderEntity

@Database(
    entities = [PopularPlantEntity::class, MyPlantEntity::class, PopularPlantFtsEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun popularPlantDao(): PopularPlantDao

    abstract fun myPlantDao(): MyPlantDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "plant_identifier.db"
    }
}