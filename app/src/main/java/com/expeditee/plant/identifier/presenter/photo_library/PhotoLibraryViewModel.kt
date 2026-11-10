package com.expeditee.plant.identifier.presenter.photo_library

import android.app.Application
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.ResultFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhotoLibraryViewModel(application: Application) :
    BaseViewModel<PhotoLibraryState>(application) {

    private val _photoState = MutableStateFlow<ResultFlow<Unit>>(ResultFlow.Initial())
    val photoState: StateFlow<ResultFlow<Unit>> = _photoState

    fun notifyError(message: String?) {
        _photoState.value = ResultFlow.Error(message)
    }

    override fun onState(state: PhotoLibraryState) = Unit
}
