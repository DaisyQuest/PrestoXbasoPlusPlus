package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionLookupMapperTest {
    @Test
    fun `maps completion items to lookup metadata`() {
        val items = listOf(
            XbCompletionItem("Alpha", XbCompletionType.FUNCTION),
            XbCompletionItem("Beta", XbCompletionType.VARIABLE),
            XbCompletionItem("LOCAL", XbCompletionType.KEYWORD),
        )

        val lookups = XbCompletionLookupMapper().map(items)

        assertThat(lookups).containsExactly(
            XbCompletionLookup(
                label = "Alpha",
                typeText = "FUNCTION",
                insertText = "Alpha()",
                caretOffsetDelta = -1,
            ),
            XbCompletionLookup(
                label = "Beta",
                typeText = "VARIABLE",
                insertText = "Beta",
                caretOffsetDelta = null,
            ),
            XbCompletionLookup(
                label = "LOCAL",
                typeText = "KEYWORD",
                insertText = "LOCAL",
                caretOffsetDelta = null,
            ),
        )
    }
}
