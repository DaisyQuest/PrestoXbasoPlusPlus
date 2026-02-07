package com.prestoxbasopp.ide.inspections

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbAstNode
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbIndexExpression
import com.prestoxbasopp.core.ast.XbLocalDeclarationStatement
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
import com.prestoxbasopp.core.ast.XbPrintStatement
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.core.lexer.XbLexResult
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbLexerError
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.parser.XbParseResult
import com.prestoxbasopp.core.parser.XbParser
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.ide.XbPsiTextBuilder
import kotlin.math.min

enum class XbInspectionSeverity {
    ERROR,
    WARNING,
    INFO,
}

data class XbInspectionFinding(
    val id: String,
    val title: String,
    val message: String,
    val severity: XbInspectionSeverity,
    val range: XbTextRange,
)

data class XbParserIssue(
    val message: String,
    val range: XbTextRange,
)

data class XbInspectionProfile(
    val enabledInspections: Set<String> = emptySet(),
    val disabledInspections: Set<String> = emptySet(),
    val severityOverrides: Map<String, XbInspectionSeverity> = emptyMap(),
) {
    fun isEnabled(id: String): Boolean {
        return if (enabledInspections.isNotEmpty()) {
            id in enabledInspections
        } else {
            id !in disabledInspections
        }
    }

    fun severityFor(id: String, default: XbInspectionSeverity): XbInspectionSeverity {
        return severityOverrides[id] ?: default
    }
}

