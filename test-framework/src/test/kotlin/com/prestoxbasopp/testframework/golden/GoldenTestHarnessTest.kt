package com.prestoxbasopp.testframework.golden

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GoldenTestHarnessTest {
    @Test
    fun `asserts matching ast and errors`() {
        val parser = GoldenParser<String> { source ->
            ParseResult(ast = source.uppercase(), errors = listOf("E1"))
        }
        val dumper = GoldenAstDumper<String> { ast -> ast }
        val testCase = GoldenTestCase<String>(
            id = "case-1",
            source = "ast",
            expectedAst = "AST",
            expectedErrors = listOf("E1"),
        )

        GoldenTestHarness.assertCase(testCase, parser, dumper)
    }

    @Test
    fun `uses sentinel when ast is missing`() {
        val parser = GoldenParser<String> { ParseResult(ast = null, errors = emptyList()) }
        val dumper = GoldenAstDumper<String> { ast -> ast }
        val testCase = GoldenTestCase<String>(
            id = "case-2",
            source = "input",
            expectedAst = GoldenTestHarness.DEFAULT_MISSING_AST_SENTINEL,
            expectedErrors = emptyList(),
        )

        GoldenTestHarness.assertCase(testCase, parser, dumper)
    }

    @Test
    fun `fails when ast mismatches`() {
        val parser = GoldenParser<String> { ParseResult(ast = "AST", errors = emptyList()) }
        val dumper = GoldenAstDumper<String> { ast -> ast }
        val testCase = GoldenTestCase<String>(
            id = "case-3",
            source = "input",
            expectedAst = "OTHER",
            expectedErrors = emptyList(),
        )

        assertThatThrownBy { GoldenTestHarness.assertCase(testCase, parser, dumper) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("AST mismatch")
    }

    @Test
    fun `asserts multiple cases`() {
        val parser = GoldenParser<String> { source ->
            ParseResult(ast = source.uppercase(), errors = emptyList())
        }
        val dumper = GoldenAstDumper<String> { ast -> ast }
        val cases = listOf(
            GoldenTestCase<String>(
                id = "case-4",
                source = "a",
                expectedAst = "A",
            ),
            GoldenTestCase<String>(
                id = "case-5",
                source = "b",
                expectedAst = "B",
            ),
        )

        GoldenTestHarness.assertCases(cases, parser, dumper)
    }
}
