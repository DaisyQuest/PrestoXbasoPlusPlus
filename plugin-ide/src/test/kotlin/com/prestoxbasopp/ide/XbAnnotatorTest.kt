package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbAnnotatorTest {
    @Test
    fun `reports unterminated string literals`() {
        val diagnostics = XbAnnotator().annotate("\"unterminated")
        assertThat(diagnostics).hasSize(2)
        assertThat(diagnostics.map { it.message }).containsExactly(
            "Unexpected token '\"unterminated' at 0.",
            "Unterminated string literal.",
        )
        assertThat(diagnostics.all { it.severity == XbSeverity.ERROR }).isTrue()
    }

    @Test
    fun `reports parse errors for incomplete unary expressions`() {
        val diagnostics = XbAnnotator().annotate("!")
        assertThat(diagnostics).hasSize(2)
        assertThat(diagnostics.map { it.message }).containsExactly(
            "Expected expression after unary 'not' at 1.",
            "Unexpected token EOF at 1.",
        )
        assertThat(diagnostics.all { it.severity == XbSeverity.ERROR }).isTrue()
    }

    @Test
    fun `reports lexer diagnostics for unsupported characters`() {
        val diagnostics = XbAnnotator().annotate("~")

        assertThat(diagnostics).isNotEmpty()
        assertThat(diagnostics.map { it.message }).anyMatch { it == "Unexpected character: `~`." }
        assertThat(diagnostics.any { it.severity == XbSeverity.ERROR }).isTrue()
    }

    @Test
    fun `returns no diagnostics for valid input`() {
        val diagnostics = XbAnnotator().annotate("if foo == 10 then return 1 endif")
        assertThat(diagnostics).isEmpty()
    }

    @Test
    fun `reports warnings from inspections`() {
        val diagnostics = XbAnnotator().annotate("if 1 then return 1 endif")
        assertThat(diagnostics).hasSize(1)
        assertThat(diagnostics.first().severity).isEqualTo(XbSeverity.WARNING)
        assertThat(diagnostics.first().message).isEqualTo("IF condition is always constant.")
    }

    @Test
    fun `reports info diagnostics from inspections`() {
        val diagnostics = XbAnnotator().annotate("return 1;\nreturn 2")
        assertThat(diagnostics.map { it.message }).contains("Statement continues on the next line.")
        assertThat(diagnostics.any { it.severity == XbSeverity.INFO }).isTrue()
    }

    @Test
    fun `ignores preprocessor directives`() {
        val diagnostics = XbAnnotator().annotate("#define FOO 1\nreturn FOO;")
        assertThat(diagnostics).isEmpty()
    }
}
