package com.prestoxbasopp.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerTokenizationTest {
    @Test
    fun `tokenizes hash as not-equal operator`() {
        val tokens = XbLexer("a # b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.NEQ,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("!=")
    }

    @Test
    fun `tokenizes at symbol for at-say-get statements`() {
        val tokens = XbLexer("@ 1, 2 SAY \"Hi\" GET value").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.AT,
            TokenType.NUMBER,
            TokenType.COMMA,
            TokenType.NUMBER,
            TokenType.SAY,
            TokenType.STRING,
            TokenType.GET,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens.first().lexeme).isEqualTo("@")
    }

    @Test
    fun `reports error token for bare exclamation mark`() {
        val tokens = XbLexer("!").lex().filter { it.type != TokenType.EOF }

        val token = tokens.single()
        assertThat(token.type).isEqualTo(TokenType.ERROR)
        assertThat(token.lexeme).isEqualTo("!")
    }
}
