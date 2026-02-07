package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerPreprocessorTest {
    @Test
    fun `captures preprocessor directives and builds source map`() {
        val source = """
            #define FOO 1
            x := 1
            # include "lib.xb"
            y := 2
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.directives).hasSize(2)
        assertThat(result.directives[0].name).isEqualTo("define")
        assertThat(result.directives[1].name).isEqualTo("include")

        val expectedFiltered = "\nx := 1\n\ny := 2"
        assertThat(result.filteredSource).isEqualTo(expectedFiltered)

        val logicalOffset = result.filteredSource.indexOf('x')
        val mappedOffset = result.sourceMap.toSourceOffset(logicalOffset)
        assertThat(mappedOffset).isEqualTo(source.indexOf('x'))

        val logicalRange = XbTextRange(logicalOffset, logicalOffset + 1)
        val mappedRange = result.sourceMap.toSourceRange(logicalRange)
        assertThat(mappedRange).isEqualTo(XbTextRange(source.indexOf('x'), source.indexOf('x') + 1))
    }

    @Test
    fun `preserves monotonic source ranges across directives`() {
        val source = """
            #include "mydefs.ch"

            FUNCTION parse_input2( cSource )

               RETURN parse_input( cSource )

            END FUNCTION
        """.trimIndent()

        val result = XbLexer().lex(source)

        val ranges = result.tokens.map { it.range }
        val starts = ranges.map { it.startOffset }
        val ends = ranges.map { it.endOffset }

        assertThat(starts).isSorted
        assertThat(ends).isSorted
    }
}
