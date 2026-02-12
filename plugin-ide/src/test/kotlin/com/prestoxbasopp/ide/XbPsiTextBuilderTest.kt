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

    @Test
    fun `function ranges include end markers for nesting`() {
        val source = """
            function Main()
               local count
               return count
            endfunction
            local after
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val function = root.children.filterIsInstance<XbPsiFunctionDeclaration>().first()

        val expectedEnd = source.indexOf("endfunction") + "endfunction".length
        assertThat(function.textRange.endOffset).isEqualTo(expectedEnd)
    }

    @Test
    fun `function range is implicitly terminated by next declaration keyword`() {
        val source = """
            function AllFilesExist(aFiles)
               return .t.

            function CenterPos(aSize, aRefSize)
               return 1
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val functions = root.children.filterIsInstance<XbPsiFunctionDeclaration>()

        assertThat(functions).hasSize(2)
        val firstFunctionEndExpected = source.indexOf("function CenterPos") - 2
        assertThat(functions[0].symbolName).isEqualTo("AllFilesExist")
        assertThat(functions[0].textRange.endOffset).isEqualTo(firstFunctionEndExpected)
        assertThat(functions[0].text.trimEnd()).endsWith("return .t.")
        assertThat(functions[1].symbolName).isEqualTo("CenterPos")
    }

    @Test
    fun `procedure range is implicitly terminated by next declaration keyword`() {
        val source = """
            procedure ChangePos(oXbp, aDistance)
               return

            function Ok2SaveDlgSize(lSaveSize)
               return .t.
            endfunction
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val declarations = root.children.filterIsInstance<XbPsiFunctionDeclaration>()

        assertThat(declarations.map { it.symbolName }).containsExactly("ChangePos", "Ok2SaveDlgSize")
        val firstDeclarationEndExpected = source.indexOf("function Ok2SaveDlgSize") - 2
        assertThat(declarations[0].textRange.endOffset).isEqualTo(firstDeclarationEndExpected)
        assertThat(declarations[0].text.trimEnd()).endsWith("return")
    }

    @Test
    fun `declaration ranges close on explicit end markers even when mismatched`() {
        val source = """
            function Main()
               return 1
            endproc
            local afterMain
        """.trimIndent()

        val root = XbPsiTextBuilder().build(source)
        val function = root.children.filterIsInstance<XbPsiFunctionDeclaration>().first()
        val afterMain = root.children.filterIsInstance<XbPsiVariableDeclaration>().first { it.symbolName == "afterMain" }

        assertThat(function.symbolName).isEqualTo("Main")
        assertThat(function.textRange.endOffset).isLessThan(afterMain.textRange.startOffset)
        assertThat(function.text.trimEnd()).endsWith("endproc")
    }
}
