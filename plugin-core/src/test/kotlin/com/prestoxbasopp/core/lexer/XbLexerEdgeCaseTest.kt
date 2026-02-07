package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerEdgeCaseTest {
    @Test
    fun `reports unterminated block comments`() {
        val source = "/* unterminated"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unterminated block comment")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text == source }
    }

    @Test
    fun `reports unexpected characters`() {
        val source = "@"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unexpected character '@'")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "@" }
    }

    @Test
    fun `lexes dot edge cases for numbers and punctuation`() {
        val source = "a . b .5 1."
        val result = XbLexer().lex(source)

        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "." }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == ".5" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == "1." }
    }

    @Test
    fun `accepts escaped quotes inside strings`() {
        val source = "\"hello \\\"world\\\"\""
        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == source }
    }

    @Test
    fun `reports invalid date formats`() {
        val source = "{^2024-1-02}"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Invalid date literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == source }
    }

    @Test
    fun `lexes multi character operators`() {
        val source = "a==b != c <= d >= e && f || g :: h -> i"
        val result = XbLexer().lex(source)

        val operatorTexts = result.tokens.filter { it.type == XbTokenType.OPERATOR }.map { it.text }
        assertThat(operatorTexts).contains("==", "!=", "<=", ">=", "&&", "||", "::", "->")
    }

    @Test
    fun `reports unterminated date literals`() {
        val source = "{^2024-01-02"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unterminated date literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.DATE && it.text == source }
    }

    @Test
    fun `reports empty source without directives`() {
        val result = XbLexer().lex("")

        assertThat(result.tokens).containsExactly(XbToken(XbTokenType.EOF, "", result.tokens.first().range))
        assertThat(result.directives).isEmpty()
        assertThat(result.filteredSource).isEmpty()
    }
}
