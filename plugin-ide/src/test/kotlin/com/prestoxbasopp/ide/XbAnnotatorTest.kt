package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbAnnotatorTest {
    @Test
    fun `reports unterminated string literals`() {
        val diagnostics = XbAnnotator().annotate("\"unterminated")
        assertThat(diagnostics).hasSize(1)
        assertThat(diagnostics.first().message).isEqualTo("Unterminated string literal.")
        assertThat(diagnostics.first().severity).isEqualTo(XbSeverity.ERROR)
    }

    @Test
    fun `reports unexpected characters`() {
        val diagnostics = XbAnnotator().annotate("!")
        assertThat(diagnostics).hasSize(1)
        assertThat(diagnostics.first().message).isEqualTo("Unexpected character: '!'.")
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
    fun `ignores preprocessor directives`() {
        val diagnostics = XbAnnotator().annotate("#define FOO 1\nreturn FOO;")
        assertThat(diagnostics).isEmpty()
    }
}
