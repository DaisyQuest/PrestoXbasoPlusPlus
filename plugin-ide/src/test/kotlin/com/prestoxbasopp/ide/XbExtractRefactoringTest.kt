package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbExtractRefactoringTest {
    @Test
    fun `extracts variable with local declaration and replacement`() {
        val source = """
            function Main()
               return total + 1
            endfunction
        """.trimIndent()
        val start = source.indexOf("total")
        val end = start + "total + 1".length

        val result = XbExtractVariableService().extract(source, start, end, "result")

        val expected = """
            function Main()
               local result := total + 1
               return result
            endfunction
        """.trimIndent()
        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).isEqualTo(expected)
    }

    @Test
    fun `extract variable reports invalid selections`() {
        val source = "function Main()\n   return total + 1\nendfunction"

        val blankName = XbExtractVariableService().extract(source, 0, 0, "")
        assertThat(blankName.errors).contains("Variable name cannot be blank.")

        val start = source.indexOf("return")
        val end = source.indexOf("endfunction")
        val multiline = XbExtractVariableService().extract(source, start, end, "value")
        assertThat(multiline.errors).contains("Extract variable expects a single expression.")
    }

    @Test
    fun `extracts function by replacing selection with a call and appending definition`() {
        val source = """
            function Main()
               local total := 1
               total := total + 1
               return total
            endfunction
        """.trimIndent()
        val selectionStart = source.indexOf("   local total")
        val selectionEnd = source.indexOf("   return total")

        val result = XbExtractFunctionService().extract(source, selectionStart, selectionEnd, "InitTotal")

        val expected = """
            function Main()
               InitTotal()
               return total
            endfunction

            function InitTotal()
               local total := 1
               total := total + 1
            endfunction
        """.trimIndent()
        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).isEqualTo(expected)
    }

    @Test
    fun `extract function validates inputs`() {
        val source = "function Main()\nendfunction"
        val result = XbExtractFunctionService().extract(source, 0, 0, " ")

        assertThat(result.errors).contains(
            "Function name cannot be blank.",
            "Select statements to extract.",
        )
    }
}
