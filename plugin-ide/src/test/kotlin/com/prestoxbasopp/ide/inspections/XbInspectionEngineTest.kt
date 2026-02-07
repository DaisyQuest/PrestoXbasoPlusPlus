package com.prestoxbasopp.ide.inspections

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionEngineTest {
    private val engine = XbInspectionEngine(XbStandardInspections.all)

    @Test
    fun `reports lexer and parser errors`() {
        val findings = engine.inspect("if @ then endif")

        assertThat(findings.map { it.id }).contains("XB100", "XB101")
        assertThat(findings.filter { it.id == "XB100" }.all { it.severity == XbInspectionSeverity.ERROR }).isTrue()
        assertThat(findings.filter { it.id == "XB101" }.all { it.severity == XbInspectionSeverity.ERROR }).isTrue()
    }

    @Test
    fun `flags empty statements and redundant parentheses`() {
        val findings = engine.inspect("if foo then ; ; endif (foo)")

        assertThat(findings.count { it.id == "XB200" }).isEqualTo(2)
        assertThat(findings.any { it.id == "XB210" }).isTrue()
    }

    @Test
    fun `does not flag parentheses used for calls`() {
        val findings = engine.inspect("return LTrim(Str(position))")

        assertThat(findings.any { it.id == "XB210" }).isFalse()
    }

    @Test
    fun `flags constant conditions and unreachable statements`() {
        val findings = engine.inspect("if 1 then return 1; foo endif")

        assertThat(findings.any { it.id == "XB220" }).isTrue()
        assertThat(findings.any { it.id == "XB240" }).isTrue()
    }

    @Test
    fun `does not mark else blocks unreachable when then returns`() {
        val findings = engine.inspect("if 1 then return 1; else foo; endif")

        assertThat(findings.any { it.id == "XB240" }).isFalse()
    }

    @Test
    fun `does not treat later function declarations as unreachable`() {
        val source = """
            function First()
               return 1
            endfunction

            function Second()
               return 2
            endfunction
        """.trimIndent()

        val findings = engine.inspect(source)

        assertThat(findings.any { it.id == "XB240" }).isFalse()
    }

    @Test
    fun `flags self comparisons`() {
        val findings = engine.inspect("if foo == foo then endif")

        assertThat(findings.any { it.id == "XB230" }).isTrue()
    }

    @Test
    fun `applies inspection profile overrides`() {
        val profile = XbInspectionProfile(
            enabledInspections = setOf("XB220"),
            severityOverrides = mapOf("XB220" to XbInspectionSeverity.INFO),
        )

        val findings = engine.inspect("if 1 then return 1 endif", profile)

        assertThat(findings).hasSize(1)
        assertThat(findings.first().severity).isEqualTo(XbInspectionSeverity.INFO)
    }

    @Test
    fun `suggests keywords for likely misspellings`() {
        val findings = engine.inspect("loal count\nfuncion Demo()\nprcedure Sample()")

        val suggestions = findings.filter { it.id == "XB250" }

        assertThat(suggestions).hasSize(3)
        assertThat(suggestions.map { it.message }).containsExactlyInAnyOrder(
            "Did you mean \"LOCAL\"?",
            "Did you mean \"FUNCTION\"?",
            "Did you mean \"PROCEDURE\"?",
        )
    }

    @Test
    fun `does not suggest keywords for short or distant identifiers`() {
        val findings = engine.inspect("lo count\nlocation value")

        assertThat(findings.none { it.id == "XB250" }).isTrue()
    }
}
