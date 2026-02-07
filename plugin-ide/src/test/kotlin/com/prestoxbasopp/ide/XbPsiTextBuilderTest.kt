package com.prestoxbasopp.ide

import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbPsiTextBuilderTest {
    @Test
    fun `builds function and variable declarations`() {
        val source = """
            function Main(user, mode)
               local count, total
               total := count + 1
               Main(user, mode)
               return total
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source, "sample.xb")
        val functions = root.children.filterIsInstance<XbPsiFunctionDeclaration>()
        val variables = root.children.filterIsInstance<XbPsiVariableDeclaration>()
        val references = root.children.filterIsInstance<XbPsiSymbolReference>()

        assertThat(functions.map { it.symbolName }).containsExactly("Main")
        assertThat(functions.first().parameters).containsExactly("user", "mode")
        assertThat(variables.map { it.symbolName }).containsExactlyInAnyOrder("count", "total")
        assertThat(references.map { it.symbolName }).contains("total", "count", "Main", "user", "mode")
    }

    @Test
    fun `builds procedure declarations`() {
        val source = """
            procedure LogStatus()
               ? "ok"
            return
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val functions = root.children.filterIsInstance<XbPsiFunctionDeclaration>()

        assertThat(functions).hasSize(1)
        assertThat(functions.first().symbolName).isEqualTo("LogStatus")
    }
}
