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

    @Test
    fun `tokenizes single quoted string with backslash`() {
        val source = "cPath += '\\\\'"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.PLUS,
            TokenType.EQ,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("\\\\")
    }

    @Test
    fun `tokenizes hash not equal operator in conditional expression`() {
        val source = "IF valtype(soSatOK) # \"L\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IF,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.RPAREN,
            TokenType.NEQ,
            TokenType.STRING,
        )
        assertThat(tokens[5].lexeme).isEqualTo("!=")
    }

    @Test
    fun `tokenizes slash as division operator when not part of comment`() {
        val tokens = XbLexer("a / b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.SLASH,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("/")
    }

    @Test
    fun `skips slash comments without producing slash token`() {
        val tokens = XbLexer("a // trailing comment\n/ b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.SLASH,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("/")
    }

    @Test
    fun `returns error token for unterminated block comment that starts with slash`() {
        val token = XbLexer("/* unterminated").lex().first { it.type != TokenType.EOF }

        assertThat(token.type).isEqualTo(TokenType.ERROR)
        assertThat(token.lexeme).isEqualTo("/* unterminated")
    }

}
