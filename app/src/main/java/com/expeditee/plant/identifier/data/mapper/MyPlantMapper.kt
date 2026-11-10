package com.expeditee.plant.identifier.data.mapper


import com.expeditee.plant.identifier.common.AbstractMapper
import com.expeditee.plant.identifier.data.entity.MyPlantEntity
import com.expeditee.plant.identifier.model.MyPlantModel

class MyPlantMapper : AbstractMapper<MyPlantEntity, MyPlantModel>() {

    override fun mapToDomain(entity: MyPlantEntity): MyPlantModel {
        return MyPlantModel(
            id = entity.id,
            uuid = entity.uuid,
            name = entity.name,
            scientificName = entity.scientificName,
            status = entity.status,
            timestamp = entity.timestamp,
            imageFile = entity.imageFile,
            metadata = entity.metadata,
            gardenId = entity.gardenId,
            isActive = entity.isActive,
            type = entity.type
        )
    }

    override fun mapToEntity(domain: MyPlantModel): MyPlantEntity {
        return MyPlantEntity(
            id = domain.id,
            uuid = domain.uuid,
            name = domain.name,
            scientificName = domain.scientificName,
            status = domain.status,
            timestamp = domain.timestamp,
            imageFile = domain.imageFile,
            metadata = domain.metadata,
            gardenId = domain.gardenId,
            isActive = domain.isActive,
            type = domain.type
        )
    }
}