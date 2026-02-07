package com.prestoxbasopp.ui

import java.util.prefs.Preferences

interface XbKeyValueStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
}

class PreferencesKeyValueStore(private val preferences: Preferences) : XbKeyValueStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        preferences.putInt(key, value)
    }
}

class XbUiSettingsStore(
    private val storage: XbKeyValueStore,
    private val defaults: XbUiSettingsState = XbUiSettingsState(),
) {
    fun load(): XbUiSettingsState {
        val enableSyntaxHighlighting = storage.getBoolean(KEY_ENABLE_SYNTAX, defaults.enableSyntaxHighlighting)
        val showInlayHints = storage.getBoolean(KEY_SHOW_INLAY_HINTS, defaults.showInlayHints)
        val tabSizeRaw = storage.getInt(KEY_TAB_SIZE, defaults.tabSize)
        val completionLimitRaw = storage.getInt(KEY_COMPLETION_LIMIT, defaults.completionLimit)

        return XbUiSettingsState(
            enableSyntaxHighlighting = enableSyntaxHighlighting,
            showInlayHints = showInlayHints,
            tabSize = tabSizeRaw.coerceIn(MIN_TAB_SIZE, MAX_TAB_SIZE),
            completionLimit = completionLimitRaw.coerceIn(MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT),
        )
    }

    fun save(state: XbUiSettingsState) {
        storage.putBoolean(KEY_ENABLE_SYNTAX, state.enableSyntaxHighlighting)
        storage.putBoolean(KEY_SHOW_INLAY_HINTS, state.showInlayHints)
        storage.putInt(KEY_TAB_SIZE, state.tabSize.coerceIn(MIN_TAB_SIZE, MAX_TAB_SIZE))
        storage.putInt(KEY_COMPLETION_LIMIT, state.completionLimit.coerceIn(MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT))
    }

    companion object {
        const val MIN_TAB_SIZE = 1
        const val MAX_TAB_SIZE = 16
        const val MIN_COMPLETION_LIMIT = 1
        const val MAX_COMPLETION_LIMIT = 200

        private const val KEY_ENABLE_SYNTAX = "ui.enableSyntaxHighlighting"
        private const val KEY_SHOW_INLAY_HINTS = "ui.showInlayHints"
        private const val KEY_TAB_SIZE = "ui.tabSize"
        private const val KEY_COMPLETION_LIMIT = "ui.completionLimit"

        fun defaultsStore(): XbUiSettingsStore {
            return XbUiSettingsStore(PreferencesKeyValueStore(Preferences.userNodeForPackage(XbUiSettingsStore::class.java)))
        }
    }
}
