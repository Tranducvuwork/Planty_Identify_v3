package com.expeditee.plant.identifier.common

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.model.Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel<State : BaseViewModel.State>(application: Application) :
    AndroidViewModel(application), KoinComponent {

    protected val appConfigSettings: AppConfigSettings by inject<AppConfigSettings>()
    protected val applicationContext: Context
        get() = getApplication<Application>().applicationContext

    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
    }

    private val _purchaseState: MutableStateFlow<Boolean> = MutableStateFlow(true)
    internal var purchaseState: StateFlow<Boolean> = _purchaseState.asStateFlow()
    internal val purchaseStateValue: Boolean
        get() = purchaseState.value

    private val _networkState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    internal val networkState: StateFlow<Boolean> = _networkState.asStateFlow()
    internal val networkStateValue: Boolean
        get() = networkState.value

    fun setNetworkState(isConnected: Boolean) {
        _networkState.value = isConnected
    }

    fun setPurchaseState(isPurchased: Boolean) {
        _purchaseState.value = isPurchased
    }

    protected fun launchBlock(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(
            context = dispatcher + coroutineExceptionHandler,
            start = start,
            block = block
        )
    }

    abstract fun onState(state: State)

    interface State

    override fun onCleared() {
        super.onCleared()
    }
}

class CommonViewModel(application: Application) : BaseViewModel<BaseViewModel.State>(application) {

    internal var introCompleted: Boolean
        get() = appConfigSettings.introCompleted
        set(value) {
            appConfigSettings.introCompleted = value
        }

    internal var currentLanguage: Language?
        get() = Language.entries.firstOrNull { it.isoCountry == appConfigSettings.currentLanguage }
        set(newLanguage) {
            appConfigSettings.currentLanguage = newLanguage?.isoCountry
        }

    override fun onState(state: State) {
        /* no-op */
    }
}
