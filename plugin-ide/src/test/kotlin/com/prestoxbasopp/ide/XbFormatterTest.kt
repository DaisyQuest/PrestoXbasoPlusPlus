package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbFormatterTest {
    @Test
    fun `formats blocks with indentation`() {
        val source = """
            if condition
            return 1
            else
            return 2
            endif
        """.trimIndent()

        val formatted = XbFormatter().format(source, indentSize = 2)

        assertThat(formatted).isEqualTo(
            listOf(
                "if condition",
                "  return 1",
                "else",
                "  return 2",
                "endif",
            ).joinToString("\n"),
        )
    }

    @Test
    fun `keeps blank lines intact`() {
        val source = "if condition\n\nendif"
        val formatted = XbFormatter().format(source, indentSize = 4)

        assertThat(formatted).isEqualTo("if condition\n\nendif")
    }
}
