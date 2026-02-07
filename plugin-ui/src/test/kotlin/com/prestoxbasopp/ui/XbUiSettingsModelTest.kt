package com.prestoxbasopp.ui

import com.prestoxbasopp.core.api.XbLanguageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbUiSettingsModelTest {
    @Test
    fun `builds display name from language service`() {
        val service = object : XbLanguageService {
            override fun languageId(): String = "xbase++"
        }
        val store = XbUiSettingsStore(InMemoryKeyValueStore())

        val model = XbUiSettingsModel(service, store)

        assertThat(model.displayName()).isEqualTo("xbase++ Settings")
    }

    @Test
    fun `updates state and persists it`() {
        val service = object : XbLanguageService {
            override fun languageId(): String = "xbase++"
        }
        val storage = InMemoryKeyValueStore()
        val store = XbUiSettingsStore(storage)
        val model = XbUiSettingsModel(service, store)

        val newState = XbUiSettingsState(
            enableSyntaxHighlighting = false,
            showInlayHints = false,
            tabSize = 2,
            completionLimit = 80,
        )

        model.updateState(newState)

        assertThat(model.state).isEqualTo(newState)
        assertThat(store.load()).isEqualTo(newState)
    }

    @Test
    fun `reset to defaults restores default state`() {
        val service = object : XbLanguageService {
            override fun languageId(): String = "xbase++"
        }
        val store = XbUiSettingsStore(InMemoryKeyValueStore())
        val model = XbUiSettingsModel(service, store)

        model.updateState(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = false,
                tabSize = 2,
                completionLimit = 80,
            ),
        )

        model.resetToDefaults()

        assertThat(model.state).isEqualTo(XbUiSettingsState())
    }
}
