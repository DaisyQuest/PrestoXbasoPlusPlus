package com.prestoxbasopp.ide.inspections

import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbIndexExpression
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbLiteralKind
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.core.lexer.XbKeywords
import com.prestoxbasopp.core.lexer.XbTokenType
import kotlin.math.abs

object XbStandardInspections {
    val all: List<XbInspectionRule> = listOf(
        xbInspection(
            id = "XB100",
            title = "Lexer syntax error",
        ) {
            description = "Reports lexer errors such as unexpected characters and malformed literals."
            severity = XbInspectionSeverity.ERROR
            onLexerErrors { error, emitter, context ->
                emitter.report(error.range, context.formatMessage(error.message))
            }
        },
        xbInspection(
            id = "XB101",
            title = "Parser syntax error",
        ) {
            description = "Reports parser recovery errors such as missing keywords or malformed statements."
            severity = XbInspectionSeverity.ERROR
            onParserIssues { issue, emitter, _ ->
                emitter.report(issue.range, issue.message)
            }
        },
        xbInspection(
            id = "XB200",
            title = "Empty statement",
        ) {
            description = "Flags redundant semicolons that introduce empty statements."
            severity = XbInspectionSeverity.WARNING
            onTokens { tokens, emitter, _ ->
                val significant = tokens.filterNot { it.type == XbTokenType.COMMENT }
                significant.forEachIndexed { index, token ->
                    if (token.type == XbTokenType.PUNCTUATION && token.text == ";") {
                        if (isEmptyStatement(significant, index)) {
                            emitter.report(token.range, "Empty statement is unnecessary.")
                        }
                    }
                }
            }
        },
        xbInspection(
            id = "XB205",
            title = "Line continuation",
        ) {
            description = "Highlights semicolons that explicitly continue a statement onto the next line."
            severity = XbInspectionSeverity.INFO
            onTokens { tokens, emitter, context ->
                val significant = tokens.filterNot { it.type == XbTokenType.COMMENT }
                val lineIndex = XbLineIndex(context.source)
                significant.forEachIndexed { index, token ->
                    if (token.type == XbTokenType.PUNCTUATION && token.text == ";") {
                        if (isEmptyStatement(significant, index)) {
                            return@forEachIndexed
                        }
                        val next = significant.getOrNull(index + 1) ?: return@forEachIndexed
                        val tokenLine = lineIndex.lineFor(token.range.startOffset)
                        val nextLine = lineIndex.lineFor(next.range.startOffset)
                        if (nextLine > tokenLine) {
                            emitter.report(token.range, "Statement continues on the next line.")
                        }
                    }
                }
            }
        },
        xbInspection(
            id = "XB210",
            title = "Redundant parentheses",
        ) {
            description = "Highlights parentheses that wrap a single literal or identifier."
            severity = XbInspectionSeverity.INFO
            onTokens { tokens, emitter, _ ->
                val significant = tokens.filterNot { it.type == XbTokenType.COMMENT }
                var index = 0
                while (index < significant.size - 2) {
                    val open = significant[index]
                    val middle = significant[index + 1]
                    val close = significant[index + 2]
                    if (open.type == XbTokenType.PUNCTUATION && open.text == "(" &&
                        close.type == XbTokenType.PUNCTUATION && close.text == ")" &&
                        middle.type in setOf(XbTokenType.IDENTIFIER, XbTokenType.NUMBER, XbTokenType.STRING)
                    ) {
                        val previous = significant.getOrNull(index - 1)
                        val isCallLike = previous?.type == XbTokenType.IDENTIFIER
                        if (!isCallLike) {
                            emitter.report(
                                com.prestoxbasopp.core.api.XbTextRange(open.range.startOffset, close.range.endOffset),
                                "Parentheses around a single value are redundant.",
                            )
                        }
                        index += 3
                        continue
                    }
                    index++
                }
            }
        },
        xbInspection(
            id = "XB220",
            title = "Constant condition",
        ) {
            description = "Warns when IF/WHILE conditions are constant expressions."
            severity = XbInspectionSeverity.WARNING
            onStatements { statement, emitter, _ ->
                val condition = when (statement) {
                    is XbIfStatement -> statement.condition
                    is XbWhileStatement -> statement.condition
                    else -> null
                }
                if (condition != null && condition.isConstantExpression()) {
                    val keyword = if (statement is XbIfStatement) "IF" else "WHILE"
                    emitter.report(statement.range, "$keyword condition is always constant.")
                }
            }
        },
        xbInspection(
            id = "XB230",
            title = "Self comparison",
        ) {
            description = "Detects comparisons where both operands are the same value."
            severity = XbInspectionSeverity.WARNING
            onExpressions { expression, emitter, _ ->
                if (expression is XbBinaryExpression && expression.operator in setOf("==", "!=")) {
                    val left = expression.left
                    val right = expression.right
                    val isSame = when {
                        left is XbIdentifierExpression && right is XbIdentifierExpression -> left.name == right.name
                        left is XbLiteralExpression && right is XbLiteralExpression ->
                            left.kind == right.kind && left.value == right.value
                        else -> false
                    }
                    if (isSame) {
                        emitter.report(expression.range, "Both sides of the comparison are identical.")
                    }
                }
            }
        },
        xbInspection(
            id = "XB240",
            title = "Unreachable statement",
        ) {
            description = "Flags statements that appear after a RETURN in the same block."
            severity = XbInspectionSeverity.WARNING
            onAst { program, emitter, _ ->
                fun startsNewSection(statement: com.prestoxbasopp.core.ast.XbStatement): Boolean {
                    return when (statement) {
                        is com.prestoxbasopp.core.ast.XbFunctionDeclaration,
                        is com.prestoxbasopp.core.ast.XbProcedureDeclaration,
                        -> true
                        is com.prestoxbasopp.core.ast.XbExpressionStatement -> {
                            val expression = statement.expression
                            expression is com.prestoxbasopp.core.ast.XbIdentifierExpression &&
                                expression.name.lowercase() in FUNCTION_BOUNDARY_KEYWORDS
                        }
                        else -> false
                    }
                }

                fun checkSequence(statements: List<com.prestoxbasopp.core.ast.XbStatement>) {
                    var seenReturn = false
                    statements.forEach { child ->
                        if (startsNewSection(child)) {
                            seenReturn = false
                        }
                        if (seenReturn) {
                            emitter.report(child.range, "Statement is unreachable after RETURN.")
                        }
                        if (child is XbReturnStatement) {
                            seenReturn = true
                        }
                        when (child) {
                            is com.prestoxbasopp.core.ast.XbIfStatement -> {
                                checkSequence(child.thenBlock.statements)
                                child.elseBlock?.let { checkSequence(it.statements) }
                            }
                            is com.prestoxbasopp.core.ast.XbWhileStatement -> checkSequence(child.body.statements)
                            is com.prestoxbasopp.core.ast.XbForStatement -> checkSequence(child.body.statements)
                            is com.prestoxbasopp.core.ast.XbFunctionDeclaration -> checkSequence(child.body.statements)
                            is com.prestoxbasopp.core.ast.XbProcedureDeclaration -> checkSequence(child.body.statements)
                            is com.prestoxbasopp.core.ast.XbBlock -> checkSequence(child.statements)
                            else -> Unit
                        }
                    }
                }

                checkSequence(program.statements)
            }
        },
        xbInspection(
            id = "XB250",
            title = "Possible keyword misspelling",
        ) {
            description = "Suggests the closest keyword when an identifier appears to be misspelled."
            severity = XbInspectionSeverity.WARNING
            onTokens { tokens, emitter, _ ->
                tokens
                    .filter { it.type == XbTokenType.IDENTIFIER }
                    .forEach { token ->
                        val suggestion = suggestKeyword(token.text)
                        if (suggestion != null) {
                            emitter.report(token.range, "Did you mean \"$suggestion\"?")
                        }
                }
            }
        },
        xbInspection(
            id = "XB260",
            title = "GOD CLASS DETECTED - TIER 1",
        ) {
            description = "Warns when a source file exceeds 3000 lines."
            severity = XbInspectionSeverity.WARNING
            onTokens { _, emitter, context ->
                val lines = lineCount(context.source)
                if (lines > 3000 && lines <= 5000) {
                    emitter.report(fileRange(context.source), "GOD CLASS DETECTED - TIER 1: file has $lines lines (>3000).")
                }
            }
        },
        xbInspection(
            id = "XB261",
            title = "GOD CLASS DETECTED - TIER 2",
        ) {
            description = "Reports files exceeding 5000 lines as low-level errors."
            severity = XbInspectionSeverity.ERROR
            onTokens { _, emitter, context ->
                val lines = lineCount(context.source)
                if (lines > 5000 && lines <= 10000) {
                    emitter.report(fileRange(context.source), "GOD CLASS DETECTED - TIER 2: file has $lines lines (>5000).")
                }
            }
        },
        xbInspection(
            id = "XB262",
            title = "GOD CLASS DETECTED - TIER 3",
        ) {
            description = "Reports files exceeding 10000 lines as low-level errors."
            severity = XbInspectionSeverity.ERROR
            onTokens { _, emitter, context ->
                val lines = lineCount(context.source)
                if (lines > 10000) {
                    emitter.report(fileRange(context.source), "GOD CLASS DETECTED - TIER 3: file has $lines lines (>10000).")
                }
            }
        },
        xbInspection(
            id = "XB270",
            title = "1-based array access misuse",
        ) {
            description = "Flags index 0 usage and FOR loops that start at 0 and iterate to Len(array)."
            severity = XbInspectionSeverity.WARNING
            onExpressions { expression, emitter, _ ->
                if (expression is XbIndexExpression && expression.index.isNumericLiteral("0")) {
                    emitter.report(expression.index.range, "Arrays in Xbase++ are 1-based. Index 0 is invalid.")
                }
            }
            onStatements { statement, emitter, _ ->
                if (statement is XbForStatement && statement.start.isNumericLiteral("0") && statement.end.isLenCall()) {
                    emitter.report(statement.range, "Arrays in Xbase++ are 1-based. FOR loops over arrays should start at 1.")
                }
            }
        },
        xbInspection(
            id = "XB271",
            title = "Function without explicit RETURN",
        ) {
            description = "Warns when FUNCTION declarations have no explicit RETURN statement."
            severity = XbInspectionSeverity.WARNING
            onStatements { statement, emitter, _ ->
                if (statement is XbFunctionDeclaration && !containsReturn(statement.body.statements)) {
                    emitter.report(statement.range, "FUNCTION '${statement.name}' has no explicit RETURN statement.")
                }
            }
        },
        xbInspection(
            id = "XB272",
            title = "Procedure returning value",
        ) {
            description = "Warns when PROCEDURE returns a value expression."
            severity = XbInspectionSeverity.WARNING
            onStatements { statement, emitter, _ ->
                if (statement is XbProcedureDeclaration) {
                    findProcedureValueReturns(statement.body.statements).forEach { valueReturn ->
                        emitter.report(valueReturn.range, "Procedures should not return values.")
                    }
                }
            }
        },
        xbInspection(
            id = "XB273",
            title = "DO WHILE infinite loop risk",
        ) {
            description = "Flags WHILE .T. loops that do not contain EXIT."
            severity = XbInspectionSeverity.WARNING
            onStatements { statement, emitter, _ ->
                if (statement is XbWhileStatement && statement.condition.isBooleanTrueLiteral() && !containsExit(statement.body.statements)) {
                    emitter.report(statement.range, "DO WHILE .T. loop without EXIT may be infinite.")
                }
            }
        },
        xbInspection(
            id = "XB274",
            title = "PUBLIC or PRIVATE overuse",
        ) {
            description = "Flags PUBLIC/PRIVATE declarations and recommends LOCAL/STATIC."
            severity = XbInspectionSeverity.WARNING
            onTokens { tokens, emitter, _ ->
                tokens.forEach { token ->
                    val keyword = token.text.lowercase()
                    if (token.type == XbTokenType.KEYWORD && keyword in setOf("public", "private")) {
                        emitter.report(token.range, "Avoid $keyword variables; prefer LOCAL or STATIC for safer scope.")
                    }
                }
            }
        },
    )
}


