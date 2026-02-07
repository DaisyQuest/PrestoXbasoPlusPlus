package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionPrefixExtractorTest {
    @Test
    fun `extracts identifier prefix at caret`() {
        val text = "function Alpha()"
        val offset = "function Al".length

        val prefix = XbCompletionPrefixExtractor.extract(text, offset)

        assertThat(prefix).isEqualTo("Al")
    }

    @Test
    fun `returns empty prefix when caret follows whitespace`() {
        val text = "function Alpha()"
        val offset = "function ".length

        val prefix = XbCompletionPrefixExtractor.extract(text, offset)

        assertThat(prefix).isEmpty()
    }

    @Test
    fun `handles caret at start or end safely`() {
        assertThat(XbCompletionPrefixExtractor.extract("Alpha", 0)).isEmpty()
        assertThat(XbCompletionPrefixExtractor.extract("Alpha", 5)).isEqualTo("Alpha")
    }

    @Test
    fun `allows digits and underscores in prefix`() {
        val text = "var item_2"
        val offset = text.length

        val prefix = XbCompletionPrefixExtractor.extract(text, offset)

        assertThat(prefix).isEqualTo("item_2")
    }
}
