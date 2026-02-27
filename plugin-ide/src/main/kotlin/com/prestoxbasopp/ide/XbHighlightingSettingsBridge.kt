package com.prestoxbasopp.ide

import com.prestoxbasopp.ui.XbHighlightingPreferences
import com.prestoxbasopp.ui.XbUiSettingsStore

interface XbHighlightingPreferencesProvider {
    fun load(): XbHighlightingPreferences
}

object XbHighlightingSettingsBridge : XbHighlightingPreferencesProvider {
    override fun load(): XbHighlightingPreferences {
        return XbUiSettingsStore.defaultsStore().load().highlightingPreferences
    }
}
