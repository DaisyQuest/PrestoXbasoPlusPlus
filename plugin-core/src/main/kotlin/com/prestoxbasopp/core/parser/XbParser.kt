package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbArrayLiteralExpression
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbIndexExpression
import com.prestoxbasopp.core.ast.XbLocalBinding
import com.prestoxbasopp.core.ast.XbLocalDeclarationStatement
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbLiteralKind
import com.prestoxbasopp.core.ast.XbPrintStatement
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
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
            TokenType.FUNCTION -> parseFunctionDeclaration()
            TokenType.PROCEDURE -> parseProcedureDeclaration()
            TokenType.LOCAL -> parseLocalDeclaration()
            TokenType.FOR -> parseForStatement()
            TokenType.IF -> parseIfStatement()
            TokenType.WHILE -> parseWhileStatement()
            TokenType.RETURN -> parseReturnStatement()
            TokenType.QUESTION -> parsePrintStatement()
            TokenType.SEMICOLON -> {
                advance()
                null
            }
            TokenType.ENDIF, TokenType.ELSE, TokenType.ENDDO, TokenType.ENDFUNCTION, TokenType.ENDPROC, TokenType.NEXT -> {
                val token = advance()
                recordError("Unexpected ${token.type.name} at ${token.startOffset}")
                null
            }
            TokenType.EOF -> null
            TokenType.IDENTIFIER -> {
                if (checkNext(TokenType.ASSIGN)) {
                    parseAssignmentStatement()
                } else {
                    parseExpressionStatement()
                }
            }
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

    private fun parseAssignmentStatement(): XbStatement? {
        val targetToken = peek()
        val target = parseAssignmentTarget() ?: run {
            recordError("Expected assignment target at ${targetToken.startOffset}")
            synchronize()
            return null
        }
        if (!match(TokenType.ASSIGN)) {
            recordError("Expected ':=' after assignment target at ${peek().startOffset}")
        }
        val value = parseExpression(0) ?: run {
            recordError("Expected expression after ':=' at ${peek().startOffset}")
            fallbackExpression(targetToken)
        }
        match(TokenType.SEMICOLON)
        val range = rangeFromOffsets(target.range.startOffset, value.range.endOffset)
        return XbAssignmentStatement(target, value, range)
    }

    private fun parsePrintStatement(): XbStatement {
        val question = advance()
        val expressions = mutableListOf<XbExpression>()
        if (!isAtEnd() && !isTerminator(peek().type)) {
            parseExpression(0)?.let { expressions += it } ?: run {
                recordError("Expected expression after '?' at ${peek().startOffset}")
                expressions += fallbackExpression(question)
            }
            while (match(TokenType.COMMA)) {
                while (match(TokenType.SEMICOLON)) {
                    // Allow line continuation between print arguments.
                }
                parseExpression(0)?.let { expressions += it } ?: run {
                    recordError("Expected expression after ',' at ${peek().startOffset}")
                    expressions += fallbackExpression(question)
                }
            }
        }
        match(TokenType.SEMICOLON)
        val endToken = previousOr(question)
        return XbPrintStatement(expressions, rangeFrom(question, endToken))
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

    private fun parseLocalDeclaration(): XbStatement {
        val localToken = advance()
        val bindings = mutableListOf<XbLocalBinding>()
        do {
            val nameToken = if (match(TokenType.IDENTIFIER)) previous() else null
            if (nameToken == null) {
                recordError("Expected identifier after LOCAL at ${peek().startOffset}")
                break
            }
            val initializer = if (match(TokenType.ASSIGN)) {
                parseExpression(0) ?: run {
                    recordError("Expected expression after ':=' at ${peek().startOffset}")
                    fallbackExpression(nameToken)
                }
            } else {
                null
            }
            val bindingRange = rangeFromOffsets(
                nameToken.startOffset,
                initializer?.range?.endOffset ?: nameToken.endOffset,
            )
            bindings += XbLocalBinding(nameToken.lexeme, initializer, bindingRange)
        } while (match(TokenType.COMMA))
        match(TokenType.SEMICOLON)
        val endToken = previousOr(localToken)
        return XbLocalDeclarationStatement(bindings, rangeFrom(localToken, endToken))
    }

    private fun parseIfStatement(): XbStatement {
        val ifToken = advance()
        val condition = parseExpression(0) ?: run {
            recordError("Expected condition after IF at ${peek().startOffset}")
            synchronizeTo(setOf(TokenType.THEN, TokenType.ELSE, TokenType.ENDIF))
            fallbackExpression(ifToken)
        }
        match(TokenType.THEN)
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
        match(TokenType.DO)
        val body = parseBlock(setOf(TokenType.ENDDO))
        if (!match(TokenType.ENDDO)) {
            recordError("Expected ENDDO to close WHILE at ${peek().startOffset}")
        }
        val endToken = previousOr(whileToken)
        return XbWhileStatement(condition, body, rangeFrom(whileToken, endToken))
    }

    private fun parseForStatement(): XbStatement {
        val forToken = advance()
        val iteratorToken = if (match(TokenType.IDENTIFIER)) previous() else null
        if (iteratorToken == null) {
            recordError("Expected iterator after FOR at ${peek().startOffset}")
            return XbForStatement(
                iterator = XbIdentifierExpression("<error>", rangeFrom(forToken, forToken)),
                start = fallbackExpression(forToken),
                end = fallbackExpression(forToken),
                step = XbLiteralExpression(XbLiteralKind.NUMBER, "1", rangeFrom(forToken, forToken)),
                body = XbBlock(emptyList(), rangeFrom(forToken, forToken)),
                range = rangeFrom(forToken, previousOr(forToken)),
            )
        }
        if (!match(TokenType.ASSIGN)) {
            recordError("Expected ':=' after FOR iterator at ${peek().startOffset}")
        }
        val startExpr = parseExpression(0) ?: run {
            recordError("Expected start expression after ':=' at ${peek().startOffset}")
            fallbackExpression(iteratorToken)
        }
        if (!match(TokenType.TO)) {
            recordError("Expected TO in FOR statement at ${peek().startOffset}")
        }
        val endExpr = parseExpression(0) ?: run {
            recordError("Expected end expression after TO at ${peek().startOffset}")
            fallbackExpression(iteratorToken)
        }
        val stepExpr = if (match(TokenType.STEP)) {
            parseExpression(0) ?: run {
                recordError("Expected step expression after STEP at ${peek().startOffset}")
                fallbackExpression(iteratorToken)
            }
        } else {
            XbLiteralExpression(XbLiteralKind.NUMBER, "1", rangeFrom(iteratorToken, iteratorToken))
        }
        val body = parseBlock(setOf(TokenType.NEXT))
        if (!match(TokenType.NEXT)) {
            recordError("Expected NEXT to close FOR at ${peek().startOffset}")
        }
        val endToken = previousOr(forToken)
        return XbForStatement(
            iterator = XbIdentifierExpression(iteratorToken.lexeme, rangeFrom(iteratorToken, iteratorToken)),
            start = startExpr,
            end = endExpr,
            step = stepExpr,
            body = body,
            range = rangeFrom(forToken, endToken),
        )
    }

    private fun parseFunctionDeclaration(): XbStatement {
        val functionToken = advance()
        val nameToken = if (match(TokenType.IDENTIFIER)) previous() else null
        if (nameToken == null) {
            recordError("Expected function name after FUNCTION at ${peek().startOffset}")
        }
        val parameters = parseParameterList()
        val body = parseBlock(setOf(TokenType.ENDFUNCTION))
        if (!match(TokenType.ENDFUNCTION)) {
            recordError("Expected ENDFUNCTION to close FUNCTION at ${peek().startOffset}")
        }
        val endToken = previousOr(functionToken)
        return XbFunctionDeclaration(
            name = nameToken?.lexeme ?: "<error>",
            parameters = parameters,
            body = body,
            range = rangeFrom(functionToken, endToken),
        )
    }

    private fun parseProcedureDeclaration(): XbStatement {
        val procedureToken = advance()
        val nameToken = if (match(TokenType.IDENTIFIER)) previous() else null
        if (nameToken == null) {
            recordError("Expected procedure name after PROCEDURE at ${peek().startOffset}")
        }
        val parameters = parseParameterList()
        val body = parseBlock(setOf(TokenType.ENDPROC))
        if (!match(TokenType.ENDPROC)) {
            recordError("Expected ENDPROC to close PROCEDURE at ${peek().startOffset}")
        }
        val endToken = previousOr(procedureToken)
        return XbProcedureDeclaration(
            name = nameToken?.lexeme ?: "<error>",
            parameters = parameters,
            body = body,
            range = rangeFrom(procedureToken, endToken),
        )
    }

    private fun parseParameterList(): List<String> {
        if (!match(TokenType.LPAREN)) {
            recordError("Expected '(' to start parameter list at ${peek().startOffset}")
            return emptyList()
        }
        val parameters = mutableListOf<String>()
        if (!check(TokenType.RPAREN)) {
            do {
                if (match(TokenType.IDENTIFIER)) {
                    parameters += previous().lexeme
                } else {
                    recordError("Expected parameter name at ${peek().startOffset}")
                    break
                }
            } while (match(TokenType.COMMA))
        }
        if (!match(TokenType.RPAREN)) {
            recordError("Expected ')' after parameter list at ${peek().startOffset}")
        }
        return parameters
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
        left = parsePostfix(left)
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
            TokenType.NIL -> XbLiteralExpression(XbLiteralKind.NIL, "nil", rangeFrom(token, token))
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
            TokenType.LBRACE -> {
                val elements = mutableListOf<XbExpression>()
                if (!check(TokenType.RBRACE)) {
                    do {
                        parseExpression(0)?.let { elements += it } ?: run {
                            recordError("Expected expression in array literal at ${peek().startOffset}")
                            elements += fallbackExpression(token)
                        }
                    } while (match(TokenType.COMMA))
                }
                if (!match(TokenType.RBRACE)) {
                    recordError("Expected '}' after array literal at ${peek().startOffset}")
                }
                val endToken = previousOr(token)
                XbArrayLiteralExpression(elements, rangeFrom(token, endToken))
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

    private fun parsePostfix(expression: XbExpression): XbExpression {
        var currentExpression = expression
        while (true) {
            currentExpression = when (peek().type) {
                TokenType.LPAREN -> {
                    advance()
                    val arguments = mutableListOf<XbExpression>()
                    if (!check(TokenType.RPAREN)) {
                        do {
                            parseExpression(0)?.let { arguments += it } ?: run {
                                recordError("Expected expression in argument list at ${peek().startOffset}")
                                arguments += fallbackExpression(previous())
                            }
                        } while (match(TokenType.COMMA))
                    }
                    if (!match(TokenType.RPAREN)) {
                        recordError("Expected ')' after arguments at ${peek().startOffset}")
                    }
                    val endToken = previousOr(lastTokenFrom(currentExpression.range))
                    XbCallExpression(
                        callee = currentExpression,
                        arguments = arguments,
                        range = rangeFromOffsets(currentExpression.range.startOffset, endToken.endOffset),
                    )
                }
                TokenType.LBRACKET -> {
                    advance()
                    val indexExpr = parseExpression(0) ?: run {
                        recordError("Expected expression inside indexer at ${peek().startOffset}")
                        fallbackExpression(previous())
                    }
                    if (!match(TokenType.RBRACKET)) {
                        recordError("Expected ']' after index expression at ${peek().startOffset}")
                    }
                    val endToken = previousOr(lastTokenFrom(indexExpr.range))
                    XbIndexExpression(
                        target = currentExpression,
                        index = indexExpr,
                        range = rangeFromOffsets(currentExpression.range.startOffset, endToken.endOffset),
                    )
                }
                else -> return currentExpression
            }
        }
    }

    private fun parseAssignmentTarget(): XbExpression? {
        if (!match(TokenType.IDENTIFIER)) {
            return null
        }
        var target: XbExpression = XbIdentifierExpression(previous().lexeme, rangeFrom(previous(), previous()))
        while (match(TokenType.LBRACKET)) {
            val indexExpr = parseExpression(0) ?: run {
                recordError("Expected expression inside indexer at ${peek().startOffset}")
                fallbackExpression(previous())
            }
            if (!match(TokenType.RBRACKET)) {
                recordError("Expected ']' after index expression at ${peek().startOffset}")
            }
            val endToken = previousOr(lastTokenFrom(indexExpr.range))
            target = XbIndexExpression(
                target = target,
                index = indexExpr,
                range = rangeFromOffsets(target.range.startOffset, endToken.endOffset),
            )
        }
        return target
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

    private fun checkNext(type: TokenType): Boolean {
        if (current + 1 >= tokens.size) return false
        return tokens[current + 1].type == type
    }

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
            type == TokenType.ENDFUNCTION ||
            type == TokenType.ENDPROC ||
            type == TokenType.NEXT ||
            type == TokenType.EOF
    }

    companion object {
        private const val PREFIX_PRECEDENCE = 7
        private val SYNC_TOKENS = setOf(
            TokenType.SEMICOLON,
            TokenType.ENDIF,
            TokenType.ELSE,
            TokenType.ENDDO,
            TokenType.ENDFUNCTION,
            TokenType.ENDPROC,
            TokenType.NEXT,
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
