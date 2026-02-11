package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbRenameServiceTest {
    private val renameService = XbRenameService()

    @Test
    fun `renames function declarations and references`() {
        val source = """
            function Foo()
                Foo()
            return
        """.trimIndent()
        val offset = source.indexOf("Foo") + 1

        val result = renameService.rename(source, offset, "Bar")

        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).contains("function Bar()")
        assertThat(result.updatedText).contains("Bar()")
        assertThat(result.updatedText).doesNotContain("Foo()")
    }

    @Test
    fun `returns an error when no symbol exists at caret`() {
        val source = """
            function Foo()
                Foo()
            return
        """.trimIndent()
        val offset = source.indexOf("return") + 1

        val result = renameService.rename(source, offset, "Bar")

        assertThat(result.errors).contains("No symbol found at the caret.")
        assertThat(result.updatedText).isEqualTo(source)
    }

    @Test
    fun `returns validation errors for blank names`() {
        val source = """
            function Foo()
                Foo()
            return
        """.trimIndent()
        val offset = source.indexOf("Foo") + 1

        val result = renameService.rename(source, offset, "   ")

        assertThat(result.errors).contains("New name must not be blank.")
        assertThat(result.updatedText).isEqualTo(source)
        assertThat(result.oldName).isEqualTo("Foo")
    }

    @Test
    fun `renames variables only within the resolved scope`() {
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
        val offset = source.indexOf("count + 1") + 1

        val result = renameService.rename(source, offset, "total")

        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).contains("local total")
        assertThat(result.updatedText).contains("total := total + 1")
        assertThat(result.updatedText).contains("local count")
        assertThat(result.updatedText).contains("count := count + 2")
    }

    @Test
    fun `renames global variables when no local declaration exists`() {
        val source = """
            public count

            function Main()
                count := count + 1
            endfunction
        """.trimIndent()
        val offset = source.indexOf("count := count") + 1

        val result = renameService.rename(source, offset, "total")

        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).contains("public total")
        assertThat(result.updatedText).contains("total := total + 1")
        assertThat(result.updatedText).doesNotContain("public count")
    }

    @Test
    fun `renames forward references to the nearest declaration in scope`() {
        val source = """
            function Main()
                count := 1
                local count
                count := count + 2
            endfunction
        """.trimIndent()
        val offset = source.indexOf("count := 1") + 1

        val result = renameService.rename(source, offset, "total")

        assertThat(result.errors).isEmpty()
        assertThat(result.updatedText).contains("total := 1")
        assertThat(result.updatedText).contains("local total")
        assertThat(result.updatedText).contains("total := total + 2")
    }
}
