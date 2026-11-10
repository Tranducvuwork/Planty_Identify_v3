package com.expeditee.plant.identifier.admob.callback

interface LammaAdCallback<T> {
    fun onLoaded(data: T?)
    fun onFailed()
}
