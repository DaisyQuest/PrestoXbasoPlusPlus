package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbLiteralKind
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWhileStatement

data class XbParseResult(
    val program: XbProgram?,
    val errors: List<String>,
)

class XbParser(private val tokens: List<Token>) {
    private var current = 0
    private val errors = mutableListOf<String>()

    fun parseProgram(): XbParseResult {
        val start = peek()
        val statements = mutableListOf<XbStatement>()
        while (!isAtEnd()) {
            val before = current
            val statement = parseStatement()
            if (statement != null) {
                statements += statement
            } else if (!isAtEnd() && current == before) {
                advance()
            }
        }
        val end = previousOr(start)
        val programRange = rangeFrom(start, end)
        return XbParseResult(XbProgram(statements, programRange), errors.toList())
    }

    private fun parseStatement(): XbStatement? {
        return when (peek().type) {
            TokenType.IF -> parseIfStatement()
            TokenType.WHILE -> parseWhileStatement()
            TokenType.RETURN -> parseReturnStatement()
            TokenType.SEMICOLON -> {
                advance()
                null
            }
            TokenType.ENDIF, TokenType.ELSE, TokenType.ENDDO -> {
                val token = advance()
                recordError("Unexpected ${token.type.name} at ${token.startOffset}")
                null
            }
            TokenType.EOF -> null
            else -> parseExpressionStatement()
        }
    }

    private fun parseExpressionStatement(): XbStatement? {
        val expr = parseExpression(0) ?: run {
            synchronize()
            return null
        }
        val endToken = if (match(TokenType.SEMICOLON)) previous() else lastTokenFrom(expr.range)
        return XbExpressionStatement(expr, rangeFromOffset(expr.range.startOffset, endToken.endOffset))
    }

    private fun parseReturnStatement(): XbStatement {
        val returnToken = advance()
        val expression = if (isAtEnd() || isTerminator(peek().type)) {
            null
        } else {
            parseExpression(0).also { expr ->
                if (expr == null) {
                    recordError("Expected expression after RETURN at ${peek().startOffset}")
                    synchronize()
                }
            }
        }
        match(TokenType.SEMICOLON)
        val endToken = previousOr(returnToken)
        val range = rangeFrom(returnToken, endToken)
        return XbReturnStatement(expression, range)
    }

    private fun parseIfStatement(): XbStatement {
        val ifToken = advance()
        val condition = parseExpression(0) ?: run {
            recordError("Expected condition after IF at ${peek().startOffset}")
            synchronizeTo(setOf(TokenType.THEN, TokenType.ELSE, TokenType.ENDIF))
            fallbackExpression(ifToken)
        }
        if (!match(TokenType.THEN)) {
            recordError("Expected THEN after IF condition at ${peek().startOffset}")
        }
        val thenBlock = parseBlock(setOf(TokenType.ELSE, TokenType.ENDIF))
        val elseBlock = if (match(TokenType.ELSE)) {
            parseBlock(setOf(TokenType.ENDIF))
        } else {
            null
        }
        if (!match(TokenType.ENDIF)) {
            recordError("Expected ENDIF to close IF at ${peek().startOffset}")
        }
        val endToken = previousOr(ifToken)
        return XbIfStatement(condition, thenBlock, elseBlock, rangeFrom(ifToken, endToken))
    }

    private fun parseWhileStatement(): XbStatement {
        val whileToken = advance()
        val condition = parseExpression(0) ?: run {
            recordError("Expected condition after WHILE at ${peek().startOffset}")
            synchronizeTo(setOf(TokenType.DO, TokenType.ENDDO))
            fallbackExpression(whileToken)
        }
        if (!match(TokenType.DO)) {
            recordError("Expected DO after WHILE condition at ${peek().startOffset}")
        }
        val body = parseBlock(setOf(TokenType.ENDDO))
        if (!match(TokenType.ENDDO)) {
            recordError("Expected ENDDO to close WHILE at ${peek().startOffset}")
        }
        val endToken = previousOr(whileToken)
        return XbWhileStatement(condition, body, rangeFrom(whileToken, endToken))
    }

    private fun parseBlock(terminators: Set<TokenType>): XbBlock {
        val start = peek()
        val startIndex = current
        val statements = mutableListOf<XbStatement>()
        while (!isAtEnd() && peek().type !in terminators) {
            val before = current
            val statement = parseStatement()
            if (statement != null) {
                statements += statement
            } else if (!isAtEnd() && current == before) {
                advance()
            }
        }
        val end = if (current == startIndex) start else previousOr(start)
        return XbBlock(statements, rangeFrom(start, end))
    }

