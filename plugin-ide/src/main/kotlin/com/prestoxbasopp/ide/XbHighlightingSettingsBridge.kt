package com.prestoxbasopp.ide

import com.prestoxbasopp.ui.XbHighlightingPreferences
import com.prestoxbasopp.ui.XbUiSettingsStore

interface XbHighlightingPreferencesProvider {
    fun load(): XbHighlightingPreferences

    fun isSyntaxHighlightingEnabled(): Boolean = true
}

object XbHighlightingSettingsBridge : XbHighlightingPreferencesProvider {
    private fun settingsState() = XbUiSettingsStore.defaultsStore().load()

    override fun load(): XbHighlightingPreferences {
        return settingsState().highlightingPreferences
    }

    override fun isSyntaxHighlightingEnabled(): Boolean = settingsState().enableSyntaxHighlighting
}
