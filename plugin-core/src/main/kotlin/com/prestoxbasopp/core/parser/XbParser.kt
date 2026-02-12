package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbBlockLiteralExpression
import com.prestoxbasopp.core.ast.XbBreakStatement
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbArrayLiteralExpression
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbAtSayGetStatement
import com.prestoxbasopp.core.ast.XbExitStatement
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbHashLiteralExpression
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
import com.prestoxbasopp.core.ast.XbSequenceStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWaitStatement
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
            TokenType.DO -> if (peekNext().type == TokenType.WHILE) {
                parseDoWhileStatement()
            } else {
                parseExpressionStatement()
            }
            TokenType.RETURN -> parseReturnStatement()
            TokenType.WAIT -> parseWaitStatement()
            TokenType.EXIT -> parseExitStatement()
            TokenType.BEGIN -> if (peekNext().type == TokenType.SEQUENCE) {
                parseSequenceStatement()
            } else {
                parseExpressionStatement()
            }
            TokenType.BREAK -> parseBreakStatement()
            TokenType.INDEX -> parseIndexCommandStatement()
            TokenType.AT -> if (!canStartAtSayGetExpression(peekNext().type)) {
                val token = advance()
                recordError("Unexpected token '@' at ${token.startOffset}")
                synchronizeTo(setOf(TokenType.SEMICOLON))
                match(TokenType.SEMICOLON)
                null
            } else {
                parseAtSayGetStatement()
            }
            TokenType.QUESTION -> parsePrintStatement()
            TokenType.SEMICOLON -> {
                advance()
                null
            }
            TokenType.ENDIF,
            TokenType.ELSE,
            TokenType.ELSEIF,
            TokenType.ENDDO,
            TokenType.ENDFUNCTION,
            TokenType.ENDPROC,
            TokenType.NEXT,
            TokenType.RECOVER,
            TokenType.END,
            -> {
                val token = advance()
                recordError("Unexpected ${token.type.name} at ${token.startOffset}")
                null
            }
            TokenType.EOF -> null
            TokenType.IDENTIFIER -> {
                if (isAssignmentStatementStart()) {
                    parseAssignmentStatement()
                } else {
                    parseExpressionStatement()
                }
            }
            else -> parseExpressionStatement()
        }
    }

    private fun parseIndexCommandStatement(): XbStatement? {
        val indexToken = advance()
        if (!match(TokenType.ON)) {
            recordError("Expected ON after INDEX at ${peek().startOffset}")
            return parseExpressionStatement()
        }
        val keyExpression = parseExpression(0) ?: run {
            recordError("Expected expression after INDEX ON at ${peek().startOffset}")
            fallbackExpression(indexToken)
        }
        if (!match(TokenType.TO)) {
            recordError("Expected TO after INDEX ON expression at ${peek().startOffset}")
            return XbExpressionStatement(keyExpression, rangeFrom(indexToken, previousOr(indexToken)))
        }
        val targetExpression = if (match(TokenType.IDENTIFIER)) {
            XbIdentifierExpression(previous().lexeme, rangeFrom(previous(), previous()))
        } else {
            recordError("Expected identifier after TO at ${peek().startOffset}")
            fallbackExpression(indexToken)
        }
        match(TokenType.SEMICOLON)
        val commandExpression = XbBinaryExpression(
            operator = "to",
            left = keyExpression,
            right = targetExpression,
            range = rangeFromOffsets(keyExpression.range.startOffset, targetExpression.range.endOffset),
        )
        return XbExpressionStatement(commandExpression, rangeFrom(indexToken, previousOr(indexToken)))
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
        val compoundOperator = matchCompoundAssignmentOperator()
        if (compoundOperator == null && !match(TokenType.ASSIGN)) {
            recordError("Expected assignment operator after assignment target at ${peek().startOffset}")
        }
        val rightHandSide = parseExpression(0) ?: run {
            recordError("Expected expression after assignment operator at ${peek().startOffset}")
            fallbackExpression(targetToken)
        }
        val value = if (compoundOperator != null) {
            XbBinaryExpression(
                operator = compoundOperator,
                left = target,
                right = rightHandSide,
                range = rangeFromOffsets(target.range.startOffset, rightHandSide.range.endOffset),
            )
        } else {
            rightHandSide
        }
        match(TokenType.SEMICOLON)
        val range = rangeFromOffsets(target.range.startOffset, value.range.endOffset)
        return XbAssignmentStatement(target, value, range)
    }

    private fun matchCompoundAssignmentOperator(): String? {
        if (check(TokenType.PLUS) && peekNext().type == TokenType.EQ) {
            advance()
            advance()
            return "+"
        }
        if (check(TokenType.MINUS) && peekNext().type == TokenType.EQ) {
            advance()
            advance()
            return "-"
        }
        if (check(TokenType.STAR) && peekNext().type == TokenType.EQ) {
            advance()
            advance()
            return "*"
        }
        if (check(TokenType.SLASH) && peekNext().type == TokenType.EQ) {
            advance()
            advance()
            return "/"
        }
        if (check(TokenType.PERCENT) && peekNext().type == TokenType.EQ) {
            advance()
            advance()
            return "%"
        }
        return null
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
        val expression = if (isAtEnd() || isTerminator(peek().type) || !canStartExpression(peek().type)) {
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

    private fun parseWaitStatement(): XbStatement {
        val waitToken = advance()
        val expression = if (isAtEnd() || isTerminator(peek().type) || !canStartExpression(peek().type)) {
            null
        } else {
            parseExpression(0).also { expr ->
                if (expr == null) {
                    recordError("Expected expression after WAIT at ${peek().startOffset}")
                    synchronize()
                }
            }
        }
        match(TokenType.SEMICOLON)
        val endToken = previousOr(waitToken)
        val range = rangeFrom(waitToken, endToken)
        return XbWaitStatement(expression, range)
    }

    private fun parseExitStatement(): XbStatement {
        val exitToken = advance()
        match(TokenType.SEMICOLON)
        val endToken = previousOr(exitToken)
        return XbExitStatement(rangeFrom(exitToken, endToken))
    }

    private fun parseBreakStatement(): XbStatement {
        val breakToken = advance()
        val expression = if (isAtEnd() || isTerminator(peek().type) || !canStartExpression(peek().type)) {
            null
        } else {
            parseExpression(0).also { expr ->
                if (expr == null) {
                    recordError("Expected expression after BREAK at ${peek().startOffset}")
                    synchronize()
                }
            }
        }
        match(TokenType.SEMICOLON)
        val endToken = previousOr(breakToken)
        return XbBreakStatement(expression, rangeFrom(breakToken, endToken))
    }

    private fun parseSequenceStatement(): XbStatement {
        val beginToken = advance()
        advance() // consume SEQUENCE
        val body = parseBlock(setOf(TokenType.RECOVER, TokenType.END))
        val recoverBlock: XbBlock?
        val recoverVariable: String?
        if (match(TokenType.RECOVER)) {
            recoverVariable = if (match(TokenType.USING) && match(TokenType.IDENTIFIER)) {
                previous().lexeme
            } else {
                null
            }
            recoverBlock = parseBlock(setOf(TokenType.END))
        } else {
            recoverVariable = null
            recoverBlock = null
        }
        if (!match(TokenType.END)) {
            recordError("Expected END to close SEQUENCE at ${peek().startOffset}")
        } else if (!match(TokenType.SEQUENCE)) {
            recordError("Expected SEQUENCE after END at ${peek().startOffset}")
        }
        val endToken = previousOr(beginToken)
        return XbSequenceStatement(body, recoverVariable, recoverBlock, rangeFrom(beginToken, endToken))
    }

    private fun parseAtSayGetStatement(): XbStatement {
        val atToken = advance()
        val rowExpr = parseExpression(0) ?: run {
            recordError("Expected row expression after '@' at ${peek().startOffset}")
            fallbackExpression(atToken)
        }
        val columnExpr = if (match(TokenType.COMMA)) {
            parseExpression(0) ?: run {
                recordError("Expected column expression after ',' at ${peek().startOffset}")
                fallbackExpression(atToken)
            }
        } else {
            null
        }
        val sayExpr = if (match(TokenType.SAY)) {
            parseExpression(0) ?: run {
                recordError("Expected expression after SAY at ${peek().startOffset}")
                fallbackExpression(atToken)
            }
        } else {
            null
        }
        val getExpr = if (match(TokenType.GET)) {
            parseExpression(0) ?: run {
                recordError("Expected expression after GET at ${peek().startOffset}")
                fallbackExpression(atToken)
            }
        } else {
            null
        }
        val validExpr = if (match(TokenType.VALID)) {
            parseExpression(0) ?: run {
                recordError("Expected expression after VALID at ${peek().startOffset}")
                fallbackExpression(atToken)
            }
        } else {
            null
        }
        val endToken = previousOr(atToken)
        return XbAtSayGetStatement(rowExpr, columnExpr, sayExpr, getExpr, validExpr, rangeFrom(atToken, endToken))
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
        val thenBlock = parseBlock(setOf(TokenType.ELSEIF, TokenType.ELSE, TokenType.ENDIF))

        val elseIfBranches = mutableListOf<Triple<Token, XbExpression, XbBlock>>()
        while (match(TokenType.ELSEIF)) {
            val elseIfToken = previous()
            val elseIfCondition = parseExpression(0) ?: run {
                recordError("Expected condition after ELSEIF at ${peek().startOffset}")
                synchronizeTo(setOf(TokenType.THEN, TokenType.ELSEIF, TokenType.ELSE, TokenType.ENDIF))
                fallbackExpression(elseIfToken)
            }
            match(TokenType.THEN)
            val elseIfThenBlock = parseBlock(setOf(TokenType.ELSEIF, TokenType.ELSE, TokenType.ENDIF))
            elseIfBranches += Triple(elseIfToken, elseIfCondition, elseIfThenBlock)
        }

        val explicitElse = if (match(TokenType.ELSE)) {
            parseBlock(setOf(TokenType.ENDIF))
        } else {
            null
        }
        var elseBlock = explicitElse
        for ((elseIfToken, elseIfCondition, elseIfThenBlock) in elseIfBranches.asReversed()) {
            val nestedElseIf = XbIfStatement(
                condition = elseIfCondition,
                thenBlock = elseIfThenBlock,
                elseBlock = elseBlock,
                range = rangeFromOffsets(
                    elseIfToken.startOffset,
                    elseBlock?.range?.endOffset ?: elseIfThenBlock.range.endOffset,
                ),
            )
            elseBlock = XbBlock(
                statements = listOf(nestedElseIf),
                range = nestedElseIf.range,
            )
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

    private fun parseDoWhileStatement(): XbStatement {
        val doToken = advance()
        advance() // consume WHILE
        val condition = parseExpression(0) ?: run {
            recordError("Expected condition after DO WHILE at ${peek().startOffset}")
            synchronizeTo(setOf(TokenType.ENDDO))
            fallbackExpression(doToken)
        }
        val body = parseBlock(setOf(TokenType.ENDDO))
        if (!match(TokenType.ENDDO)) {
            recordError("Expected ENDDO to close DO WHILE at ${peek().startOffset}")
        }
        val endToken = previousOr(doToken)
        return XbWhileStatement(condition, body, rangeFrom(doToken, endToken))
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
        val body = parseBlock(FUNCTION_BLOCK_TERMINATORS)
        if (!match(TokenType.ENDFUNCTION) && !canImplicitlyTerminateDeclarationBody()) {
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
        val body = parseBlock(PROCEDURE_BLOCK_TERMINATORS)
        if (!match(TokenType.ENDPROC) && !canImplicitlyTerminateDeclarationBody()) {
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



    private fun canImplicitlyTerminateDeclarationBody(): Boolean {
        return isAtEnd() || peek().type in DECLARATION_START_TOKENS
    }

    private fun parseExpression(minPrecedence: Int): XbExpression? {
        var left = parsePrefix() ?: return null
        left = parsePostfix(left)
        while (true) {
            val precedence = infixPrecedence(peek().type) ?: break
            if (precedence < minPrecedence) break
            val operatorToken = advance()
            if (!canStartExpression(peek().type) && isExpressionBoundary(peek().type)) {
                return left
            }
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
            TokenType.TRUE -> XbLiteralExpression(XbLiteralKind.BOOLEAN, "true", rangeFrom(token, token))
            TokenType.FALSE -> XbLiteralExpression(XbLiteralKind.BOOLEAN, "false", rangeFrom(token, token))
            TokenType.IDENTIFIER -> XbIdentifierExpression(token.lexeme, rangeFrom(token, token))
            TokenType.MINUS, TokenType.PLUS, TokenType.NOT, TokenType.AMP, TokenType.AT -> {
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
                if (match(TokenType.PIPE)) {
                    val parameters = mutableListOf<String>()
                    if (!match(TokenType.PIPE)) {
                        do {
                            if (match(TokenType.IDENTIFIER)) {
                                parameters += previous().lexeme
                            } else {
                                recordError("Expected parameter name in block literal at ${peek().startOffset}")
                                break
                            }
                        } while (match(TokenType.COMMA))
                        if (!match(TokenType.PIPE)) {
                            recordError("Expected '|' after block parameters at ${peek().startOffset}")
                        }
                    }
                    val body = parseExpression(0) ?: run {
                        recordError("Expected expression in block literal at ${peek().startOffset}")
                        fallbackExpression(token)
                    }
                    if (!match(TokenType.RBRACE)) {
                        recordError("Expected '}' after block literal at ${peek().startOffset}")
                    }
                    val endToken = previousOr(token)
                    XbBlockLiteralExpression(parameters, body, rangeFrom(token, endToken))
                } else if (match(TokenType.ARROW)) {
                    if (!match(TokenType.RBRACE)) {
                        recordError("Expected '}' after hash literal at ${peek().startOffset}")
                    }
                    val endToken = previousOr(token)
                    XbHashLiteralExpression(emptyList(), rangeFrom(token, endToken))
                } else {
                    val firstExpr = if (!check(TokenType.RBRACE)) {
                        parseExpression(0) ?: run {
                            recordError("Expected expression in array literal at ${peek().startOffset}")
                            fallbackExpression(token)
                        }
                    } else {
                        null
                    }
                    if (firstExpr != null && match(TokenType.ARROW)) {
                        val entries = mutableListOf<Pair<XbExpression, XbExpression>>()
                        val valueExpr = parseExpression(0) ?: run {
                            recordError("Expected value expression in hash literal at ${peek().startOffset}")
                            fallbackExpression(token)
                        }
                        entries += firstExpr to valueExpr
                        while (match(TokenType.COMMA)) {
                            val keyExpr = parseExpression(0) ?: run {
                                recordError("Expected key expression in hash literal at ${peek().startOffset}")
                                fallbackExpression(token)
                            }
                            if (!match(TokenType.ARROW)) {
                                recordError("Expected '=>' after hash key at ${peek().startOffset}")
                            }
                            val nextValueExpr = parseExpression(0) ?: run {
                                recordError("Expected value expression in hash literal at ${peek().startOffset}")
                                fallbackExpression(token)
                            }
                            entries += keyExpr to nextValueExpr
                        }
                        if (!match(TokenType.RBRACE)) {
                            recordError("Expected '}' after hash literal at ${peek().startOffset}")
                        }
                        val endToken = previousOr(token)
                        XbHashLiteralExpression(entries, rangeFrom(token, endToken))
                    } else {
                        val elements = mutableListOf<XbExpression>()
                        firstExpr?.let { elements += it }
                        if (firstExpr != null) {
                            while (match(TokenType.COMMA)) {
                                parseExpression(0)?.let { elements += it } ?: run {
                                    recordError("Expected expression in array literal at ${peek().startOffset}")
                                    elements += fallbackExpression(token)
                                }
                            }
                        }
                        if (!match(TokenType.RBRACE)) {
                            recordError("Expected '}' after array literal at ${peek().startOffset}")
                        }
                        val endToken = previousOr(token)
                        XbArrayLiteralExpression(elements, rangeFrom(token, endToken))
                    }
                }
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
                    val indexExpressions = mutableListOf<XbExpression>()
                    parseExpression(0)?.let { indexExpressions += it } ?: run {
                        recordError("Expected expression inside indexer at ${peek().startOffset}")
                        indexExpressions += fallbackExpression(previous())
                    }
                    while (match(TokenType.COMMA)) {
                        parseExpression(0)?.let { indexExpressions += it } ?: run {
                            recordError("Expected expression after ',' inside indexer at ${peek().startOffset}")
                            indexExpressions += fallbackExpression(previous())
                        }
                    }
                    if (!match(TokenType.RBRACKET)) {
                        recordError("Expected ']' after index expression at ${peek().startOffset}")
                    }
                    var indexedExpression = currentExpression
                    for (indexExpr in indexExpressions) {
                        indexedExpression = XbIndexExpression(
                            target = indexedExpression,
                            index = indexExpr,
                            range = rangeFromOffsets(indexedExpression.range.startOffset, indexExpr.range.endOffset),
                        )
                    }
                    indexedExpression
                }
                TokenType.COLON -> {
                    val colonToken = advance()
                    val memberToken = if (match(TokenType.IDENTIFIER)) previous() else null
                    if (memberToken == null) {
                        recordError("Expected member name after ':' at ${peek().startOffset}")
                    }
                    val arguments = mutableListOf<XbExpression>()
                    arguments += currentExpression
                    if (match(TokenType.LPAREN)) {
                        if (!check(TokenType.RPAREN)) {
                            do {
                                parseExpression(0)?.let { arguments += it } ?: run {
                                    recordError("Expected expression in argument list at ${peek().startOffset}")
                                    arguments += fallbackExpression(previousOr(colonToken))
                                }
                            } while (match(TokenType.COMMA))
                        }
                        if (!match(TokenType.RPAREN)) {
                            recordError("Expected ')' after arguments at ${peek().startOffset}")
                        }
                    }
                    val calleeToken = memberToken ?: colonToken
                    val callee = XbIdentifierExpression(
                        calleeToken.lexeme.ifEmpty { "<error>" },
                        rangeFrom(calleeToken, calleeToken),
                    )
                    val endToken = previousOr(calleeToken)
                    XbCallExpression(
                        callee = callee,
                        arguments = arguments,
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
            val indexExpressions = mutableListOf<XbExpression>()
            parseExpression(0)?.let { indexExpressions += it } ?: run {
                recordError("Expected expression inside indexer at ${peek().startOffset}")
                indexExpressions += fallbackExpression(previous())
            }
            while (match(TokenType.COMMA)) {
                parseExpression(0)?.let { indexExpressions += it } ?: run {
                    recordError("Expected expression after ',' inside indexer at ${peek().startOffset}")
                    indexExpressions += fallbackExpression(previous())
                }
            }
            if (!match(TokenType.RBRACKET)) {
                recordError("Expected ']' after index expression at ${peek().startOffset}")
            }
            for (indexExpr in indexExpressions) {
                target = XbIndexExpression(
                    target = target,
                    index = indexExpr,
                    range = rangeFromOffsets(target.range.startOffset, indexExpr.range.endOffset),
                )
            }
        }
        return target
    }

    private fun infixPrecedence(type: TokenType): Int? {
        return when (type) {
            TokenType.OR -> 1
            TokenType.AND -> 2
            TokenType.EQ, TokenType.NEQ, TokenType.CONTAINS -> 3
            TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE -> 4
            TokenType.PLUS, TokenType.MINUS -> 5
            TokenType.STAR, TokenType.SLASH, TokenType.PERCENT -> 6
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

    private fun isAssignmentStatementStart(): Boolean {
        if (!check(TokenType.IDENTIFIER)) return false
        var index = current + 1
        if (index >= tokens.size) return false
        if (isAssignmentOperatorAt(index)) {
            return true
        }
        while (index < tokens.size && tokens[index].type == TokenType.LBRACKET) {
            var depth = 0
            while (index < tokens.size) {
                val type = tokens[index].type
                if (type == TokenType.LBRACKET) {
                    depth++
                } else if (type == TokenType.RBRACKET) {
                    depth--
                    if (depth == 0) {
                        index++
                        break
                    }
                }
                index++
            }
            if (depth != 0 || index >= tokens.size) {
                return false
            }
            if (isAssignmentOperatorAt(index)) {
                return true
            }
        }
        return false
    }

    private fun isAssignmentOperatorAt(index: Int): Boolean {
        if (index >= tokens.size) return false
        if (tokens[index].type == TokenType.ASSIGN) {
            return true
        }
        if (index + 1 >= tokens.size) {
            return false
        }
        val operator = tokens[index].type
        return (operator == TokenType.PLUS ||
            operator == TokenType.MINUS ||
            operator == TokenType.STAR ||
            operator == TokenType.SLASH ||
            operator == TokenType.PERCENT) &&
            tokens[index + 1].type == TokenType.EQ
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
            return previous()
        }
        return peek()
    }

    private fun peek(): Token = tokens[current]

    private fun peekNext(): Token = if (current + 1 < tokens.size) tokens[current + 1] else tokens.last()

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

    private fun canStartAtSayGetExpression(type: TokenType): Boolean {
        return canStartExpression(type) && type != TokenType.AT
    }

    private fun canStartExpression(type: TokenType): Boolean {
        return when (type) {
            TokenType.NUMBER,
            TokenType.STRING,
            TokenType.NIL,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.IDENTIFIER,
            TokenType.MINUS,
            TokenType.PLUS,
            TokenType.NOT,
            TokenType.AMP,
            TokenType.AT,
            TokenType.LPAREN,
            TokenType.LBRACE,
            -> true
            else -> false
        }
    }

    private fun isExpressionBoundary(type: TokenType): Boolean {
        return type == TokenType.SEMICOLON ||
            type == TokenType.COMMA ||
            type == TokenType.RPAREN ||
            type == TokenType.RBRACKET ||
            type == TokenType.RBRACE ||
            isTerminator(type)
    }

    private fun isTerminator(type: TokenType): Boolean {
        return type == TokenType.SEMICOLON ||
            type == TokenType.ENDIF ||
            type == TokenType.ELSE ||
            type == TokenType.ELSEIF ||
            type == TokenType.ENDDO ||
            type == TokenType.ENDFUNCTION ||
            type == TokenType.ENDPROC ||
            type == TokenType.NEXT ||
            type == TokenType.RECOVER ||
            type == TokenType.END ||
            type == TokenType.EOF
    }

    companion object {
        private const val PREFIX_PRECEDENCE = 7
        private val SYNC_TOKENS = setOf(
            TokenType.SEMICOLON,
            TokenType.ENDIF,
            TokenType.ELSE,
            TokenType.ELSEIF,
            TokenType.ENDDO,
            TokenType.ENDFUNCTION,
            TokenType.ENDPROC,
            TokenType.NEXT,
            TokenType.RECOVER,
            TokenType.END,
            TokenType.EOF,
        )

        private val DECLARATION_START_TOKENS = setOf(
            TokenType.FUNCTION,
            TokenType.PROCEDURE,
        )

        private val FUNCTION_BLOCK_TERMINATORS = setOf(TokenType.ENDFUNCTION) + DECLARATION_START_TOKENS

        private val PROCEDURE_BLOCK_TERMINATORS = setOf(TokenType.ENDPROC) + DECLARATION_START_TOKENS

        fun parse(source: String): XbParseResult {
            val preprocess = com.prestoxbasopp.core.lexer.XbPreprocessor.preprocess(source)
            val lexer = XbLexer(preprocess.filteredSource, preprocess.sourceMap)
            val tokens = lexer.lex()
            val parser = XbParser(tokens)
            return parser.parseProgram()
        }
    }
}
