package com.prestoxbasopp.ide

import com.prestoxbasopp.ui.XbHighlightingPreferences
import com.prestoxbasopp.ui.XbKeyValueStore
import com.prestoxbasopp.ui.XbUiSettingsState
import com.prestoxbasopp.ui.XbUiSettingsStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbFormattingServiceTest {
    @Test
    fun `formatting service applies formatter to text`() {
        val source = """
            if foo
            bar()
            endif
        """.trimIndent()

        val formatted = XbFormattingService().formatText(source, indentSize = 2)

        assertThat(formatted).isEqualTo(
            """
            if foo
              bar()
            endif
            """.trimIndent(),
        )
    }

    @Test
    fun `resolveIndentSize uses ui tab size when code style is default`() {
        val service = XbFormattingService(
            settingsStore = XbUiSettingsStore(InMemoryKeyValueStore()).also {
                it.save(XbUiSettingsState(tabSize = 3, highlightingPreferences = XbHighlightingPreferences()))
            },
        )

        assertThat(service.resolveIndentSize(codeStyleIndentSize = 4)).isEqualTo(3)
    }

    @Test
    fun `resolveIndentSize keeps explicit code style indentation when customized`() {
        val service = XbFormattingService(
            settingsStore = XbUiSettingsStore(InMemoryKeyValueStore()).also {
                it.save(XbUiSettingsState(tabSize = 2, highlightingPreferences = XbHighlightingPreferences()))
            },
        )

        assertThat(service.resolveIndentSize(codeStyleIndentSize = 8)).isEqualTo(8)
    }

    private class InMemoryKeyValueStore : XbKeyValueStore {
        private val booleans = mutableMapOf<String, Boolean>()
        private val ints = mutableMapOf<String, Int>()
        private val strings = mutableMapOf<String, String>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue

        override fun putInt(key: String, value: Int) {
            ints[key] = value
        }

        override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue

        override fun putString(key: String, value: String) {
            strings[key] = value
        }
    }
}
