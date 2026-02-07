package com.prestoxbasopp.core.ast

import com.prestoxbasopp.core.api.XbTextRange

sealed interface XbAstNode {
    val range: XbTextRange
}

data class XbProgram(
    val statements: List<XbStatement>,
    override val range: XbTextRange,
) : XbAstNode

sealed interface XbStatement : XbAstNode

data class XbBlock(
    val statements: List<XbStatement>,
    override val range: XbTextRange,
) : XbStatement

data class XbLocalDeclarationStatement(
    val bindings: List<XbLocalBinding>,
    override val range: XbTextRange,
) : XbStatement

data class XbLocalBinding(
    val name: String,
    val initializer: XbExpression?,
    val range: XbTextRange,
)

data class XbExpressionStatement(
    val expression: XbExpression,
    override val range: XbTextRange,
) : XbStatement

data class XbAssignmentStatement(
    val target: XbExpression,
    val value: XbExpression,
    override val range: XbTextRange,
) : XbStatement

data class XbPrintStatement(
    val expressions: List<XbExpression>,
    override val range: XbTextRange,
) : XbStatement

data class XbReturnStatement(
    val expression: XbExpression?,
    override val range: XbTextRange,
) : XbStatement

data class XbIfStatement(
    val condition: XbExpression,
    val thenBlock: XbBlock,
    val elseBlock: XbBlock?,
    override val range: XbTextRange,
) : XbStatement

data class XbWhileStatement(
    val condition: XbExpression,
    val body: XbBlock,
    override val range: XbTextRange,
) : XbStatement

data class XbForStatement(
    val iterator: XbIdentifierExpression,
    val start: XbExpression,
    val end: XbExpression,
    val step: XbExpression,
    val body: XbBlock,
    override val range: XbTextRange,
) : XbStatement

data class XbFunctionDeclaration(
    val name: String,
    val parameters: List<String>,
    val body: XbBlock,
    override val range: XbTextRange,
) : XbStatement

data class XbProcedureDeclaration(
    val name: String,
    val parameters: List<String>,
    val body: XbBlock,
    override val range: XbTextRange,
) : XbStatement

sealed interface XbExpression : XbAstNode

enum class XbLiteralKind {
    NUMBER,
    STRING,
    NIL,
}

data class XbLiteralExpression(
    val kind: XbLiteralKind,
    val value: String,
    override val range: XbTextRange,
) : XbExpression

data class XbIdentifierExpression(
    val name: String,
    override val range: XbTextRange,
) : XbExpression

data class XbUnaryExpression(
    val operator: String,
    val expression: XbExpression,
    override val range: XbTextRange,
) : XbExpression

data class XbBinaryExpression(
    val operator: String,
    val left: XbExpression,
    val right: XbExpression,
    override val range: XbTextRange,
) : XbExpression

data class XbCallExpression(
    val callee: XbExpression,
    val arguments: List<XbExpression>,
    override val range: XbTextRange,
) : XbExpression

data class XbIndexExpression(
    val target: XbExpression,
    val index: XbExpression,
    override val range: XbTextRange,
) : XbExpression

data class XbArrayLiteralExpression(
    val elements: List<XbExpression>,
    override val range: XbTextRange,
) : XbExpression
