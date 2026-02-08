package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import com.intellij.icons.AllIcons
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbStructureViewTest {
    @Test
    fun `builds structure view with fallback names`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = null,
            textRange = XbTextRange(0, 20),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.FUNCTION_DECLARATION,
                    name = "main",
                    textRange = XbTextRange(0, 10),
                    text = "function main()",
                    parameters = listOf("arg1", "arg2"),
                ),
                XbPsiSnapshot(
                    elementType = XbPsiElementType.BLOCK,
                    name = null,
                    textRange = XbTextRange(11, 20),
                    text = "{ }",
                ),
            ),
        )

        val structure = XbStructureViewBuilder().build(snapshot)

        assertThat(structure.name).isEqualTo("file")
        assertThat(structure.children.map { it.name }).containsExactly("main(arg1, arg2)")
    }

    @Test
    fun `computes breadcrumbs for matching offset`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 100),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.FUNCTION_DECLARATION,
                    name = "main",
                    textRange = XbTextRange(10, 50),
                    text = "function main()",
                ),
            ),
        )

        val breadcrumbs = XbBreadcrumbsService().breadcrumbs(snapshot, 20)
        assertThat(breadcrumbs.map { it.name }).containsExactly("root", "main")
    }

    @Test
    fun `promotes nested function declarations into the structure tree`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 200),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.BLOCK,
                    name = null,
                    textRange = XbTextRange(0, 100),
                    text = "{ }",
                    children = listOf(
                        XbPsiSnapshot(
                            elementType = XbPsiElementType.FUNCTION_DECLARATION,
                            name = "inner",
                            textRange = XbTextRange(10, 90),
                            text = "function inner()",
                        ),
                    ),
                ),
            ),
        )

        val structure = XbStructureViewBuilder().build(snapshot)

        assertThat(structure.children.map { it.name }).containsExactly("inner")
    }

    @Test
    fun `includes variable declarations in the structure tree`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 50),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.VARIABLE_DECLARATION,
                    name = "count",
                    textRange = XbTextRange(10, 15),
                    text = "count",
                ),
            ),
        )

        val structure = XbStructureViewBuilder().build(snapshot)

        assertThat(structure.children.map { it.name }).containsExactly("count")
    }

    @Test
    fun `labels static variables distinctly in the structure tree`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 50),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.VARIABLE_DECLARATION,
                    name = "flag",
                    textRange = XbTextRange(10, 15),
                    text = "flag",
                    isMutable = false,
                ),
            ),
        )

        val structure = XbStructureViewBuilder().build(snapshot)

        assertThat(structure.children.map { it.name }).containsExactly("static flag")
        assertThat(structure.children.first().isMutable).isFalse
    }

    @Test
    fun `nests variables inside their function scope`() {
        val content = XbStructureViewFileContent(
            fileName = "sample.prg",
            text = """
                function Main()
                   local count
                endfunction
                local after
            """.trimIndent(),
        )

        val root = XbStructureViewRootBuilder().buildRoot(content)

        assertThat(root.children.map { it.name }).containsExactly("Main", "after")
        val functionItem = root.children.first()
        assertThat(functionItem.children.map { it.name }).containsExactly("count")
    }

    @Test
    fun `returns empty breadcrumbs when offset is outside`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 10),
            text = "file",
        )

        val breadcrumbs = XbBreadcrumbsService().breadcrumbs(snapshot, 99)
        assertThat(breadcrumbs).isEmpty()
    }

    @Test
    fun `prefers editor text when resolving structure view content`() {
        val resolver = XbStructureViewFileContentResolver()

        val content = resolver.resolve(
            fileName = "current.prg",
            psiText = "function fromPsi()",
            editorText = "function fromEditor()",
        )

        assertThat(content.fileName).isEqualTo("current.prg")
        assertThat(content.text).isEqualTo("function fromEditor()")
    }

    @Test
    fun `falls back to psi text when editor text is unavailable`() {
        val resolver = XbStructureViewFileContentResolver()

        val content = resolver.resolve(
            fileName = "current.prg",
            psiText = "function fromPsi()",
            editorText = null,
        )

        assertThat(content.fileName).isEqualTo("current.prg")
        assertThat(content.text).isEqualTo("function fromPsi()")
    }

    @Test
    fun `builds structure view root from resolved content`() {
        val content = XbStructureViewFileContent(
            fileName = "focused.prg",
            text = "function main()\nreturn",
        )

        val root = XbStructureViewRootBuilder().buildRoot(content)

        assertThat(root.name).isEqualTo("focused.prg")
        assertThat(root.children.map { it.name }).containsExactly("main")
    }

    @Test
    fun `maps structure view icons by element type`() {
        val functionItem = XbStructureItem(
            name = "main",
            elementType = XbPsiElementType.FUNCTION_DECLARATION,
            textRange = XbTextRange(0, 10),
            isMutable = null,
            children = emptyList(),
        )
        val mutableVariableItem = XbStructureItem(
            name = "count",
            elementType = XbPsiElementType.VARIABLE_DECLARATION,
            textRange = XbTextRange(11, 15),
            isMutable = true,
            children = emptyList(),
        )
        val staticVariableItem = XbStructureItem(
            name = "total",
            elementType = XbPsiElementType.VARIABLE_DECLARATION,
            textRange = XbTextRange(16, 20),
            isMutable = false,
            children = emptyList(),
        )

        assertThat(XbStructureViewPresentation.iconFor(functionItem)).isEqualTo(AllIcons.Nodes.Function)
        assertThat(XbStructureViewPresentation.iconFor(mutableVariableItem)).isEqualTo(AllIcons.Nodes.Variable)
        assertThat(XbStructureViewPresentation.iconFor(staticVariableItem)).isEqualTo(AllIcons.Nodes.Constant)
    }
}
