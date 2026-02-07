package com.prestoxbasopp.testframework.golden

import com.prestoxbasopp.testframework.operations.OperationDefinition
import java.nio.file.Files
import java.nio.file.Path

private const val MIN_SUFFIX = "_min.xb"
private const val EDGE_SUFFIX = "_edge.xb"
private const val AST_SUFFIX = ".ast.txt"


data class GoldenFixture(
    val id: String,
    val minSource: String,
    val edgeSource: String,
    val expectedAst: String,
    val expectedErrors: List<String>,
) {
    fun <TAst> toTestCases(): List<GoldenTestCase<TAst>> {
        return listOf(
            GoldenTestCase(
                id = "$id:min",
                source = minSource,
                expectedAst = expectedAst,
                expectedErrors = expectedErrors,
            ),
            GoldenTestCase(
                id = "$id:edge",
                source = edgeSource,
                expectedAst = expectedAst,
                expectedErrors = expectedErrors,
            ),
        )
    }
}

object GoldenFixtureLoader {
    fun loadForOperation(operation: OperationDefinition, fixturesRoot: Path): GoldenFixture {
        val id = operation.id?.takeIf { it.isNotBlank() }
            ?: error("operation id is required to load golden fixtures")
        val expectedErrors = operation.expectedErrors ?: emptyList()
        return load(fixturesRoot, id, expectedErrors)
    }

    fun load(fixturesRoot: Path, id: String, expectedErrors: List<String> = emptyList()): GoldenFixture {
        val minSource = readRequiredFile(fixturesRoot.resolve("$id$MIN_SUFFIX"))
        val edgeSource = readRequiredFile(fixturesRoot.resolve("$id$EDGE_SUFFIX"))
        val expectedAst = readRequiredFile(fixturesRoot.resolve("$id$AST_SUFFIX")).trimEnd()
        return GoldenFixture(
            id = id,
            minSource = minSource,
            edgeSource = edgeSource,
            expectedAst = expectedAst,
            expectedErrors = expectedErrors,
        )
    }

    private fun readRequiredFile(path: Path): String {
        if (!Files.exists(path)) {
            error("Missing required fixture file: ${path.toAbsolutePath()}")
        }
        return Files.readString(path)
    }
}
