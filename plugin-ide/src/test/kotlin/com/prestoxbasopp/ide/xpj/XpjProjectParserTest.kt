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

    @Test
    fun `parses indented xpj sections including windows paths and auto-depend blocks`() {
        val input = """
               [PROJECT]
                  VERSION               = 2.2
                  DEBUG                 = yes
                  TARGET_DIR            = .\run
                  project.xpj
               [project.xpj]
                  customer.exe
               [customer.exe]
                  COMPILE               = xpp       // Missing compiler and linker
                  COMPILE_FLAGS         = /q        // information is added
                  GUI                   = no
               // ${'$'}START-AUTODEPEND
                  collat.ch
                  customer.obj
               // ${'$'}STOP-AUTODEPEND
                  customer.prg
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.sections).hasSize(3)
        assertThat(result.section("PROJECT")?.entries).containsExactly(
            XpjEntry.Definition("VERSION", "2.2"),
            XpjEntry.Definition("DEBUG", "yes"),
            XpjEntry.Definition("TARGET_DIR", ".\\run"),
            XpjEntry.Reference("project.xpj"),
        )
        assertThat(result.section("customer.exe")?.entries).containsExactly(
            XpjEntry.Definition("COMPILE", "xpp"),
            XpjEntry.Definition("COMPILE_FLAGS", "/q"),
            XpjEntry.Definition("GUI", "no"),
            XpjEntry.Reference("collat.ch"),
            XpjEntry.Reference("customer.obj"),
            XpjEntry.Reference("customer.prg"),
        )
    }

    @Test
    fun `does not treat URL-style values as comments`() {
        val input = """
            [PROJECT]
                PRE_BUILD = "https://example.com/tool"
                project.xpj
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.section("PROJECT")?.entries).containsExactly(
            XpjEntry.Definition("PRE_BUILD", "\"https://example.com/tool\""),
            XpjEntry.Reference("project.xpj"),
        )
    }
}
