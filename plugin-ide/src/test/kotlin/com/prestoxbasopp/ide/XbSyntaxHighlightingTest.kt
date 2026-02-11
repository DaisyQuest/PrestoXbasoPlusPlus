package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbSyntaxHighlightingTest {
    @Test
    fun `highlights tokens with expected styles`() {
        val source = """
            #define ANSWER 42
            #include "defs.ch"
            function foo()
                local d := {^2024-01-02}
                local s := "bar"
                local sym := #name
                local cb := {|a| a + 1|}
                return foo + 1
                // comment
            end
        """.trimIndent()
        val spans = XbSyntaxHighlighter().highlight(source)

        val styles = spans.map { it.style }
        assertThat(styles).contains(
            XbHighlightStyle.KEYWORD,
            XbHighlightStyle.IDENTIFIER,
            XbHighlightStyle.OPERATOR,
            XbHighlightStyle.NUMBER,
            XbHighlightStyle.STRING,
            XbHighlightStyle.PUNCTUATION,
            XbHighlightStyle.DATE,
            XbHighlightStyle.SYMBOL,
            XbHighlightStyle.CODEBLOCK,
            XbHighlightStyle.COMMENT,
            XbHighlightStyle.PREPROCESSOR,
            XbHighlightStyle.MACRO_DEFINITION,
        )
        assertThat(styles).doesNotContain(XbHighlightStyle.ERROR)
        assertThat(spans).allSatisfy { span ->
            assertThat(span.textRange.endOffset).isGreaterThanOrEqualTo(span.textRange.startOffset)
        }
    }

    @Test
    fun `highlights invalid tokens as errors`() {
        val spans = XbSyntaxHighlighter().highlight("0x")

        assertThat(spans).hasSize(1)
        assertThat(spans.single().style).isEqualTo(XbHighlightStyle.ERROR)
    }

    @Test
    fun `omits eof tokens from highlighting`() {
        val source = ""
        val spans = XbSyntaxHighlighter().highlight(source)
        assertThat(spans).isEmpty()
    }
}
