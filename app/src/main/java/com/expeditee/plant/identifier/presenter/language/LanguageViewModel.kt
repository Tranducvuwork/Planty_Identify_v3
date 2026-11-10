package com.expeditee.plant.identifier.presenter.language

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class LanguageViewModel(application: Application) :
    BaseViewModel<BaseViewModel.State>(application) {

    private val _languageList = MutableStateFlow<List<Language>?>(null)
    internal val languageList = _languageList.onStart { fetchLanguages() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        null
    )

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

    private fun fetchLanguages() {
        _languageList.value = Language.getAllLanguages()
    }

    override fun onState(state: State) {
        /* no-op */
    }
}
