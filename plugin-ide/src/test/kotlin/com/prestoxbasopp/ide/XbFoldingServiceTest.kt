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

    @Test
    fun `collects folding ranges for control and loop statements from source`() {
        val source = """
            FUNCTION main()
                IF x == 1
                    ? x
                ENDIF
                WHILE x < 10
                    x := x + 1
                ENDDO
                FOR i := 1 TO 3
                    ? i
                NEXT
            ENDFUNCTION
            PROCEDURE helper()
                ? 0
            ENDPROC
        """.trimIndent()

        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "sample",
            textRange = XbTextRange(0, source.length),
            text = source,
        )

        val ranges = XbFoldingService().foldingRanges(snapshot)

        val expectedRanges = listOf(
            XbTextRange(source.indexOf("FUNCTION"), source.indexOf("ENDFUNCTION") + "ENDFUNCTION".length),
            XbTextRange(source.indexOf("IF"), source.indexOf("ENDIF") + "ENDIF".length),
            XbTextRange(source.indexOf("WHILE"), source.indexOf("ENDDO") + "ENDDO".length),
            XbTextRange(source.indexOf("FOR"), source.indexOf("NEXT") + "NEXT".length),
            XbTextRange(source.indexOf("PROCEDURE"), source.indexOf("ENDPROC") + "ENDPROC".length),
        )

        assertThat(ranges.map { it.textRange }).containsAll(expectedRanges)
        assertThat(ranges.map { it.placeholder }).contains(
            "main(...)",
            "helper(...)",
            "if (...)",
            "while (...)",
            "for i ...",
        )
    }

    @Test
    fun `deduplicates folding ranges when snapshots overlap parsed blocks`() {
        val source = """
            FUNCTION main()
                ? 1
            ENDFUNCTION
        """.trimIndent()

        val snapshot = XbPsiSnapshot(
            elementType = XbPsiElementType.FILE,
            name = "sample",
            textRange = XbTextRange(0, source.length),
            text = source,
            children = listOf(
                XbPsiSnapshot(
                    elementType = XbPsiElementType.FUNCTION_DECLARATION,
                    name = "main",
                    textRange = XbTextRange(0, source.length),
                    text = source,
                ),
            ),
        )

        val ranges = XbFoldingService().foldingRanges(snapshot)
        val mainRanges = ranges.filter { it.placeholder == "main(...)" }
        assertThat(mainRanges).hasSize(1)
    }
    @Test
    fun `class method folds stop before the next class token`() {
        val source = """
            CLASS DbaseF5
                CLASS METHOD load(...)
                CLASS METHOD findBy(...)
            ENDCLASS
        """.trimIndent()

        val snapshot = XbPsiTextBuilder().buildSnapshot(source)
        val ranges = XbFoldingService().foldingRanges(snapshot)

        val loadRange = ranges.first { it.placeholder == "METHOD {...}" }
        val nextClassOffset = source.indexOf("CLASS METHOD findBy")
        assertThat(loadRange.textRange.endOffset).isLessThanOrEqualTo(nextClassOffset)
    }

    @Test
    fun `method declarations use method placeholder while functions keep function placeholder`() {
        val source = """
            CLASS DbaseF5
                CLASS METHOD delete(entityOrKey, options)
                    LOCAL key := entityOrKey
                    RETURN key
            ENDCLASS
            FUNCTION utilMain()
                RETURN .T.
            ENDFUNCTION
        """.trimIndent()

        val snapshot = XbPsiTextBuilder().buildSnapshot(source)
        val ranges = XbFoldingService().foldingRanges(snapshot)

        assertThat(ranges.map { it.placeholder }).contains("METHOD {...}")
        assertThat(ranges.map { it.placeholder }).contains("utilMain(...)")
        assertThat(ranges.map { it.placeholder }).doesNotContain("delete(...)")
    }
}

