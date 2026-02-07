package com.prestoxbasopp.ui

interface XbUiSettingsView {
    fun render(state: XbUiSettingsState)
    fun currentState(): XbUiSettingsState
}

class XbUiSettingsPresenter(
    private val model: XbUiSettingsModel,
    private val view: XbUiSettingsView,
) {
    fun load() {
        view.render(model.state)
    }

    fun isModified(): Boolean {
        return view.currentState() != model.state
    }

    fun applyChanges() {
        model.updateState(view.currentState())
    }

    fun reset() {
        model.resetToDefaults()
        view.render(model.state)
    }
}
