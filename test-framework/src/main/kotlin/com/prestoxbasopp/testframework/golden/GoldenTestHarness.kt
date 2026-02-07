package com.prestoxbasopp.testframework.golden

data class GoldenTestCase<TAst>(
    val id: String,
    val source: String,
    val expectedAst: String,
    val expectedErrors: List<String> = emptyList(),
)

fun interface GoldenAstDumper<TAst> {
    fun dump(ast: TAst): String
}

fun interface GoldenParser<TAst> {
    fun parse(source: String): ParseResult<TAst>
}

data class ParseResult<TAst>(
    val ast: TAst?,
    val errors: List<String>,
)

object GoldenTestHarness {
    const val DEFAULT_MISSING_AST_SENTINEL = "<no-ast>"

    fun <TAst> assertCase(
        testCase: GoldenTestCase<TAst>,
        parser: GoldenParser<TAst>,
        dumper: GoldenAstDumper<TAst>,
        missingAstSentinel: String = DEFAULT_MISSING_AST_SENTINEL,
    ) {
        val result = parser.parse(testCase.source)
        val actualAst = result.ast?.let(dumper::dump) ?: missingAstSentinel
        if (actualAst != testCase.expectedAst) {
            throw AssertionError(
                "AST mismatch for ${testCase.id}: expected='${testCase.expectedAst}', actual='$actualAst'",
            )
        }
        if (result.errors != testCase.expectedErrors) {
            throw AssertionError(
                "Error list mismatch for ${testCase.id}: expected='${testCase.expectedErrors}', actual='${result.errors}'",
            )
        }
    }

    fun <TAst> assertCases(
        testCases: Iterable<GoldenTestCase<TAst>>,
        parser: GoldenParser<TAst>,
        dumper: GoldenAstDumper<TAst>,
        missingAstSentinel: String = DEFAULT_MISSING_AST_SENTINEL,
    ) {
        testCases.forEach { testCase ->
            assertCase(testCase, parser, dumper, missingAstSentinel)
        }
    }
}
