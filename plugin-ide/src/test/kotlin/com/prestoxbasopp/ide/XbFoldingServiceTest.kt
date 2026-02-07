package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbFoldingServiceTest {
    @Test
    fun `collects folding ranges for foldable elements`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "root",
            textRange = XbTextRange(0, 50),
            text = "file",
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.FUNCTION_DECLARATION,
                    name = "main",
                    textRange = XbTextRange(0, 20),
                    text = "function main()",
                    children = listOf(
                        XbPsiSnapshot(
                            elementType = XbPsiElementType.BLOCK,
                            name = null,
                            textRange = XbTextRange(5, 18),
                            text = "{ }",
                        ),
                    ),
                ),
                XbPsiSnapshot(
                    elementType = XbPsiElementType.LITERAL,
                    name = null,
                    textRange = XbTextRange(21, 21),
                    text = "0",
                ),
            ),
        )

        val ranges = XbFoldingService().foldingRanges(snapshot)

        assertThat(ranges).hasSize(2)
        assertThat(ranges.map { it.placeholder }).contains("main(...)", "{...}")
    }

    @Test
    fun `skips zero length ranges`() {
        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.BLOCK,
            name = null,
            textRange = XbTextRange(0, 1),
            text = "{}",
        )

        val ranges = XbFoldingService().foldingRanges(snapshot)
        assertThat(ranges).isEmpty()
    }
}
