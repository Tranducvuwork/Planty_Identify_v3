package com.expeditee.plant.identifier.presenter.result_identify

import android.app.Application
import com.expeditee.plant.identifier.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IdentifyResultViewModel(application: Application) :
    BaseViewModel<IdentifyResultState>(application) {

    private val _state = MutableStateFlow<IdentifyResultState>(IdentifyResultState.Loading)
    val state: StateFlow<IdentifyResultState> = _state.asStateFlow()

    override fun onState(state: IdentifyResultState) {
        _state.value = state
    }

    fun loadResult(plantName: String, confidencePercent: Int, source: String?) {
        onState(
            IdentifyResultState.Content(
                plantName = plantName,
                confidencePercent = confidencePercent.coerceIn(0, 100),
                source = source,
            ),
        )
    }
}

sealed class IdentifyResultState : BaseViewModel.State {
    data object Loading : IdentifyResultState()

    data class Content(
        val plantName: String,
        val confidencePercent: Int,
        val source: String?,
    ) : IdentifyResultState()
}
