package com.prestoxbasopp.ide.inspections

import com.prestoxbasopp.core.ast.XbBinaryExpression
import com.prestoxbasopp.core.ast.XbBlockLiteralExpression
import com.prestoxbasopp.core.ast.XbHashLiteralExpression
import com.prestoxbasopp.core.ast.XbIdentifierExpression
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbLiteralExpression
import com.prestoxbasopp.core.ast.XbReturnStatement
import com.prestoxbasopp.core.ast.XbSequenceStatement
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
    fun `walkStatements descends into sequence statements`() {
        val context = XbInspectionContext.fromSource(
            """
            BEGIN SEQUENCE
               RETURN 1
            RECOVER USING oErr
               RETURN 2
            END SEQUENCE
            """.trimIndent(),
        )

        val statements = context.walkStatements().toList()

        assertThat(statements.filterIsInstance<XbSequenceStatement>()).hasSize(1)
        assertThat(statements.filterIsInstance<XbReturnStatement>()).hasSize(2)
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
    fun `walkExpressions includes literals from complex statements`() {
        val context = XbInspectionContext.fromSource(
            """
            LOCAL h := {=>}
            LOCAL b := {|x| x + 1 }
            @ 1, 2 SAY "Hi" GET cInput VALID cInput > 0
            WAIT "done"
            BREAK 1
            """.trimIndent(),
        )

        val expressions = context.walkExpressions().toList()

        assertThat(expressions.filterIsInstance<XbHashLiteralExpression>()).hasSize(1)
        assertThat(expressions.filterIsInstance<XbBlockLiteralExpression>()).hasSize(1)
        assertThat(expressions.filterIsInstance<XbIdentifierExpression>()).anyMatch { it.name == "cInput" }
        assertThat(expressions.filterIsInstance<XbLiteralExpression>().map { it.value })
            .contains("Hi", "done", "1", "2")
    }

    @Test
    fun `formatMessage normalizes parser and lexer messages`() {
        val context = XbInspectionContext.fromSource("return 1")

        assertThat(context.formatMessage("   "))
            .isEqualTo("Unknown issue.")
        assertThat(context.formatMessage("Unexpected character '!'"))
            .isEqualTo("Unexpected character: `!`.")
        assertThat(context.formatMessage("Unexpected character $"))
            .isEqualTo("Unexpected character: $.")
        assertThat(context.formatMessage("Already punctuated."))
            .isEqualTo("Already punctuated.")
        assertThat(context.formatMessage("Ends with exclamation!"))
            .isEqualTo("Ends with exclamation!")
        assertThat(context.formatMessage("Missing endif"))
            .isEqualTo("Missing endif.")
    }

    @Test
    fun `fromLexOnly skips parser and psi construction`() {
        val context = XbInspectionContext.fromLexOnly("return 1")

        assertThat(context.parseResult.program).isNull()
        assertThat(context.parseResult.errors).isEmpty()
        assertThat(context.psiFile).isNull()
        assertThat(context.tokens).isNotEmpty()
    }

}
