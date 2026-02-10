package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerCodeblockTest {
    @Test
    fun `lexes codeblock with nested braces and strings`() {
        val source = """
            {|n| IIF( n % 2 == 0, ;
                ( {|m| m*m } )(n), ;
                ( {|m| m+m } )(n) ) }
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.CODEBLOCK && it.text == source }
    }

    @Test
    fun `lexes codeblock containing comments and nested literals`() {
        val source = """
            {|x| /* ignore } */ ;
                QOut( "brace: }" ) ;
                a := { 1, 2, {|| "inner" } } ;
                // line comment with }
                RETURN x }
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.CODEBLOCK && it.text == source }
    }
}
