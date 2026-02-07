package com.prestoxbasopp.ide.inspections

import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbUnaryExpression
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionContextTest {
    @Test
    fun `walkStatements includes nested statements`() {
        val context = XbInspectionContext.fromSource("if 1 then return 2; endif")

        val statements = context.walkStatements().toList()

        assertThat(statements.filterIsInstance<XbIfStatement>()).hasSize(1)
        assertThat(statements.filterIsInstance<XbReturnStatement>()).hasSize(1)
    }

    @Test
    fun `walkExpressions descends into unary and binary expressions`() {
        val context = XbInspectionContext.fromSource("if -1 + foo * 2 then return not bar; endif")

        val expressions = context.walkExpressions().toList()

        assertThat(expressions.filterIsInstance<XbBinaryExpression>()).hasSize(2)
        assertThat(expressions.filterIsInstance<XbUnaryExpression>()).hasSize(2)
        assertThat(expressions.filterIsInstance<XbIdentifierExpression>()).hasSize(2)
        assertThat(expressions.filterIsInstance<XbLiteralExpression>()).hasSize(2)
    }

    @Test
    fun `formatMessage normalizes parser and lexer messages`() {
        val context = XbInspectionContext.fromSource("return 1")

        assertThat(context.formatMessage("Unexpected character '!'"))
            .isEqualTo("Unexpected character: '!'.")
        assertThat(context.formatMessage("Already punctuated."))
            .isEqualTo("Already punctuated.")
        assertThat(context.formatMessage("Missing endif"))
            .isEqualTo("Missing endif.")
    }
}
