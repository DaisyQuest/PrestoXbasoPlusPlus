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
        val offset = source.indexOf("function") + 1

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
}
