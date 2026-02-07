package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerLiteralErrorTest {
    @Test
    fun `reports unterminated string`() {
        val source = "\"unterminated"
        val result = XbLexer().lex(source)

        val messages = result.errors.map { it.message }
        assertThat(messages).contains("Unterminated string literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING }
    }

    @Test
    fun `reports unterminated codeblock`() {
        val source = "{|missing"
        val result = XbLexer().lex(source)

        val messages = result.errors.map { it.message }
        assertThat(messages).contains("Unterminated codeblock literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.CODEBLOCK }
    }

    @Test
    fun `reports invalid date and hex literals`() {
        val source = "{^2024-13-40} 0x"
        val result = XbLexer().lex(source)

        val messages = result.errors.map { it.message }
        assertThat(messages).contains(
            "Invalid date literal",
            "Invalid hex literal",
        )
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "{^2024-13-40}" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "0x" }
    }

    @Test
    fun `reports invalid exponent`() {
        val source = "1e+"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Invalid exponent")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "1e+" }
    }

    @Test
    fun `reports empty symbol literal`() {
        val source = "a #"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Empty symbol literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "#" }
    }
}
