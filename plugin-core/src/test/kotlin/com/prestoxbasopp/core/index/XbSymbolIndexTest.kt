package com.prestoxbasopp.core.index

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.core.psi.XbPsiLiteral
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.stubs.XbStubType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbSymbolIndexTest {
    @Test
    fun `indexes declarations and usages`() {
        val function = XbPsiFunctionDeclaration(
            symbolName = "compute",
            parameters = listOf("x"),
            textRange = XbTextRange(0, 7),
            text = "compute",
        )
        val reference = XbPsiSymbolReference(
            symbolName = "compute",
            textRange = XbTextRange(7, 14),
            text = "compute",
        )
        val file = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 14),
            text = "computecompute",
            children = listOf(function, reference),
        )

        val index = XbSymbolIndex()
        index.index(file, namespace = listOf("module"))

        val declarations = index.findDeclarations("compute", XbStubType.FUNCTION)
        val usages = index.findUsages("compute")

        assertThat(declarations).hasSize(1)
        assertThat(declarations.first().fqName).isEqualTo("module.compute")
        assertThat(usages).hasSize(1)
        assertThat(usages.first().symbolName).isEqualTo("compute")
    }

    @Test
    fun `skips blank usage names`() {
        val usage = XbPsiSymbolReference(
            symbolName = " ",
            textRange = XbTextRange(0, 1),
            text = "x",
        )

        val index = XbSymbolIndex()
        index.indexElement(usage)

        assertThat(index.findUsages("")).isEmpty()
    }

    @Test
    fun `findAll returns both declarations and usages`() {
        val declaration = XbPsiVariableDeclaration(
            symbolName = "value",
            isMutable = true,
            textRange = XbTextRange(0, 5),
            text = "value",
        )
        val usage = XbPsiSymbolReference(
            symbolName = "value",
            textRange = XbTextRange(5, 10),
            text = "value",
        )
        val file = XbPsiFile(
            name = null,
            textRange = XbTextRange(0, 10),
            text = "valuevalue",
            children = listOf(declaration, usage),
        )

        val index = XbSymbolIndex()
        index.index(file)

        val result = index.findAll("value", XbStubType.VARIABLE)

        assertThat(result.declarations).hasSize(1)
        assertThat(result.usages).hasSize(1)
    }

    @Test
    fun `non-symbol elements do not affect indexes`() {
        val literal = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 1),
            text = "1",
        )

        val index = XbSymbolIndex()
        index.indexElement(literal)

        assertThat(index.findAll("1", XbStubType.FUNCTION).declarations).isEmpty()
    }
}
