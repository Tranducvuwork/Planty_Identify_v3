package com.expeditee.plant.identifier.data.mapper

import com.expeditee.plant.identifier.common.AbstractMapper
import com.expeditee.plant.identifier.data.entity.ReminderEntity
import com.expeditee.plant.identifier.model.ReminderScheduleModel

class ReminderMapper : AbstractMapper<ReminderEntity, ReminderScheduleModel>() {
    override fun mapToDomain(entity: ReminderEntity): ReminderScheduleModel =
        ReminderScheduleModel(
            id = entity.id,
            plantId = entity.plantId,
            plantName = entity.plantName,
            remindType = entity.remindType,
            remindTime = entity.remindTime,
            repeatType = entity.repeatType,
            previousDate = entity.previousDate,
            nextDate = entity.nextDate,
            lastDate = entity.lastDate,
            isEnabled = entity.isEnabled,
            imagePath = entity.imagePath
        )

    override fun mapToEntity(domain: ReminderScheduleModel): ReminderEntity =
        ReminderEntity(
            id = domain.id,
            plantId = domain.plantId,
            plantName = domain.plantName,
            remindType = domain.remindType,
            remindTime = domain.remindTime,
            repeatType = domain.repeatType,
            previousDate = domain.previousDate,
            nextDate = domain.nextDate,
            lastDate = domain.lastDate,
            isEnabled = domain.isEnabled,
            imagePath = domain.imagePath
        )
}
