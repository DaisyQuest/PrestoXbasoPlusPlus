package com.prestoxbasopp.testframework.golden

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AstDumpFormatTest {
    @Test
    fun `renders leaf node without attributes`() {
        val node = AstDumpNode("Root")

        val rendered = AstDumpFormat.render(node)

        assertThat(rendered).isEqualTo("Root")
    }

    @Test
    fun `renders attributes sorted and escaped`() {
        val node = AstDumpNode(
            name = "Root",
            attributes = mapOf(
                "b" to "two words",
                "a" to "value\"x",
            ),
        )

        val rendered = AstDumpFormat.render(node)

        assertThat(rendered).isEqualTo("Root[a=\"value\\\"x\", b=\"two words\"]")
    }

    @Test
    fun `renders children with indentation`() {
        val node = AstDumpNode(
            name = "Root",
            children = listOf(
                AstDumpNode("Child"),
                AstDumpNode("ChildTwo", attributes = mapOf("k" to "v")),
            ),
        )

        val rendered = AstDumpFormat.render(node)

        assertThat(rendered).isEqualTo(
            """
            Root
              Child
              ChildTwo[k=v]
            """.trimIndent(),
        )
    }

    @Test
    fun `rejects blank node names`() {
        assertThatThrownBy { AstDumpNode(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("AST node name must not be blank")
    }

    @Test
    fun `rejects empty indent`() {
        val node = AstDumpNode("Root")

        assertThatThrownBy { AstDumpFormat.render(node, "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Indent must not be empty")
    }
}
