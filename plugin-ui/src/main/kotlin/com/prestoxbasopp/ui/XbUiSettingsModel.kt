package com.prestoxbasopp.ui

import com.prestoxbasopp.core.api.XbLanguageService

class XbUiSettingsModel(
    private val languageService: XbLanguageService,
    private val settingsStore: XbUiSettingsStore,
) {
    var state: XbUiSettingsState = settingsStore.load()
        private set

    fun displayName(): String {
        return "${languageService.languageId()} Settings"
    }

    fun updateState(newState: XbUiSettingsState) {
        state = newState
        settingsStore.save(newState)
    }

    fun resetToDefaults() {
        updateState(XbUiSettingsState())
    }
}
