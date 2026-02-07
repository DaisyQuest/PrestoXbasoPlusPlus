package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerEdgeCaseTest {
    @Test
    fun `lexes escaped quotes inside strings`() {
        val source = "\"a\\\"b\" 'c\\'d'"
        val result = XbLexer().lex(source)

        val strings = result.tokens.filter { it.type == XbTokenType.STRING }.map { it.text }
        assertThat(strings).containsExactly("\"a\\\"b\"", "'c\\'d'")
        assertThat(result.errors).isEmpty()
    }

    @Test
    fun `lexes leading and trailing dot numbers`() {
        val source = ".5 1. 2.e3"
        val result = XbLexer().lex(source)

        val numbers = result.tokens.filter { it.type == XbTokenType.NUMBER }.map { it.text }
        assertThat(numbers).containsExactly(".5", "1.", "2.e3")
        assertThat(result.errors).isEmpty()
    }

    @Test
    fun `reports unexpected characters`() {
        val source = "@"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unexpected character '@'")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "@" }
    }

    @Test
    fun `captures directive without a name`() {
        val source = "#\nx := 1"
        val result = XbLexer().lex(source)

        assertThat(result.directives).hasSize(1)
        assertThat(result.directives.first().name).isEmpty()
        assertThat(result.directives.first().text).isEqualTo("#")
        assertThat(result.filteredSource).isEqualTo("\nx := 1")
    }

    @Test
    fun `reports unterminated block comment`() {
        val source = "/* missing"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unterminated block comment")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text.startsWith("/*") }
    }
}
