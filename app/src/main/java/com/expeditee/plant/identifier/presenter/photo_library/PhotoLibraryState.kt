package com.expeditee.plant.identifier.presenter.photo_library

import com.expeditee.plant.identifier.common.BaseViewModel

sealed interface PhotoLibraryState : BaseViewModel.State {
    data object Initial : PhotoLibraryState
}
