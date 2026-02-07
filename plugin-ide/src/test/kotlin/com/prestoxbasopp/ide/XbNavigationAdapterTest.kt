package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbNavigationAdapterTest {
    private val adapter = XbNavigationAdapter()

    @Test
    fun `reference navigates to function declaration`() {
        val source = """
            function Add(a, b)
               return a + b
            endfunction

            function Main()
               local result := Add(1, 2)
               return result
            endfunction
        """.trimIndent()

        val referenceOffset = source.indexOf("Add(1")
        val targets = adapter.findTargets(source, referenceOffset)

        val expectedStart = source.indexOf("function Add")
        val expectedEnd = source.indexOf(")", expectedStart) + 1
        assertThat(targets).containsExactly(XbTextRange(expectedStart, expectedEnd))
    }

    @Test
    fun `definition navigates to usages`() {
        val source = """
            function Total(count)
               return count
            endfunction

            function Main()
               local value := Total(3)
               Total(4)
               return value
            endfunction
        """.trimIndent()

        val definitionOffset = source.indexOf("Total(count)") + "Total".length - 1
        val targets = adapter.findTargets(source, definitionOffset)

        val firstUsage = source.indexOf("Total(3)")
        val secondUsage = source.indexOf("Total(4)")
        assertThat(targets).containsExactly(
            XbTextRange(firstUsage, firstUsage + "Total".length),
            XbTextRange(secondUsage, secondUsage + "Total".length),
        )
    }

    @Test
    fun `reference navigates to variable declaration`() {
        val source = """
            function Main()
               local count
               count := count + 1
               return count
            endfunction
        """.trimIndent()

        val referenceOffset = source.indexOf("count +")
        val targets = adapter.findTargets(source, referenceOffset)

        val declarationStart = source.indexOf("count\n")
        assertThat(targets).containsExactly(XbTextRange(declarationStart, declarationStart + "count".length))
    }
}
