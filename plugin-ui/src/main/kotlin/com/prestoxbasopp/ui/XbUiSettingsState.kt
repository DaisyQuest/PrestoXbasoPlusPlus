package com.prestoxbasopp.ui

data class XbUiSettingsState(
    val enableSyntaxHighlighting: Boolean = true,
    val showInlayHints: Boolean = true,
    val tabSize: Int = 4,
    val completionLimit: Int = 50,
)
