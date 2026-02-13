package com.prestoxbasopp.ide.inspections

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionEngineTest {
    private val engine = XbInspectionEngine(XbStandardInspections.all)

    @Test
    fun `reports lexer and parser errors`() {
        val findings = engine.inspect("if @ then endif")

        assertThat(findings.map { it.id }).contains("XB101")
        assertThat(findings.filter { it.id == "XB101" }.all { it.severity == XbInspectionSeverity.ERROR }).isTrue()
    }

    @Test
    fun `flags empty statements and redundant parentheses`() {
        val findings = engine.inspect("if foo then ; ; endif (foo)")

        assertThat(findings.count { it.id == "XB200" }).isEqualTo(2)
        assertThat(findings.any { it.id == "XB210" }).isTrue()
    }

    @Test
    fun `highlights line continuations`() {
        val source = """
            if foo;
               bar
            endif
        """.trimIndent()

        val findings = engine.inspect(source)

        assertThat(findings.any { it.id == "XB205" }).isTrue()
        assertThat(findings.none { it.id == "XB200" }).isTrue()
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
    fun `does not mark method body unreachable because previous method returned`() {
        val source = """
            class DbaseF5
                method init(data)
                method normalizeForPersistence()
            endclass

            method DbaseF5:init(data)
                return Self

            method DbaseF5:normalizeForPersistence()
                local payload := {=>}
                payload["NF"] := 1
                return payload
        """.trimIndent()

        val findings = engine.inspect(source)

        assertThat(findings.none { it.id == "XB240" }).isTrue()
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

    @Test
    fun `detects god class tiers with expected severities`() {
        val tier1Source = makeLineSource(3001)
        val tier2Source = makeLineSource(5001)
        val tier3Source = makeLineSource(10001)

        val tier1Findings = engine.inspect(tier1Source)
        val tier2Findings = engine.inspect(tier2Source)
        val tier3Findings = engine.inspect(tier3Source)

        assertThat(tier1Findings.single { it.id == "XB260" }.severity).isEqualTo(XbInspectionSeverity.WARNING)
        assertThat(tier1Findings.none { it.id == "XB261" || it.id == "XB262" }).isTrue()

        assertThat(tier2Findings.single { it.id == "XB261" }.severity).isEqualTo(XbInspectionSeverity.ERROR)
        assertThat(tier2Findings.none { it.id == "XB260" || it.id == "XB262" }).isTrue()

        assertThat(tier3Findings.single { it.id == "XB262" }.severity).isEqualTo(XbInspectionSeverity.ERROR)
        assertThat(tier3Findings.none { it.id == "XB260" || it.id == "XB261" }).isTrue()
    }

    @Test
    fun `flags index zero and for loop from zero to len`() {
        val source = """
            local a := {1,2,3}
            ? a[0]
            for i := 0 to Len(a)
                ? a[i]
            next
        """.trimIndent()

        val findings = engine.inspect(source)

        assertThat(findings.count { it.id == "XB270" }).isEqualTo(2)
    }

    @Test
    fun `does not flag array access rule for non zero indexes`() {
        val source = """
            local a := {1,2,3}
            ? a[1]
            for i := 1 to Len(a)
                ? a[i]
            next
        """.trimIndent()

        val findings = engine.inspect(source)

        assertThat(findings.none { it.id == "XB270" }).isTrue()
    }

    @Test
    fun `warns when function has no explicit return and does not warn when return exists`() {
        val withoutReturn = "function Foo()\nlocal x := 1\nendfunction"
        val withReturn = "function Bar()\nreturn 1\nendfunction"

        val withoutReturnFindings = engine.inspect(withoutReturn)
        val withReturnFindings = engine.inspect(withReturn)

        assertThat(withoutReturnFindings.any { it.id == "XB271" }).isTrue()
        assertThat(withReturnFindings.none { it.id == "XB271" }).isTrue()
    }

    @Test
    fun `warns when procedure returns a value only`() {
        val withValueReturn = "procedure Test()\nreturn 5\nendprocedure"
        val bareReturn = "procedure Test2()\nreturn\nendprocedure"

        val withValueReturnFindings = engine.inspect(withValueReturn)
        val bareReturnFindings = engine.inspect(bareReturn)

        assertThat(withValueReturnFindings.any { it.id == "XB272" }).isTrue()
        assertThat(bareReturnFindings.none { it.id == "XB272" }).isTrue()
    }

    @Test
    fun `flags while true loop without exit and allows loop with exit`() {
        val noExit = "while .T.\n? 1\nenddo"
        val withExit = "while .T.\nexit\nenddo"

        val noExitFindings = engine.inspect(noExit)
        val withExitFindings = engine.inspect(withExit)

        assertThat(noExitFindings.any { it.id == "XB273" }).isTrue()
        assertThat(withExitFindings.none { it.id == "XB273" }).isTrue()
    }

    @Test
    fun `flags public and private declarations`() {
        val findings = engine.inspect("public x\nprivate y\nlocal z")

        assertThat(findings.count { it.id == "XB274" }).isEqualTo(2)
    }

    @Test
    fun `uses lex-only context for oversized sources`() {
        val policy = XbInspectionPerformancePolicy(maxSourceLengthForFullInspection = 64)
        val engine = XbInspectionEngine(XbStandardInspections.all, policy)
        val source = buildString {
            repeat(80) {
                append("if 1 then return 1 endif\n")
            }
        }

        val findings = engine.inspect(source)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `uses full context below oversized threshold`() {
        val policy = XbInspectionPerformancePolicy(maxSourceLengthForFullInspection = 256)
        val engine = XbInspectionEngine(XbStandardInspections.all, policy)

        val findings = engine.inspect("if 1 then return 1 endif")

        assertThat(findings.any { it.id == "XB220" }).isTrue()
    }

    private fun makeLineSource(lines: Int): String = buildString {
        repeat(lines) { index ->
            append("? ")
            append(index + 1)
            if (index < lines - 1) {
                append('\n')
            }
        }
    }
}
