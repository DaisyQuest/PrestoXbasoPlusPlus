package com.prestoxbasopp.ide

import com.prestoxbasopp.ui.XbHighlightingPreferences
import com.prestoxbasopp.ui.XbKeyValueStore
import com.prestoxbasopp.ui.XbUiSettingsState
import com.prestoxbasopp.ui.XbUiSettingsStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbFormattingServiceTest {
    @Test
    fun `formatting service applies formatter to text using spaces`() {
        val source = """
            if foo
            bar()
            endif
        """.trimIndent()

        val formatted = XbFormattingService().formatText(
            source = source,
            codeStyleIndentSize = 2,
            codeStyleTabSize = 2,
            useTabCharacter = false,
        )

        assertThat(formatted).isEqualTo(
            """
            if foo
              bar()
            endif
            """.trimIndent(),
        )
    }

    @Test
    fun `formatting service applies formatter to text using tabs when enabled`() {
        val source = "if foo\nbar()\nendif"

        val formatted = XbFormattingService().formatText(
            source = source,
            codeStyleIndentSize = 4,
            codeStyleTabSize = 8,
            useTabCharacter = true,
        )

        assertThat(formatted).isEqualTo("if foo\n\tbar()\nendif")
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

    @Test
    fun `resolveSpaceIndentSize prefers positive indent size`() {
        val service = XbFormattingService()

        assertThat(service.resolveSpaceIndentSize(indentSize = 2, tabSize = 8)).isEqualTo(2)
    }

    @Test
    fun `resolveSpaceIndentSize falls back to tab size when indent size is zero`() {
        val service = XbFormattingService()

        assertThat(service.resolveSpaceIndentSize(indentSize = 0, tabSize = 3)).isEqualTo(3)
    }

    @Test
    fun `resolveSpaceIndentSize clamps negative tab size to zero`() {
        val service = XbFormattingService()

        assertThat(service.resolveSpaceIndentSize(indentSize = -1, tabSize = -2)).isEqualTo(0)
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
