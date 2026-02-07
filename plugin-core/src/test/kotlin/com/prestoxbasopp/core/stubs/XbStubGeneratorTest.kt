package com.prestoxbasopp.core.stubs

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiLiteral
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbStubGeneratorTest {
    @Test
    fun `generates stub for function declaration`() {
        val element = XbPsiFunctionDeclaration(
            symbolName = "run",
            parameters = emptyList(),
            textRange = XbTextRange(0, 3),
            text = "run",
        )

        val stub = XbStubGenerator.from(element, namespace = listOf("global", ""))

        assertThat(stub).isNotNull
        assertThat(stub?.type).isEqualTo(XbStubType.FUNCTION)
        assertThat(stub?.fqName).isEqualTo("global.run")
        assertThat(stub?.stubId).isEqualTo("FUNCTION:global.run")
    }

    @Test
    fun `generates stub for variable declaration`() {
        val element = XbPsiVariableDeclaration(
            symbolName = "value",
            isMutable = false,
            textRange = XbTextRange(0, 5),
            text = "value",
        )

        val stub = XbStubGenerator.from(element)

        assertThat(stub?.type).isEqualTo(XbStubType.VARIABLE)
        assertThat(stub?.name).isEqualTo("value")
    }

    @Test
    fun `returns null when name is blank`() {
        val element = XbPsiVariableDeclaration(
            symbolName = " ",
            isMutable = true,
            textRange = XbTextRange(0, 1),
            text = "x",
        )

        val stub = XbStubGenerator.from(element)

        assertThat(stub).isNull()
    }

    @Test
    fun `returns null for non-declaration elements`() {
        val reference = XbPsiSymbolReference(
            symbolName = "ref",
            textRange = XbTextRange(0, 3),
            text = "ref",
        )
        val literal = XbPsiLiteral(
            literalKind = "number",
            textRange = XbTextRange(0, 1),
            text = "1",
        )

        assertThat(XbStubGenerator.from(reference)).isNull()
        assertThat(XbStubGenerator.from(literal)).isNull()
    }
}
