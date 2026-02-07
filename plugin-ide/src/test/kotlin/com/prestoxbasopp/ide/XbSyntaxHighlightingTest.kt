package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbSyntaxHighlightingTest {
    @Test
    fun `highlights tokens with expected styles`() {
        val source = "if foo == 10 return \"bar\"; !"
        val spans = XbSyntaxHighlighter().highlight(source)

        val styles = spans.map { it.style }
        assertThat(styles).contains(
            XbHighlightStyle.KEYWORD,
            XbHighlightStyle.IDENTIFIER,
            XbHighlightStyle.OPERATOR,
            XbHighlightStyle.NUMBER,
            XbHighlightStyle.STRING,
            XbHighlightStyle.PUNCTUATION,
            XbHighlightStyle.ERROR,
        )
        assertThat(spans).allSatisfy { span ->
            assertThat(span.textRange.endOffset).isGreaterThanOrEqualTo(span.textRange.startOffset)
        }
    }

    @Test
    fun `omits eof tokens from highlighting`() {
        val source = ""
        val spans = XbSyntaxHighlighter().highlight(source)
        assertThat(spans).isEmpty()
    }
}
