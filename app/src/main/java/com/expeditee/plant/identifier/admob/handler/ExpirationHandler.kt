package com.expeditee.plant.identifier.admob.handler

interface ExpirationHandler {
    fun isAdExpired(currentTimeMillis: Long, lastTimeAdded: Long): Boolean
}

class AdmobExpirationHandler(
    private val adTimeout: Long
) : ExpirationHandler {

    override fun isAdExpired(
        currentTimeMillis: Long,
        lastTimeAdded: Long
    ): Boolean = currentTimeMillis - lastTimeAdded >= adTimeout
}
