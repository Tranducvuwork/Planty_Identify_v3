package com.expeditee.plant.identifier.common

interface BaseMapper<Entity, Domain> {
    fun mapToDomain(entity: Entity): Domain
    fun mapToEntity(domain: Domain): Entity
    fun mapToDomainList(entities: List<Entity>): List<Domain>
    fun mapToEntityList(domains: List<Domain>): List<Entity>
}