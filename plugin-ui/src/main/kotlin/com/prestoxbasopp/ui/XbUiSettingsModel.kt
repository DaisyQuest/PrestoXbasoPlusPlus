package com.prestoxbasopp.ui

import com.prestoxbasopp.core.api.XbLanguageService

class XbUiSettingsModel(private val languageService: XbLanguageService) {
    fun displayName(): String {
        return "${languageService.languageId()} Settings"
    }
}
