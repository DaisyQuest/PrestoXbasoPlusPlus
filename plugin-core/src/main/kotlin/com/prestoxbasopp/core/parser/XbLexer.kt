package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbSourceOffsetMapping

class XbLexer(
    private val source: String,
    private val sourceMap: XbSourceOffsetMapping = XbSourceOffsetMapping.identity(source.length),
) {
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
        val errorToken = skipWhitespaceAndComments()
        if (errorToken != null) {
            return errorToken
        }
        if (isAtEnd()) {
            val mapped = mapRange(index, index)
            return Token(TokenType.EOF, "", mapped.startOffset, mapped.endOffset)
        }
        val start = index
        val current = advance()
        return when {
            current.isLetter() || current == '_' -> readIdentifier(start)
            current.isDigit() -> readNumber(start)
            current == '"' -> readString(start)
            current == '+' -> token(TokenType.PLUS, "+", start, index)
            current == '-' -> token(TokenType.MINUS, "-", start, index)
            current == '*' -> token(TokenType.STAR, "*", start, index)
            current == '/' -> token(TokenType.SLASH, "/", start, index)
            current == '%' -> token(TokenType.PERCENT, "%", start, index)
            current == '(' -> token(TokenType.LPAREN, "(", start, index)
            current == ')' -> token(TokenType.RPAREN, ")", start, index)
            current == '[' -> token(TokenType.LBRACKET, "[", start, index)
            current == ']' -> token(TokenType.RBRACKET, "]", start, index)
            current == '{' -> token(TokenType.LBRACE, "{", start, index)
            current == '}' -> token(TokenType.RBRACE, "}", start, index)
            current == ';' -> token(TokenType.SEMICOLON, ";", start, index)
            current == ',' -> token(TokenType.COMMA, ",", start, index)
            current == '?' -> token(TokenType.QUESTION, "?", start, index)
            current == '$' -> token(TokenType.CONTAINS, "$", start, index)
            current == '&' -> token(TokenType.AMP, "&", start, index)
            current == '|' -> token(TokenType.PIPE, "|", start, index)
            current == '@' -> token(TokenType.AT, "@", start, index)
            current == '#' -> token(TokenType.NEQ, "!=", start, index)
            current == '.' -> readDotKeyword(start)
            current == '=' -> {
                if (match('>')) {
                    token(TokenType.ARROW, "=>", start, index)
                } else if (match('=')) {
                    token(TokenType.EQ, "==", start, index)
                } else {
                    token(TokenType.EQ, "=", start, index)
                }
            }
            current == ':' -> {
                if (match('=')) {
                    token(TokenType.ASSIGN, ":=", start, index)
                } else {
                    token(TokenType.ERROR, ":", start, index)
                }
            }
            current == '!' -> {
                if (match('=')) {
                    token(TokenType.NEQ, "!=", start, index)
                } else {
                    token(TokenType.ERROR, "!", start, index)
                }
            }
            current == '<' -> {
                if (match('=')) {
                    token(TokenType.LTE, "<=", start, index)
                } else {
                    token(TokenType.LT, "<", start, index)
                }
            }
            current == '>' -> {
                if (match('=')) {
                    token(TokenType.GTE, ">=", start, index)
                } else {
                    token(TokenType.GT, ">", start, index)
                }
            }
            else -> token(TokenType.ERROR, current.toString(), start, index)
        }
    }

    private fun readIdentifier(start: Int): Token {
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }
        val lexeme = source.substring(start, index)
        val type = keywordType(lexeme)
        return token(type, lexeme, start, index)
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
        return token(TokenType.NUMBER, lexeme, start, index)
    }

    private fun readString(start: Int): Token {
        val builder = StringBuilder()
        while (!isAtEnd()) {
            val next = peek()
            if (next == '"') {
                if (peekNext() == '"') {
                    advance()
                    advance()
                    builder.append('"')
                    continue
                }
                break
            }
            if (next == '\n') {
                break
            }
            builder.append(advance())
        }
        if (isAtEnd() || peek() != '"') {
            val lexeme = source.substring(start, index)
            return token(TokenType.ERROR, lexeme, start, index)
        }
        advance()
        return token(TokenType.STRING, builder.toString(), start, index)
    }

    private fun keywordType(text: String): TokenType {
        return when (text.lowercase()) {
            "if" -> TokenType.IF
            "then" -> TokenType.THEN
            "else" -> TokenType.ELSE
            "elseif" -> TokenType.ELSEIF
            "endif" -> TokenType.ENDIF
            "while" -> TokenType.WHILE
            "do" -> TokenType.DO
            "enddo" -> TokenType.ENDDO
            "return" -> TokenType.RETURN
            "wait" -> TokenType.WAIT
            "exit" -> TokenType.EXIT
            "begin" -> TokenType.BEGIN
            "sequence" -> TokenType.SEQUENCE
            "recover" -> TokenType.RECOVER
            "using" -> TokenType.USING
            "break" -> TokenType.BREAK
            "end" -> TokenType.END
            "say" -> TokenType.SAY
            "get" -> TokenType.GET
            "valid" -> TokenType.VALID
            "function" -> TokenType.FUNCTION
            "procedure" -> TokenType.PROCEDURE
            "endfunction" -> TokenType.ENDFUNCTION
            "endfunc" -> TokenType.ENDFUNCTION
            "endproc" -> TokenType.ENDPROC
            "endprocedure" -> TokenType.ENDPROC
            "local" -> TokenType.LOCAL
            "for" -> TokenType.FOR
            "to" -> TokenType.TO
            "step" -> TokenType.STEP
            "next" -> TokenType.NEXT
            "nil" -> TokenType.NIL
            "and" -> TokenType.AND
            "or" -> TokenType.OR
            "not" -> TokenType.NOT
            ".and." -> TokenType.AND
            ".or." -> TokenType.OR
            ".not." -> TokenType.NOT
            ".t." -> TokenType.TRUE
            ".f." -> TokenType.FALSE
            else -> TokenType.IDENTIFIER
        }
    }

    private fun skipWhitespaceAndComments(): Token? {
        while (true) {
            while (!isAtEnd()) {
                val char = peek()
                if (char == ' ' || char == '\r' || char == '\t' || char == '\n') {
                    index++
                } else {
                    break
                }
            }
            if (isAtEnd()) {
                return null
            }
            if (peek() == '/' && peekNext() == '/') {
                index += 2
                while (!isAtEnd() && peek() != '\n') {
                    index++
                }
                continue
            }
            if (peek() == '/' && peekNext() == '*') {
                val start = index
                index += 2
                while (!isAtEnd() && !(peek() == '*' && peekNext() == '/')) {
                    index++
                }
                if (isAtEnd()) {
                    val lexeme = source.substring(start, index)
                    return token(TokenType.ERROR, lexeme, start, index)
                }
                index += 2
                continue
            }
            if (peek() == ';' && isLineContinuation(index)) {
                index++
                while (!isAtEnd() && peek() != '\n') {
                    index++
                }
                continue
            }
            return null
        }
    }

    private fun readDotKeyword(start: Int): Token {
        val end = source.indexOf('.', start + 1)
        if (end == -1) {
            return token(TokenType.ERROR, ".", start, index)
        }
        val lexeme = source.substring(start, end + 1)
        index = end + 1
        val type = keywordType(lexeme)
        if (type == TokenType.IDENTIFIER) {
            return token(TokenType.ERROR, lexeme, start, index)
        }
        val normalized = when (type) {
            TokenType.AND -> "and"
            TokenType.OR -> "or"
            TokenType.NOT -> "not"
            TokenType.TRUE -> "true"
            TokenType.FALSE -> "false"
            else -> lexeme
        }
        return token(type, normalized, start, index)
    }

    private fun isLineContinuation(startIndex: Int): Boolean {
        var cursor = startIndex + 1
        while (cursor < source.length) {
            val char = source[cursor]
            if (char == '\n') {
                return !isBlankLine(cursor + 1)
            }
            if (!char.isWhitespace()) {
                return false
            }
            cursor++
        }
        return true
    }

    private fun isBlankLine(startIndex: Int): Boolean {
        var cursor = startIndex
        while (cursor < source.length) {
            val char = source[cursor]
            if (char == '\n') {
                return true
            }
            if (!char.isWhitespace()) {
                return false
            }
            cursor++
        }
        return true
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

    private fun mapRange(start: Int, end: Int): XbTextRange {
        val mapped = sourceMap.toSourceRange(XbTextRange(start, end))
        return mapped ?: XbTextRange(start, end)
    }

    private fun token(type: TokenType, lexeme: String, start: Int, end: Int): Token {
        val mapped = mapRange(start, end)
        return Token(type, lexeme, mapped.startOffset, mapped.endOffset)
    }
}
