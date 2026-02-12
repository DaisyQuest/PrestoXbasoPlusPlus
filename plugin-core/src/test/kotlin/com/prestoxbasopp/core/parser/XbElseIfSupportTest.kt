package com.prestoxbasopp.core.parser

import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbReturnStatement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbElseIfSupportTest {
    @Test
    fun `tokenizes elseif as dedicated keyword`() {
        val tokens = XbLexer("if x elseif y endif").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IF,
            TokenType.IDENTIFIER,
            TokenType.ELSEIF,
            TokenType.IDENTIFIER,
            TokenType.ENDIF,
        )
    }

    @Test
    fun `parses elseif chain as nested else branch`() {
        val source = """
            IF x
                RETURN 1
            ELSEIF y
                RETURN 2
            ELSE
                RETURN 3
            ENDIF
        """.trimIndent()

        val result = XbParser.parse(source)

        assertThat(result.errors).isEmpty()
        val topLevel = result.program!!.statements.single() as XbIfStatement
        val elseIf = topLevel.elseBlock!!.statements.single() as XbIfStatement

        assertThat(topLevel.condition).hasFieldOrPropertyWithValue("name", "x")
        assertThat((topLevel.thenBlock.statements.single() as XbReturnStatement).expression)
            .hasFieldOrPropertyWithValue("value", "1")

        assertThat(elseIf.condition).hasFieldOrPropertyWithValue("name", "y")
        assertThat((elseIf.thenBlock.statements.single() as XbReturnStatement).expression)
            .hasFieldOrPropertyWithValue("value", "2")
        assertThat((elseIf.elseBlock!!.statements.single() as XbReturnStatement).expression)
            .hasFieldOrPropertyWithValue("value", "3")
    }

    @Test
    fun `reports missing elseif condition and recovers to endif`() {
        val result = XbParser.parse(
            """
                IF x
                    RETURN 1
                ELSEIF
                    RETURN 2
                ENDIF
            """.trimIndent(),
        )

        assertThat(result.errors).anyMatch { it.contains("Expected condition after ELSEIF") }
        assertThat(result.program).isNotNull
    }
}
