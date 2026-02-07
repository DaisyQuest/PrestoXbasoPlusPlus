package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbArrayLiteralExpression
import com.prestoxbasopp.core.ast.XbAssignmentStatement
import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlock
import com.prestoxbasopp.core.ast.XbCallExpression
import com.prestoxbasopp.core.ast.XbExpression
import com.prestoxbasopp.core.ast.XbExpressionStatement
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbIndexExpression
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbLiteralKind
import com.prestoxbasopp.core.ast.XbLocalDeclarationStatement
import com.prestoxbasopp.core.ast.XbPrintStatement
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
import com.prestoxbasopp.core.ast.XbProgram
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.testframework.golden.AstDumpNode

fun XbProgram.toDumpNode(): AstDumpNode {
    val collapsedExpression = statements.singleOrNull() as? XbExpressionStatement
    if (collapsedExpression != null) {
        return AstDumpNode(
            name = "File",
            children = listOf(collapsedExpression.expression.toDumpNode()),
        )
    }
    return AstDumpNode(
        name = "File",
        children = statements.map { it.toDumpNode() },
    )
}

private fun XbStatement.toDumpNode(): AstDumpNode {
    return when (this) {
        is XbExpressionStatement -> AstDumpNode(
            name = "Stmt.Expression",
            children = listOf(expression.toDumpNode()),
        )
        is XbAssignmentStatement -> AstDumpNode(
            name = "Stmt.Assignment",
            children = listOf(target.toDumpNode(), value.toDumpNode()),
        )
        is XbPrintStatement -> AstDumpNode(
            name = "Stmt.Print",
            children = expressions.map { it.toDumpNode() },
        )
        is XbLocalDeclarationStatement -> AstDumpNode(
            name = "Stmt.Local",
            children = bindings.map { binding ->
                AstDumpNode(
                    name = "Local.Binding",
                    attributes = mapOf("name" to binding.name),
                    children = binding.initializer?.let { listOf(it.toDumpNode()) } ?: emptyList(),
                )
            },
        )
        is XbReturnStatement -> AstDumpNode(
            name = "Stmt.Return",
            children = expression?.let { listOf(it.toDumpNode()) } ?: emptyList(),
        )
        is XbIfStatement -> AstDumpNode(
            name = "Stmt.If",
            children = buildList {
                add(condition.toDumpNode())
                add(thenBlock.toDumpNode(branch = "then"))
                val elseNode = elseBlock ?: XbBlock(emptyList(), thenBlock.range)
                add(elseNode.toDumpNode(branch = "else"))
            },
        )
        is XbWhileStatement -> AstDumpNode(
            name = "Stmt.While",
            children = listOf(
                condition.toDumpNode(),
                body.toDumpNode(),
            ),
        )
        is XbForStatement -> AstDumpNode(
            name = "Stmt.For",
            children = buildList {
                add(iterator.toDumpNode())
                add(start.toDumpNode())
                add(end.toDumpNode())
                add(step.toDumpNode())
                if (body.statements.isNotEmpty()) {
                    add(body.toDumpNode())
                }
            },
        )
        is XbFunctionDeclaration -> AstDumpNode(
            name = "Decl.Function",
            attributes = mapOf("name" to name),
            children = listOf(
                AstDumpNode(
                    name = "Params",
                    children = parameters.map { param ->
                        AstDumpNode(name = "Expr.Identifier", attributes = mapOf("name" to param))
                    },
                ),
                body.toDumpNode(),
            ),
        )
        is XbProcedureDeclaration -> AstDumpNode(
            name = "Decl.Procedure",
            attributes = mapOf("name" to name),
            children = listOf(
                AstDumpNode(
                    name = "Params",
                    children = parameters.map { param ->
                        AstDumpNode(name = "Expr.Identifier", attributes = mapOf("name" to param))
                    },
                ),
                body.toDumpNode(),
            ),
        )
        is XbBlock -> this.toDumpNode()
        else -> AstDumpNode(name = "Stmt.Unknown")
    }
}

private fun XbBlock.toDumpNode(branch: String? = null): AstDumpNode {
    val attributes = branch?.let { mapOf("branch" to it) } ?: emptyMap()
    return AstDumpNode(
        name = "Block",
        attributes = attributes,
        children = statements.map { it.toDumpNode() },
    )
}

private fun XbExpression.toDumpNode(): AstDumpNode {
    return when (this) {
        is XbLiteralExpression -> when (kind) {
            XbLiteralKind.NUMBER -> AstDumpNode(
                name = "Expr.Literal.Number",
                attributes = mapOf("value" to value),
            )
            XbLiteralKind.STRING -> AstDumpNode(
                name = "Expr.Literal.String",
                attributes = mapOf("value" to value),
            )
            XbLiteralKind.NIL -> AstDumpNode(
                name = "Expr.Literal.Nil",
            )
        }
        is XbIdentifierExpression -> AstDumpNode(
            name = "Expr.Identifier",
            attributes = mapOf("name" to name),
        )
        is XbUnaryExpression -> AstDumpNode(
            name = when (operator.lowercase()) {
                "-" -> "Expr.Unary.Negation"
                "not" -> "Expr.Unary.Not"
                else -> "Expr.Unary"
            },
            attributes = if (operator.lowercase() in setOf("-", "not")) emptyMap() else mapOf("op" to operator),
            children = listOf(expression.toDumpNode()),
        )
        is XbBinaryExpression -> AstDumpNode(
            name = when (operator.lowercase()) {
                "+" -> "Expr.Binary.Add"
                "-" -> "Expr.Binary.Subtract"
                "*" -> "Expr.Binary.Multiply"
                "/" -> "Expr.Binary.Divide"
                "==" -> "Expr.Binary.Equal"
                "!=" -> "Expr.Binary.NotEqual"
                "<" -> "Expr.Binary.LessThan"
                "<=" -> "Expr.Binary.LessThanOrEqual"
                ">" -> "Expr.Binary.GreaterThan"
                ">=" -> "Expr.Binary.GreaterThanOrEqual"
                "and" -> "Expr.Binary.And"
                "or" -> "Expr.Binary.Or"
                else -> "Expr.Binary"
            },
            attributes = if (operator.lowercase() in setOf(
                    "+",
                    "-",
                    "*",
                    "/",
                    "==",
                    "!=",
                    "<",
                    "<=",
                    ">",
                    ">=",
                    "and",
                    "or",
                )
            ) {
                emptyMap()
            } else {
                mapOf("op" to operator)
            },
            children = listOf(left.toDumpNode(), right.toDumpNode()),
        )
        is XbCallExpression -> AstDumpNode(
            name = "Expr.Call",
            children = listOf(callee.toDumpNode()) + arguments.map { it.toDumpNode() },
        )
        is XbIndexExpression -> AstDumpNode(
            name = "Expr.Index",
            children = listOf(target.toDumpNode(), index.toDumpNode()),
        )
        is XbArrayLiteralExpression -> AstDumpNode(
            name = "Expr.ArrayLiteral",
            children = elements.map { it.toDumpNode() },
        )
        else -> AstDumpNode(name = "Expr.Unknown")
    }
}
