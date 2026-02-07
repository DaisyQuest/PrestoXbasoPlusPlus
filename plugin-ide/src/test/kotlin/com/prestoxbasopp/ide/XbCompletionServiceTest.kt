package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbCompletionServiceTest {
    @Test
    fun `suggests completions with prefix filtering`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 30),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "Alpha",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 10),
                    text = "function Alpha()",
                ),
                XbPsiVariableDeclaration(
                    symbolName = "Beta",
                    isMutable = false,
                    textRange = XbTextRange(11, 20),
                    text = "var Beta",
                ),
                XbPsiVariableDeclaration(
                    symbolName = "Alpha",
                    isMutable = true,
                    textRange = XbTextRange(21, 30),
                    text = "var Alpha",
                ),
            ),
        )

        val items = XbCompletionService().suggest(root, "Al", caseSensitive = true)
        assertThat(items).containsExactly(
            XbCompletionItem("Alpha", XbCompletionType.FUNCTION),
            XbCompletionItem("Alpha", XbCompletionType.VARIABLE),
        )
    }

    @Test
    fun `handles case insensitive completion`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 10),
            text = "file",
            children = listOf(
                XbPsiFunctionDeclaration(
                    symbolName = "Gamma",
                    parameters = emptyList(),
                    textRange = XbTextRange(0, 5),
                    text = "function Gamma()",
                ),
            ),
        )

        val items = XbCompletionService().suggest(root, "ga", caseSensitive = false)
        assertThat(items).containsExactly(XbCompletionItem("Gamma", XbCompletionType.FUNCTION))
    }

    @Test
    fun `includes keyword suggestions`() {
        val root = XbPsiFile(
            name = "sample",
            textRange = XbTextRange(0, 5),
            text = "file",
            children = emptyList(),
        )

        val items = XbCompletionService().suggest(root, "lo", caseSensitive = false)

        assertThat(items).containsExactly(XbCompletionItem("LOCAL", XbCompletionType.KEYWORD))
    }
}
