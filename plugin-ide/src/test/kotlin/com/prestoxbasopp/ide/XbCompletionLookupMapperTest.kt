package com.prestoxbasopp.ide

import com.prestoxbasopp.core.stubs.XbStubType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionLookupMapperTest {
    @Test
    fun `maps completion items to lookup metadata`() {
        val items = listOf(
            XbCompletionItem("Alpha", XbStubType.FUNCTION),
            XbCompletionItem("Beta", XbStubType.VARIABLE),
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
        )
    }
}
