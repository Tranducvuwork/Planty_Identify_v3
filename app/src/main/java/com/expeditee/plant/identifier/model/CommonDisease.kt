package com.expeditee.plant.identifier.model

data class CommonDisease(
    val id: Int? = null,
    val similarImages: List<SimilarImage>? = null,
    val commonNames: List<String>? = null,
    val scientificName: String? = null,
    val otherNames: List<String>? = null,
    val family: String? = null,
    val description: String? = null,
    val symptoms: List<String>? = null,
    val causes: List<String>? = null,
    val treatment: List<String>? = null,
    val prevention: List<String>? = null,
    val severity: String? = null,
    val affectedPlants: List<String>? = null,
)

data class SimilarImage(
    val url: String? = null,
    val urlSmall: String? = null,
    val license: License? = null,
)

data class License(
    val url: String? = null,
    val name: String? = null,
)