package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.lexer.XbTokenType
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
                foo(42)
                // comment
            end
        """.trimIndent()
        val spans = XbSyntaxHighlighter().highlight(source)

        val styles = spans.map { it.style }
        assertThat(styles).contains(
            XbHighlightStyle.KEYWORD,
            XbHighlightStyle.IDENTIFIER,
            XbHighlightStyle.FUNCTION_DECLARATION,
            XbHighlightStyle.FUNCTION_CALL,
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

    @Test
    fun `semantic classifier handles declaration and fallback branches`() {
        val classifier = XbSemanticTokenClassifier()
        val tokens = listOf(
            XbToken(XbTokenType.KEYWORD, "function", XbTextRange(0, 8)),
            XbToken(XbTokenType.PUNCTUATION, "(", XbTextRange(9, 10)),
            XbToken(XbTokenType.IDENTIFIER, "foo", XbTextRange(10, 13)),
            XbToken(XbTokenType.PUNCTUATION, ")", XbTextRange(13, 14)),
            XbToken(XbTokenType.KEYWORD, "procedure", XbTextRange(15, 24)),
            XbToken(XbTokenType.IDENTIFIER, "bar", XbTextRange(25, 28)),
            XbToken(XbTokenType.IDENTIFIER, "baz", XbTextRange(29, 32)),
            XbToken(XbTokenType.PUNCTUATION, "(", XbTextRange(32, 33)),
        )

        val styles = classifier.classify(tokens)

        assertThat(styles).containsExactly(
            XbHighlightStyle.KEYWORD,
            XbHighlightStyle.PUNCTUATION,
            XbHighlightStyle.IDENTIFIER,
            XbHighlightStyle.PUNCTUATION,
            XbHighlightStyle.KEYWORD,
            XbHighlightStyle.FUNCTION_DECLARATION,
            XbHighlightStyle.FUNCTION_CALL,
            XbHighlightStyle.PUNCTUATION,
        )
    }
}
