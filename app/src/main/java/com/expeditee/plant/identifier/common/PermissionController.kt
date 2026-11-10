package com.expeditee.plant.identifier.common

interface IPermissionController {
    fun registerPermissionLauncher()
    fun askPermission(
        permission: String,
        onAcceptedAction: () -> Unit,
        onDeniedAction: () -> Unit
    )
}
