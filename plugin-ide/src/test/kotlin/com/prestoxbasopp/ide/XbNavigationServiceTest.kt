package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.stubs.XbStubType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbNavigationServiceTest {
    @Test
    fun `finds declarations and usages`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 30),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "main",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 10),
                    text = "function main()",
                ),
                XbPsiVariableDeclaration(
                    symbolName = "count",
                    isMutable = true,
                    textRange = XbTextRange(11, 20),
                    text = "var count",
                ),
                XbPsiSymbolReference(
                    symbolName = "main",
                    textRange = XbTextRange(21, 25),
                    text = "main",
                ),
            ),
        )

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val declarations = service.findDeclarations("main", XbStubType.FUNCTION, index)
        val usages = service.findUsages("main", index)
        val functionTargets = service.findFunctionTargets("main", index)
        val jumpTarget = service.jumpToFunctionDeclaration("main", index)
        val functionUsages = service.findFunctionUsages("main", index)

        assertThat(declarations).hasSize(1)
        assertThat(usages).hasSize(1)
        assertThat(usages.first().symbolName).isEqualTo("main")
        assertThat(functionTargets.declarations).hasSize(1)
        assertThat(functionTargets.usages).hasSize(1)
        assertThat(jumpTarget?.name).isEqualTo("main")
        assertThat(functionUsages).hasSize(1)
    }

    @Test
    fun `returns empty results for unknown symbols`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 10),
            text = "file",
        )

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val targets = service.findAll("missing", XbStubType.FUNCTION, index)
        assertThat(targets.declarations).isEmpty()
        assertThat(targets.usages).isEmpty()
    }

    @Test
    fun `function navigation ignores non-function-only symbols`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 20),
            text = "file",
            children = listOf(
                XbPsiVariableDeclaration(
                    symbolName = "onlyVar",
                    isMutable = false,
                    textRange = XbTextRange(0, 10),
                    text = "var onlyVar",
                ),
                XbPsiSymbolReference(
                    symbolName = "onlyVar",
                    textRange = XbTextRange(11, 18),
                    text = "onlyVar",
                ),
            ),
        )

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val targets = service.findFunctionTargets("onlyVar", index)
        val jumpTarget = service.jumpToFunctionDeclaration("onlyVar", index)

        assertThat(targets.declarations).isEmpty()
        assertThat(targets.usages).isEmpty()
        assertThat(jumpTarget).isNull()
    }

    @Test
    fun `ctrl click on function definition returns usages`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 40),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "main",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 10),
                    text = "function main()",
                ),
                XbPsiSymbolReference(
                    symbolName = "main",
                    textRange = XbTextRange(20, 24),
                    text = "main",
                ),
            ),
        )
        val snapshot = XbPsiSnapshot.fromElement(root.children.first())

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val usages = service.findUsagesFromDefinition(snapshot, index)

        assertThat(usages).hasSize(1)
        assertThat(usages.first().symbolName).isEqualTo("main")
    }

    @Test
    fun `ctrl click on invocation jumps to definition`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 40),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "main",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 10),
                    text = "function main()",
                ),
                XbPsiSymbolReference(
                    symbolName = "main",
                    textRange = XbTextRange(20, 24),
                    text = "main",
                ),
            ),
        )
        val snapshot = XbPsiSnapshot.fromElement(root.children.last())

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val target = service.jumpToDefinitionFromInvocation(snapshot, index)

        assertThat(target?.name).isEqualTo("main")
    }

    @Test
    fun `ctrl click navigation returns empty when symbol name is missing`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 10),
            text = "file",
        )
        val snapshot = XbPsiSnapshot(
            elementType = com.prestoxbasopp.core.psi.XbPsiElementType.FUNCTION_DECLARATION,
            name = " ",
            textRange = XbTextRange(0, 1),
            text = "function",
        )

        val service = XbNavigationService()
        val index = service.buildIndex(root)

        val usages = service.findUsagesFromDefinition(snapshot, index)
        val target = service.jumpToDefinitionFromInvocation(snapshot, index)

        assertThat(usages).isEmpty()
        assertThat(target).isNull()
    }
}
