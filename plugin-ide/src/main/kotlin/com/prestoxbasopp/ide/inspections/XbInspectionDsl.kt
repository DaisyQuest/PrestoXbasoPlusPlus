package com.prestoxbasopp.ide.inspections

import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.lexer.XbLexerError
import com.prestoxbasopp.core.lexer.XbToken

class XbInspectionEmitter internal constructor(
    private val id: String,
    private val title: String,
    private val severity: XbInspectionSeverity,
) {
    internal val findings: MutableList<XbInspectionFinding> = mutableListOf()

    fun report(range: com.prestoxbasopp.core.api.XbTextRange, message: String) {
        findings += XbInspectionFinding(id, title, message, severity, range)
    }
}

class XbInspectionBuilder internal constructor(
    private val id: String,
    private val title: String,
) {
    var description: String = ""
    var severity: XbInspectionSeverity = XbInspectionSeverity.WARNING
    private val checks = mutableListOf<(XbInspectionContext, XbInspectionEmitter) -> Unit>()

    fun onLexerErrors(handler: (XbLexerError, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter ->
            context.lexerErrors.forEach { error -> handler(error, emitter, context) }
        }
    }

    fun onParserIssues(handler: (XbParserIssue, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter ->
            context.parserIssues.forEach { issue -> handler(issue, emitter, context) }
        }
    }

    fun onTokens(handler: (List<XbToken>, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter -> handler(context.tokens, emitter, context) }
    }

    fun onAst(handler: (XbProgram, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter -> context.program?.let { handler(it, emitter, context) } }
    }

    fun onStatements(handler: (XbStatement, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter ->
            context.walkStatements().forEach { statement -> handler(statement, emitter, context) }
        }
    }

    fun onExpressions(handler: (XbExpression, XbInspectionEmitter, XbInspectionContext) -> Unit) {
        checks += { context, emitter ->
            context.walkExpressions().forEach { expression -> handler(expression, emitter, context) }
        }
    }

    internal fun build(): XbInspectionRule {
        val descriptionValue = description
        val severityValue = severity
        val checksSnapshot = checks.toList()
        return object : XbInspectionRule {
            override val id: String = this@XbInspectionBuilder.id
            override val title: String = this@XbInspectionBuilder.title
            override val description: String = descriptionValue
            override val defaultSeverity: XbInspectionSeverity = severityValue

            override fun inspect(context: XbInspectionContext): List<XbInspectionFinding> {
                val emitter = XbInspectionEmitter(id, title, defaultSeverity)
                checksSnapshot.forEach { check -> check(context, emitter) }
                return emitter.findings.toList()
            }
        }
    }
}

fun xbInspection(id: String, title: String, init: XbInspectionBuilder.() -> Unit): XbInspectionRule {
    val builder = XbInspectionBuilder(id, title)
    builder.init()
    return builder.build()
}