private val FUNCTION_BOUNDARY_KEYWORDS = setOf(
    "function",
    "procedure",
    "method",
    "class",
    "inline",
    "endclass",
)


private fun isEmptyStatement(tokens: List<com.prestoxbasopp.core.lexer.XbToken>, index: Int): Boolean {
    val previous = tokens.getOrNull(index - 1)
    val next = tokens.getOrNull(index + 1)
    val previousKeyword = previous?.text?.lowercase()
    val nextKeyword = next?.text?.lowercase()
    return previous == null ||
        (previous.type == XbTokenType.PUNCTUATION && previous.text == ";") ||
        previousKeyword in setOf("then", "do", "else") ||
        next == null ||
        (next.type == XbTokenType.PUNCTUATION && next.text == ";") ||
        nextKeyword in setOf("endif", "enddo", "else")
}

private fun fileRange(source: String): com.prestoxbasopp.core.api.XbTextRange {
    return com.prestoxbasopp.core.api.XbTextRange(0, source.length)
}

private fun lineCount(source: String): Int {
    if (source.isEmpty()) return 0
    var lines = 1
    source.forEach { char ->
        if (char == '\n') {
            lines++
        }
    }
    return lines
}

private fun XbExpression.isNumericLiteral(value: String): Boolean {
    return this is XbLiteralExpression && kind == XbLiteralKind.NUMBER && this.value == value
}

