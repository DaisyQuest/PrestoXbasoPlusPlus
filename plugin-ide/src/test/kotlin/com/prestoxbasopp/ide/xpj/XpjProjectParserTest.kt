package com.prestoxbasopp.ide.xpj

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XpjProjectParserTest {
    private val parser = XpjProjectParser()

    @Test
    fun `parses project sections definitions and references`() {
        val input = """
            [PROJECT]
                DEBUG = YES
                project.xpj

            [project.xpj]
                app.exe

            [app.exe]
                GUI = YES
                main.prg
                main.arc
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.sections).hasSize(3)
        assertThat(result.section("PROJECT")?.entries).containsExactly(
            XpjEntry.Definition("DEBUG", "YES"),
            XpjEntry.Reference("project.xpj"),
        )
        assertThat(result.section("app.exe")?.entries).containsExactly(
            XpjEntry.Definition("GUI", "YES"),
            XpjEntry.Reference("main.prg"),
            XpjEntry.Reference("main.arc"),
        )
    }

    @Test
    fun `ignores blank lines comments and content outside sections`() {
        val input = """
            DEBUG = YES
            // header comment

            [PROJECT] // trailing comment
                DEBUG = NO // inline comment
                project.xpj
        """.trimIndent()

        val result = parser.parse(input)

        val section = result.sections.single()
        assertThat(section.name).isEqualTo("PROJECT")
        assertThat(section.entries).containsExactly(
            XpjEntry.Definition("DEBUG", "NO"),
            XpjEntry.Reference("project.xpj"),
        )
    }
}
