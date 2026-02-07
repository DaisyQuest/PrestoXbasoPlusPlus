package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
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
}
