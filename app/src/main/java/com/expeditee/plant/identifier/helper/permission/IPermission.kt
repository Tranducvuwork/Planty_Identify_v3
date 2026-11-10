package com.expeditee.plant.identifier.helper.permission

import permissions.dispatcher.PermissionRequest

typealias OnShowRationale = (request: PermissionRequest) -> Unit
typealias OnPermissionGranted = () -> Unit
typealias OnPermissionDenied = () -> Unit
typealias OnNeverAskAgain = () -> Unit

interface IPermission {

}