    private fun parseExpression(minPrecedence: Int): XbExpression? {
        var left = parsePrefix() ?: return null
        while (true) {
            val precedence = infixPrecedence(peek().type) ?: break
            if (precedence < minPrecedence) break
            val operatorToken = advance()
            val right = parseExpression(precedence + 1) ?: run {
                recordError("Expected expression after '${operatorToken.lexeme}' at ${peek().startOffset}")
                fallbackExpression(operatorToken)
            }
            left = XbBinaryExpression(
                operator = operatorToken.lexeme,
                left = left,
                right = right,
                range = rangeFromOffsets(left.range.startOffset, right.range.endOffset),
            )
        }
        return left
    }

    private fun parsePrefix(): XbExpression? {
        val token = advance()
        return when (token.type) {
            TokenType.NUMBER -> XbLiteralExpression(XbLiteralKind.NUMBER, token.lexeme, rangeFrom(token, token))
            TokenType.STRING -> XbLiteralExpression(XbLiteralKind.STRING, token.lexeme, rangeFrom(token, token))
            TokenType.IDENTIFIER -> XbIdentifierExpression(token.lexeme, rangeFrom(token, token))
            TokenType.MINUS, TokenType.NOT -> {
                val operator = token.lexeme
                val expression = parseExpression(PREFIX_PRECEDENCE) ?: run {
                    recordError("Expected expression after unary '$operator' at ${peek().startOffset}")
                    fallbackExpression(token)
                }
                XbUnaryExpression(
                    operator = operator,
                    expression = expression,
                    range = rangeFromOffsets(token.startOffset, expression.range.endOffset),
                )
            }
            TokenType.LPAREN -> {
                val expression = parseExpression(0) ?: run {
                    recordError("Expected expression after '(' at ${peek().startOffset}")
                    fallbackExpression(token)
                }
                if (!match(TokenType.RPAREN)) {
                    recordError("Expected ')' after expression at ${peek().startOffset}")
                }
                expression
            }
            TokenType.ERROR -> {
                recordError("Unexpected token '${token.lexeme}' at ${token.startOffset}")
                null
            }
            else -> {
                recordError("Unexpected token ${token.type} at ${token.startOffset}")
                null
            }
        }
    }

    private fun infixPrecedence(type: TokenType): Int? {
        return when (type) {
            TokenType.OR -> 1
            TokenType.AND -> 2
            TokenType.EQ, TokenType.NEQ -> 3
            TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE -> 4
            TokenType.PLUS, TokenType.MINUS -> 5
            TokenType.STAR, TokenType.SLASH -> 6
            else -> null
        }
    }

    private fun synchronize() {
        synchronizeTo(SYNC_TOKENS)
    }

    private fun synchronizeTo(tokens: Set<TokenType>) {
        while (!isAtEnd() && peek().type !in tokens) {
            advance()
        }
        if (match(TokenType.SEMICOLON)) {
            return
        }
    }

    private fun recordError(message: String) {
        errors += message
    }

    private fun match(type: TokenType): Boolean {
        if (check(type)) {
            advance()
            return true
        }
        return false
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private fun previousOr(fallback: Token): Token = if (current > 0) previous() else fallback

    private fun lastTokenFrom(range: XbTextRange): Token {
        return Token(TokenType.ERROR, "", range.startOffset, range.endOffset)
    }

    private fun rangeFrom(start: Token, end: Token): XbTextRange {
        return XbTextRange(start.startOffset, end.endOffset)
    }

    private fun rangeFromOffsets(startOffset: Int, endOffset: Int): XbTextRange {
        return XbTextRange(startOffset, endOffset)
    }

    private fun rangeFromOffset(startOffset: Int, endOffset: Int): XbTextRange {
        return XbTextRange(startOffset, endOffset)
    }

    private fun fallbackExpression(token: Token): XbExpression {
        return XbIdentifierExpression(
            name = "<error>",
            range = rangeFromOffsets(token.startOffset, token.endOffset),
        )
    }

    private fun isTerminator(type: TokenType): Boolean {
        return type == TokenType.SEMICOLON ||
            type == TokenType.ENDIF ||
            type == TokenType.ELSE ||
            type == TokenType.ENDDO ||
            type == TokenType.EOF
    }

    companion object {
        private const val PREFIX_PRECEDENCE = 7
        private val SYNC_TOKENS = setOf(
            TokenType.SEMICOLON,
            TokenType.ENDIF,
            TokenType.ELSE,
            TokenType.ENDDO,
            TokenType.EOF,
        )

        fun parse(source: String): XbParseResult {
            val preprocess = com.prestoxbasopp.core.lexer.XbPreprocessor.preprocess(source)
            val lexer = XbLexer(preprocess.filteredSource, preprocess.sourceMap)
            val tokens = lexer.lex()
            val parser = XbParser(tokens)
            return parser.parseProgram()
        }
    }
}