private fun XbExpression.isLenCall(): Boolean {
    if (this !is XbCallExpression) return false
    val calleeName = (callee as? XbIdentifierExpression)?.name?.lowercase() ?: return false
    return calleeName == "len" && arguments.size == 1
}

private fun XbExpression.isBooleanTrueLiteral(): Boolean {
    return this is XbLiteralExpression && kind == XbLiteralKind.BOOLEAN &&
        (value.equals(".T.", ignoreCase = true) || value.equals("true", ignoreCase = true))
}

private fun containsReturn(statements: List<com.prestoxbasopp.core.ast.XbStatement>): Boolean {
    return statements.any { statement ->
        when (statement) {
            is XbReturnStatement -> true
            is com.prestoxbasopp.core.ast.XbIfStatement ->
                containsReturn(statement.thenBlock.statements) || containsReturn(statement.elseBlock?.statements.orEmpty())
            is com.prestoxbasopp.core.ast.XbWhileStatement -> containsReturn(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbForStatement -> containsReturn(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbSequenceStatement ->
                containsReturn(statement.body.statements) || containsReturn(statement.recoverBlock?.statements.orEmpty())
            is com.prestoxbasopp.core.ast.XbBlock -> containsReturn(statement.statements)
            else -> false
        }
    }
}

private fun findProcedureValueReturns(statements: List<com.prestoxbasopp.core.ast.XbStatement>): List<XbReturnStatement> {
    val findings = mutableListOf<XbReturnStatement>()
    statements.forEach { statement ->
        when (statement) {
            is XbReturnStatement -> if (statement.expression != null) findings += statement
            is com.prestoxbasopp.core.ast.XbIfStatement -> {
                findings += findProcedureValueReturns(statement.thenBlock.statements)
                findings += findProcedureValueReturns(statement.elseBlock?.statements.orEmpty())
            }
            is com.prestoxbasopp.core.ast.XbWhileStatement -> findings += findProcedureValueReturns(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbForStatement -> findings += findProcedureValueReturns(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbSequenceStatement -> {
                findings += findProcedureValueReturns(statement.body.statements)
                findings += findProcedureValueReturns(statement.recoverBlock?.statements.orEmpty())
            }
            is com.prestoxbasopp.core.ast.XbBlock -> findings += findProcedureValueReturns(statement.statements)
            else -> Unit
        }
    }
    return findings
}

private fun containsExit(statements: List<com.prestoxbasopp.core.ast.XbStatement>): Boolean {
    return statements.any { statement ->
        when (statement) {
            is com.prestoxbasopp.core.ast.XbExitStatement -> true
            is com.prestoxbasopp.core.ast.XbIfStatement ->
                containsExit(statement.thenBlock.statements) || containsExit(statement.elseBlock?.statements.orEmpty())
            is com.prestoxbasopp.core.ast.XbWhileStatement -> containsExit(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbForStatement -> containsExit(statement.body.statements)
            is com.prestoxbasopp.core.ast.XbSequenceStatement ->
                containsExit(statement.body.statements) || containsExit(statement.recoverBlock?.statements.orEmpty())
            is com.prestoxbasopp.core.ast.XbBlock -> containsExit(statement.statements)
            else -> false
        }
    }
}

private class XbLineIndex(source: String) {
    private val lineStarts: IntArray = buildLineStarts(source)

    fun lineFor(offset: Int): Int {
        val safeOffset = offset.coerceAtLeast(0)
        var low = 0
        var high = lineStarts.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val start = lineStarts[mid]
            val nextStart = if (mid == lineStarts.lastIndex) Int.MAX_VALUE else lineStarts[mid + 1]
            if (safeOffset < start) {
                high = mid - 1
            } else if (safeOffset >= nextStart) {
                low = mid + 1
            } else {
                return mid
            }
        }
        return lineStarts.lastIndex
    }

    private fun buildLineStarts(source: String): IntArray {
        if (source.isEmpty()) return intArrayOf(0)
        val starts = mutableListOf(0)
        source.forEachIndexed { index, char ->
            if (char == '\n') {
                val next = index + 1
                if (next <= source.length) {
                    starts += next
                }
            }
        }
        return starts.toIntArray()
    }
}

private fun XbExpression.isConstantExpression(): Boolean {
    return when (this) {
        is XbLiteralExpression -> true
        is XbUnaryExpression -> expression.isConstantExpression()
        is XbBinaryExpression -> left.isConstantExpression() && right.isConstantExpression()
        is XbIdentifierExpression -> false
        is XbCallExpression -> false
        is XbIndexExpression -> false
        is com.prestoxbasopp.core.ast.XbArrayLiteralExpression ->
            elements.all { it.isConstantExpression() }
        is com.prestoxbasopp.core.ast.XbHashLiteralExpression ->
            entries.all { (key, value) ->
                key.isConstantExpression() && value.isConstantExpression()
            }
        is com.prestoxbasopp.core.ast.XbBlockLiteralExpression -> false
    }
}

private val KEYWORD_SUGGESTIONS: Set<String> = XbKeywords.all
private const val KEYWORD_SUGGESTION_MAX_DISTANCE = 1
private const val KEYWORD_SUGGESTION_MIN_LENGTH = 4

private fun suggestKeyword(text: String): String? {
    val normalized = text.lowercase()
    if (normalized.length < KEYWORD_SUGGESTION_MIN_LENGTH) {
        return null
    }
    var bestMatch: String? = null
    var bestDistance = KEYWORD_SUGGESTION_MAX_DISTANCE + 1
    KEYWORD_SUGGESTIONS.forEach { keyword ->
        val distance = levenshteinDistanceWithin(normalized, keyword, KEYWORD_SUGGESTION_MAX_DISTANCE)
        if (distance != null) {
            if (distance < bestDistance || (distance == bestDistance && keyword < (bestMatch ?: keyword))) {
                bestMatch = keyword
                bestDistance = distance
            }
        }
    }
    return bestMatch?.uppercase()
}

private fun levenshteinDistanceWithin(source: String, target: String, maxDistance: Int): Int? {
    if (source == target) {
        return 0
    }
    if (abs(source.length - target.length) > maxDistance) {
        return null
    }
    val previous = IntArray(target.length + 1) { it }
    val current = IntArray(target.length + 1)
    for (i in 1..source.length) {
        current[0] = i
        var minInRow = current[0]
        val sourceChar = source[i - 1]
        for (j in 1..target.length) {
            val cost = if (sourceChar == target[j - 1]) 0 else 1
            val deletion = previous[j] + 1
            val insertion = current[j - 1] + 1
            val substitution = previous[j - 1] + cost
            val value = minOf(deletion, insertion, substitution)
            current[j] = value
            if (value < minInRow) {
                minInRow = value
            }
        }
        if (minInRow > maxDistance) {
            return null
        }
        for (j in previous.indices) {
            previous[j] = current[j]
        }
    }
    return previous[target.length].takeIf { it <= maxDistance }
}
