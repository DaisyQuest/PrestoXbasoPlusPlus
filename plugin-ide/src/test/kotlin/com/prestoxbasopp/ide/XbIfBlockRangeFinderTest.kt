package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbIfBlockRangeFinderTest {
    private val finder = XbIfBlockRangeFinder()

    @Test
    fun `returns if block range when hovering keyword`() {
        val source = """
            if foo
               if bar
               endif
            endif
        """.trimIndent()
        val index = finder.buildIndex(source)
        val offset = source.indexOf("if foo")

        val range = finder.findRange(index, offset)

        assertThat(range).isNotNull
        assertThat(range!!.startOffset).isEqualTo(0)
        assertThat(range.endOffset).isGreaterThan(source.indexOf("endif"))
    }

    @Test
    fun `returns null when offset is not on if keyword`() {
        val source = "if foo\nendif"
        val index = finder.buildIndex(source)
        val offset = source.indexOf("foo")

        val range = finder.findRange(index, offset)

        assertThat(range).isNull()
    }
}
