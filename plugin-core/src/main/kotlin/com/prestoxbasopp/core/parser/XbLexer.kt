package com.prestoxbasopp.core.parser

class XbLexer(private val source: String) {
    private var index = 0

    fun lex(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (true) {
            val token = nextToken()
            tokens += token
            if (token.type == TokenType.EOF) {
                return tokens
            }
        }
    }

    private fun nextToken(): Token {
        skipWhitespace()
        if (isAtEnd()) {
            return Token(TokenType.EOF, "", index, index)
        }
        val start = index
        val current = advance()
        return when {
            current.isLetter() || current == '_' -> readIdentifier(start)
            current.isDigit() -> readNumber(start)
            current == '"' -> readString(start)
            current == '+' -> Token(TokenType.PLUS, "+", start, index)
            current == '-' -> Token(TokenType.MINUS, "-", start, index)
            current == '*' -> Token(TokenType.STAR, "*", start, index)
            current == '/' -> Token(TokenType.SLASH, "/", start, index)
            current == '(' -> Token(TokenType.LPAREN, "(", start, index)
            current == ')' -> Token(TokenType.RPAREN, ")", start, index)
            current == ';' -> Token(TokenType.SEMICOLON, ";", start, index)
            current == ',' -> Token(TokenType.COMMA, ",", start, index)
            current == '=' -> {
                if (match('=')) {
                    Token(TokenType.EQ, "==", start, index)
                } else {
                    Token(TokenType.EQ, "=", start, index)
                }
            }
            current == '!' -> {
                if (match('=')) {
                    Token(TokenType.NEQ, "!=", start, index)
                } else {
                    Token(TokenType.ERROR, "!", start, index)
                }
            }
            current == '<' -> {
                if (match('=')) {
                    Token(TokenType.LTE, "<=", start, index)
                } else {
                    Token(TokenType.LT, "<", start, index)
                }
            }
            current == '>' -> {
                if (match('=')) {
                    Token(TokenType.GTE, ">=", start, index)
                } else {
                    Token(TokenType.GT, ">", start, index)
                }
            }
            else -> Token(TokenType.ERROR, current.toString(), start, index)
        }
    }

    private fun readIdentifier(start: Int): Token {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }
        val lexeme = source.substring(start, index)
        val type = keywordType(lexeme)
        return Token(type, lexeme, start, index)
    }

    private fun readNumber(start: Int): Token {
        while (!isAtEnd() && peek().isDigit()) {
            advance()
        }
        if (!isAtEnd() && peek() == '.' && peekNext().isDigit()) {
            advance()
            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }
        }
        val lexeme = source.substring(start, index)
        return Token(TokenType.NUMBER, lexeme, start, index)
    }

    private fun readString(start: Int): Token {
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') {
                break
            }
            advance()
        }
        if (isAtEnd() || peek() != '"') {
            val lexeme = source.substring(start, index)
            return Token(TokenType.ERROR, lexeme, start, index)
        }
        advance()
        val lexeme = source.substring(start + 1, index - 1)
        return Token(TokenType.STRING, lexeme, start, index)
    }

    private fun keywordType(text: String): TokenType {
        return when (text.lowercase()) {
            "if" -> TokenType.IF
            "then" -> TokenType.THEN
            "else" -> TokenType.ELSE
            "endif" -> TokenType.ENDIF
            "while" -> TokenType.WHILE
            "do" -> TokenType.DO
            "enddo" -> TokenType.ENDDO
            "return" -> TokenType.RETURN
            "and" -> TokenType.AND
            "or" -> TokenType.OR
            "not" -> TokenType.NOT
            else -> TokenType.IDENTIFIER
        }
    }

    private fun skipWhitespace() {
        while (!isAtEnd()) {
            val char = peek()
            if (char == ' ' || char == '\r' || char == '\t' || char == '\n') {
                index++
            } else {
                return
            }
        }
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[index] != expected) return false
        index++
        return true
    }

    private fun peek(): Char = source[index]

    private fun peekNext(): Char = if (index + 1 >= source.length) '\u0000' else source[index + 1]

    private fun advance(): Char = source[index++]

    private fun isAtEnd(): Boolean = index >= source.length
}
