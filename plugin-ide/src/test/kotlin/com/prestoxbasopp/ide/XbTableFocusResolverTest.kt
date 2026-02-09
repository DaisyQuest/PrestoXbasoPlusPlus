package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbTableFocusResolverTest {
    @Test
    fun `resolves last selected table before caret`() {
        val source = """
            sele charges
            select invoices
            invoices->id
        """.trimIndent()

        val resolver = XbTableFocusResolver()
        val resolved = resolver.resolve(source, source.length)

        assertThat(resolved).isEqualTo("invoices")
    }

    @Test
    fun `returns null when no table focus command is present`() {
        val source = "return foo"

        val resolver = XbTableFocusResolver()

        assertThat(resolver.resolve(source, source.length)).isNull()
    }
}
