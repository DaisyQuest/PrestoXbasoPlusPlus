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
        val diagnostics = XbAnnotator().annotate("if foo == 10 return 1")
        assertThat(diagnostics).isEmpty()
    }
}
