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

data class XbExpressionStatement(
    val expression: XbExpression,
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

sealed interface XbExpression : XbAstNode

enum class XbLiteralKind {
    NUMBER,
    STRING,
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
