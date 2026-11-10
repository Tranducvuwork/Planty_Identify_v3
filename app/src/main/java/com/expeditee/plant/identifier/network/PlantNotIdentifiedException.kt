package com.expeditee.plant.identifier.network

class PlantNotIdentifiedException(
    message: String = "Plant not identified",
    val userReason: String? = null,
) : Exception(message)
