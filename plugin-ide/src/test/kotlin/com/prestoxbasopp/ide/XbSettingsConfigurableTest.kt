package com.prestoxbasopp.ide

import com.prestoxbasopp.ui.XbHighlightCategory
import com.prestoxbasopp.ui.XbHighlightingPreferences
import com.prestoxbasopp.ui.XbKeyValueStore
import com.prestoxbasopp.ui.XbUiSettingsModel
import com.prestoxbasopp.ui.XbUiSettingsPanel
import com.prestoxbasopp.ui.XbUiSettingsState
import com.prestoxbasopp.ui.XbUiSettingsStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbSettingsConfigurableTest {
    @Test
    fun `isModified returns false before ui is created`() {
        val configurable = XbSettingsConfigurable(model = modelWithStore())
        assertThat(configurable.isModified).isFalse()
    }

    @Test
    fun `apply persists updated settings`() {
        val store = XbUiSettingsStore(InMemoryKeyValueStore())
        val model = XbUiSettingsModel(XbIdeLanguageService(), store)
        val panel = XbUiSettingsPanel()
        val configurable = XbSettingsConfigurable(model = model, panelFactory = { panel })

        configurable.createComponent()

        val updatedState = XbUiSettingsState(
            enableSyntaxHighlighting = false,
            showInlayHints = false,
            tabSize = 2,
            completionLimit = 25,
            highlightingPreferences = XbHighlightingPreferences(
                wordOverrides = mapOf("foo" to XbHighlightCategory.FUNCTION_CALL),
            ),
        )
        panel.render(updatedState)

        assertThat(configurable.isModified).isTrue()
        configurable.apply()

        assertThat(configurable.isModified).isFalse()
        assertThat(model.state).isEqualTo(updatedState)
    }

    @Test
    fun `reset returns panel state to defaults`() {
        val store = XbUiSettingsStore(InMemoryKeyValueStore())
        val model = XbUiSettingsModel(XbIdeLanguageService(), store)
        val panel = XbUiSettingsPanel()
        val configurable = XbSettingsConfigurable(model = model, panelFactory = { panel })

        configurable.createComponent()
        panel.render(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = false,
                tabSize = 8,
                completionLimit = 120,
            ),
        )

        configurable.reset()

        assertThat(panel.currentState()).isEqualTo(XbUiSettingsState())
    }

    private fun modelWithStore(store: XbKeyValueStore = InMemoryKeyValueStore()): XbUiSettingsModel {
        return XbUiSettingsModel(XbIdeLanguageService(), XbUiSettingsStore(store))
    }

    private class InMemoryKeyValueStore : XbKeyValueStore {
        private val booleans = mutableMapOf<String, Boolean>()
        private val ints = mutableMapOf<String, Int>()
        private val strings = mutableMapOf<String, String>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return booleans[key] ?: defaultValue
        }

        override fun putBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun getInt(key: String, defaultValue: Int): Int {
            return ints[key] ?: defaultValue
        }

        override fun putInt(key: String, value: Int) {
            ints[key] = value
        }

        override fun getString(key: String, defaultValue: String): String {
            return strings[key] ?: defaultValue
        }

        override fun putString(key: String, value: String) {
            strings[key] = value
        }
    }
}