class XbInspectionContext private constructor(
    val source: String,
    val lexResult: XbLexResult,
    val parseResult: XbParseResult,
    val psiFile: XbPsiFile,
) {
    val tokens: List<XbToken> = lexResult.tokens
    val lexerErrors: List<XbLexerError> = lexResult.errors
    val parserIssues: List<XbParserIssue> = buildParserIssues(parseResult.errors, source.length)
    val program: XbProgram? = parseResult.program

    fun formatMessage(message: String): String {
        return when {
            message.startsWith("Unexpected character ") -> {
                val suffix = message.removePrefix("Unexpected character ").trim()
                "Unexpected character: $suffix."
            }
            message.endsWith(".") -> message
            else -> "$message."
        }
    }

    fun walkStatements(): Sequence<XbStatement> {
        val programRoot = program ?: return emptySequence()
        return sequence {
            yieldAll(walkStatements(programRoot))
        }
    }

    fun walkExpressions(): Sequence<XbExpression> {
        val programRoot = program ?: return emptySequence()
        return sequence {
            yieldAll(walkExpressions(programRoot))
        }
    }

    private fun walkStatements(node: XbAstNode): Sequence<XbStatement> = sequence {
        when (node) {
            is XbProgram -> node.statements.forEach { statement ->
                yield(statement)
                yieldAll(walkStatements(statement))
            }
            is XbBlock -> node.statements.forEach { statement ->
                yield(statement)
                yieldAll(walkStatements(statement))
            }
            is XbIfStatement -> {
                yieldAll(walkStatements(node.thenBlock))
                node.elseBlock?.let { yieldAll(walkStatements(it)) }
            }
            is XbWhileStatement -> yieldAll(walkStatements(node.body))
            is XbForStatement -> yieldAll(walkStatements(node.body))
            is XbFunctionDeclaration -> yieldAll(walkStatements(node.body))
            is XbProcedureDeclaration -> yieldAll(walkStatements(node.body))
            is XbExpressionStatement -> Unit
            is XbAssignmentStatement -> Unit
            is XbPrintStatement -> Unit
            is XbLocalDeclarationStatement -> Unit
            is XbReturnStatement -> Unit
            is XbExpression -> Unit
        }
    }

    private fun walkExpressions(node: XbAstNode): Sequence<XbExpression> = sequence {
        when (node) {
            is XbProgram -> node.statements.forEach { yieldAll(walkExpressions(it)) }
            is XbBlock -> node.statements.forEach { yieldAll(walkExpressions(it)) }
            is XbExpressionStatement -> {
                yield(node.expression)
                yieldAll(walkExpressions(node.expression))
            }
            is XbReturnStatement -> node.expression?.let { expression ->
                yield(expression)
                yieldAll(walkExpressions(expression))
            }
            is XbIfStatement -> {
                yield(node.condition)
                yieldAll(walkExpressions(node.condition))
                yieldAll(walkExpressions(node.thenBlock))
                node.elseBlock?.let { yieldAll(walkExpressions(it)) }
            }
            is XbWhileStatement -> {
                yield(node.condition)
                yieldAll(walkExpressions(node.condition))
                yieldAll(walkExpressions(node.body))
            }
            is XbForStatement -> {
                yield(node.iterator)
                yieldAll(walkExpressions(node.iterator))
                yield(node.start)
                yieldAll(walkExpressions(node.start))
                yield(node.end)
                yieldAll(walkExpressions(node.end))
                yield(node.step)
                yieldAll(walkExpressions(node.step))
                yieldAll(walkExpressions(node.body))
            }
            is XbFunctionDeclaration -> yieldAll(walkExpressions(node.body))
            is XbProcedureDeclaration -> yieldAll(walkExpressions(node.body))
            is com.prestoxbasopp.core.ast.XbUnaryExpression -> {
                yield(node.expression)
                yieldAll(walkExpressions(node.expression))
            }
            is com.prestoxbasopp.core.ast.XbBinaryExpression -> {
                yield(node.left)
                yieldAll(walkExpressions(node.left))
                yield(node.right)
                yieldAll(walkExpressions(node.right))
            }
            is XbCallExpression -> {
                yield(node.callee)
                yieldAll(walkExpressions(node.callee))
                node.arguments.forEach { argument ->
                    yield(argument)
                    yieldAll(walkExpressions(argument))
                }
            }
            is XbIndexExpression -> {
                yield(node.target)
                yieldAll(walkExpressions(node.target))
                yield(node.index)
                yieldAll(walkExpressions(node.index))
            }
            is com.prestoxbasopp.core.ast.XbArrayLiteralExpression -> node.elements.forEach { element ->
                yield(element)
                yieldAll(walkExpressions(element))
            }
            is XbAssignmentStatement -> {
                yield(node.target)
                yieldAll(walkExpressions(node.target))
                yield(node.value)
                yieldAll(walkExpressions(node.value))
            }
            is XbPrintStatement -> node.expressions.forEach { expression ->
                yield(expression)
                yieldAll(walkExpressions(expression))
            }
            is XbLocalDeclarationStatement -> node.bindings.forEach { binding ->
                binding.initializer?.let { initializer ->
                    yield(initializer)
                    yieldAll(walkExpressions(initializer))
                }
            }
            is com.prestoxbasopp.core.ast.XbIdentifierExpression -> Unit
            is com.prestoxbasopp.core.ast.XbLiteralExpression -> Unit
        }
    }

    private fun buildParserIssues(errors: List<String>, length: Int): List<XbParserIssue> {
        if (errors.isEmpty()) return emptyList()
        return errors.map { message ->
            val offset = parserOffsetRegex.findAll(message)
                .mapNotNull { it.groups[1]?.value?.toIntOrNull() }
                .lastOrNull()
            val safeOffset = offset?.coerceIn(0, length) ?: 0
            val endOffset = min(safeOffset + 1, length)
            XbParserIssue(message = formatMessage(message), range = XbTextRange(safeOffset, endOffset))
        }
    }

    companion object {
        private val parserOffsetRegex = Regex("\\b at (\\d+)\\b")

        fun fromSource(
            source: String,
            lexer: XbLexer = XbLexer(),
            parser: (String) -> XbParseResult = { text -> XbParser.parse(text) },
            psiBuilder: XbPsiTextBuilder = XbPsiTextBuilder(),
        ): XbInspectionContext {
            val lexResult = lexer.lex(source)
            val parseResult = parser(source)
            val psiFile = psiBuilder.build(source)
            return XbInspectionContext(source, lexResult, parseResult, psiFile)
        }
    }
}

interface XbInspectionRule {
    val id: String
    val title: String
    val description: String
    val defaultSeverity: XbInspectionSeverity

    fun inspect(context: XbInspectionContext): List<XbInspectionFinding>
}
