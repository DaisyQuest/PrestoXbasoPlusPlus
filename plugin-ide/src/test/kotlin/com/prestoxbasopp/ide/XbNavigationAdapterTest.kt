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

        val declarationStart = source.indexOf("function Add")
        val declarationEnd = source.indexOf("endfunction", declarationStart) + "endfunction".length
        assertThat(targets).containsExactly(XbTextRange(declarationStart, declarationEnd))
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

    @Test
    fun `variable definition only navigates to usages in the same scope`() {
        val source = """
            function Main()
               local count
               count := count + 1
            endfunction

            function Other()
               local count
               count := count + 2
            endfunction
        """.trimIndent()

        val definitionOffset = source.indexOf("local count") + "local ".length
        val targets = adapter.findTargets(source, definitionOffset)

        val assignmentStart = source.indexOf("count := count + 1")
        assertThat(targets).containsExactly(
            XbTextRange(assignmentStart, assignmentStart + "count".length),
            XbTextRange(assignmentStart + "count := ".length, assignmentStart + "count := count".length),
        )
    }
}
