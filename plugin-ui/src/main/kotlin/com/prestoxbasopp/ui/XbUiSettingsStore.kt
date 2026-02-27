package com.prestoxbasopp.ui

import java.util.prefs.Preferences

interface XbKeyValueStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
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

    override fun getString(key: String, defaultValue: String): String {
        return preferences.get(key, defaultValue)
    }

    override fun putString(key: String, value: String) {
        preferences.put(key, value)
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
        val styleMappings = decodeStyleMappings(
            storage.getString(KEY_HIGHLIGHT_STYLE_MAPPINGS, ""),
            defaults.highlightingPreferences.styleMappings,
        )
        val wordOverrides = decodeWordOverrides(
            storage.getString(KEY_HIGHLIGHT_WORD_OVERRIDES, ""),
            defaults.highlightingPreferences.wordOverrides,
        )

        return XbUiSettingsState(
            enableSyntaxHighlighting = enableSyntaxHighlighting,
            showInlayHints = showInlayHints,
            tabSize = tabSizeRaw.coerceIn(MIN_TAB_SIZE, MAX_TAB_SIZE),
            completionLimit = completionLimitRaw.coerceIn(MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT),
            highlightingPreferences = XbHighlightingPreferences(
                styleMappings = styleMappings,
                wordOverrides = wordOverrides,
            ).withNormalizedOverrides(),
        )
    }

    fun save(state: XbUiSettingsState) {
        storage.putBoolean(KEY_ENABLE_SYNTAX, state.enableSyntaxHighlighting)
        storage.putBoolean(KEY_SHOW_INLAY_HINTS, state.showInlayHints)
        storage.putInt(KEY_TAB_SIZE, state.tabSize.coerceIn(MIN_TAB_SIZE, MAX_TAB_SIZE))
        storage.putInt(KEY_COMPLETION_LIMIT, state.completionLimit.coerceIn(MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT))
        storage.putString(KEY_HIGHLIGHT_STYLE_MAPPINGS, encodeStyleMappings(state.highlightingPreferences.styleMappings))
        storage.putString(KEY_HIGHLIGHT_WORD_OVERRIDES, encodeWordOverrides(state.highlightingPreferences.wordOverrides))
    }

    private fun decodeStyleMappings(
        encoded: String,
        fallback: Map<XbHighlightCategory, XbHighlightCategory>,
    ): Map<XbHighlightCategory, XbHighlightCategory> {
        val parsed = encoded.split(";")
            .asSequence()
            .mapNotNull { entry ->
                val delimiter = entry.indexOf('=')
                if (delimiter <= 0 || delimiter == entry.lastIndex) {
                    return@mapNotNull null
                }
                val source = entry.substring(0, delimiter).trim()
                val target = entry.substring(delimiter + 1).trim()
                val sourceCategory = XbHighlightCategory.entries.find { it.name == source } ?: return@mapNotNull null
                val targetCategory = XbHighlightCategory.entries.find { it.name == target } ?: return@mapNotNull null
                sourceCategory to targetCategory
            }
            .toMap()

        if (parsed.isEmpty()) {
            return fallback
        }
        return XbHighlightCategory.entries.associateWith { category ->
            parsed[category] ?: fallback[category] ?: category
        }
    }

    private fun decodeWordOverrides(
        encoded: String,
        fallback: Map<String, XbHighlightCategory>,
    ): Map<String, XbHighlightCategory> {
        val parsed = encoded.split(";")
            .asSequence()
            .mapNotNull { entry ->
                val delimiter = entry.indexOf('=')
                if (delimiter <= 0 || delimiter == entry.lastIndex) {
                    return@mapNotNull null
                }
                val word = entry.substring(0, delimiter).trim().lowercase()
                if (word.isEmpty()) {
                    return@mapNotNull null
                }
                val style = entry.substring(delimiter + 1).trim()
                val styleCategory = XbHighlightCategory.entries.find { it.name == style } ?: return@mapNotNull null
                word to styleCategory
            }
            .toMap()

        return if (parsed.isEmpty()) fallback else parsed
    }

    private fun encodeStyleMappings(mappings: Map<XbHighlightCategory, XbHighlightCategory>): String {
        return XbHighlightCategory.entries
            .joinToString(";") { category ->
                val mapped = mappings[category] ?: category
                "${category.name}=${mapped.name}"
            }
    }

    private fun encodeWordOverrides(overrides: Map<String, XbHighlightCategory>): String {
        return overrides.entries
            .asSequence()
            .map { it.key.trim().lowercase() to it.value }
            .filter { it.first.isNotEmpty() }
            .sortedBy { it.first }
            .joinToString(";") { (word, style) -> "$word=${style.name}" }
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
        private const val KEY_HIGHLIGHT_STYLE_MAPPINGS = "ui.highlightStyleMappings"
        private const val KEY_HIGHLIGHT_WORD_OVERRIDES = "ui.highlightWordOverrides"

        fun defaultsStore(): XbUiSettingsStore {
            return XbUiSettingsStore(PreferencesKeyValueStore(Preferences.userNodeForPackage(XbUiSettingsStore::class.java)))
        }
    }
}
