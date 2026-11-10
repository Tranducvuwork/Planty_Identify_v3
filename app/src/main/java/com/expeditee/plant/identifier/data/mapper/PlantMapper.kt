package com.expeditee.plant.identifier.data.mapper

import com.expeditee.plant.identifier.common.AbstractMapper
import com.expeditee.plant.identifier.model.reponse.PlantResponse
import com.expeditee.plant.identifier.data.entity.PopularPlantEntity
import com.expeditee.plant.identifier.model.PopularPlantModel
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement


class PlantMapper(
    private val remoteConfigManagement: RemoteConfigManagement
) : AbstractMapper<PopularPlantEntity, PopularPlantModel>() {

    private fun getBaseUrl(): String? =
        remoteConfigManagement.baseUrlData.takeIf { it.isNotBlank() }


    fun mapResponseToEntity(response: PlantResponse): PopularPlantEntity {
        val commonNames = response.commonNames ?: emptyList()
        return PopularPlantEntity(
            id = response.id,
            commonNames = commonNames,
            scientificName = response.scientificName,
            mainImageUrl = response.mainImage?.url?.let { "PlantImage/$it" }
        )
    }

    fun mapResponseListToEntityList(responses: List<PlantResponse>): List<PopularPlantEntity> {
        return responses.map { mapResponseToEntity(it) }
    }

    override fun mapToDomain(entity: PopularPlantEntity): PopularPlantModel {
        val scientificName = entity.scientificName?.takeIf { it.isNotBlank() } ?: "Unknown Plant"
        val commonName = entity.commonNames.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "Unknown Plant"

        val imageUrl = entity.mainImageUrl?.trim()?.takeIf { it.isNotBlank() }
            ?.let { "${getBaseUrl()}$it" }

        val type = if (commonName.contains("Flowering", ignoreCase = true) || commonName.contains("Flower", ignoreCase = true)) {
            PopularPlantModel.TYPE_FLOWER
        } else {
            PopularPlantModel.TYPE_PLANT
        }

        return PopularPlantModel(
            id = entity.id.toString(),
            name = commonName,
            scientificName = scientificName,
            imageUrl = imageUrl,
            type = type
        )
    }

    override fun mapToEntity(domain: PopularPlantModel): PopularPlantEntity {
        return PopularPlantEntity(
            id = domain.id.toIntOrNull() ?: 0,
            commonNames = listOf(domain.name),
            scientificName = domain.scientificName,
            mainImageUrl = domain.imageUrl
        )
    }
}