package com.prestoxbasopp.ui

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class XbHighlightingPreferencesTest {
    @Test
    fun `constructor requires complete style mappings`() {
        assertThatThrownBy {
            XbHighlightingPreferences(styleMappings = emptyMap())
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `withNormalizedOverrides trims and lowercases keys`() {
        val preferences = XbHighlightingPreferences(
            wordOverrides = mapOf("  Foo  " to XbHighlightCategory.KEYWORD, " " to XbHighlightCategory.STRING),
        )

        val normalized = preferences.withNormalizedOverrides()

        assertThat(normalized.wordOverrides).containsExactly(mapOf("foo" to XbHighlightCategory.KEYWORD).entries.first())
    }
}
