package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationDefinition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class CompletionFixtureLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads completion fixtures for operation`() {
        val operationDir = tempDir.resolve("op").createDirectories()
        writeCase(operationDir.resolve("min"), "/*caret*/", "Alpha", "LOCAL")
        writeCase(operationDir.resolve("edge"), "x /*caret*/", "Beta", "GLOBAL")
        val operation = OperationDefinition(
            id = "op",
            category = "expr",
            exampleMin = "min",
            exampleEdge = "edge",
            expectedAstShape = "AST",
            expectedErrors = emptyList(),
            precedenceGroup = null,
        )

        val fixture = CompletionFixtureLoader.loadForOperation(operation, tempDir)

        assertThat(fixture.id).isEqualTo("op")
        assertThat(fixture.min.completions.entries.first().label).isEqualTo("Alpha")
        assertThat(fixture.edge.ranking.ranked.first().scope).isEqualTo("GLOBAL")
        assertThat(fixture.cases().map { it.first }).containsExactly("op:min", "op:edge")
    }

    @Test
    fun `rejects missing operation id`() {
        val operation = OperationDefinition(
            id = null,
            category = "expr",
            exampleMin = "min",
            exampleEdge = "edge",
            expectedAstShape = "AST",
            expectedErrors = emptyList(),
            precedenceGroup = null,
        )

        assertThatThrownBy { CompletionFixtureLoader.loadForOperation(operation, tempDir) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("operation id is required")
    }

    @Test
    fun `fails when caret marker is missing`() {
        val operationDir = tempDir.resolve("op").createDirectories()
        writeCase(operationDir.resolve("min"), "no caret", "Alpha", "LOCAL")
        writeCase(operationDir.resolve("edge"), "/*caret*/", "Beta", "GLOBAL")

        assertThatThrownBy { CompletionFixtureLoader.load(tempDir, "op") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Missing caret marker")
    }

    @Test
    fun `fails when required file is missing`() {
        val operationDir = tempDir.resolve("op").createDirectories()
        operationDir.resolve("min").createDirectories().resolve("input.xb").writeText("/*caret*/")

        assertThatThrownBy { CompletionFixtureLoader.load(tempDir, "op") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Missing required completion fixture file")
    }

    @Test
    fun `fails when completions entry is missing required fields`() {
        val operationDir = tempDir.resolve("op").createDirectories()
        operationDir.resolve("min").createDirectories().resolve("input.xb").writeText("/*caret*/")
        operationDir.resolve("min").resolve("expected.completions.json").writeText("{entries: [{label: A}]}")
        operationDir.resolve("min").resolve("expected.ranking.json").writeText(validRankingJson("A", "LOCAL"))
        writeCase(operationDir.resolve("edge"), "/*caret*/", "Beta", "GLOBAL")

        assertThatThrownBy { CompletionFixtureLoader.load(tempDir, "op") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("entries[0].kind")
    }

    @Test
    fun `fails when ranking score is invalid`() {
        val operationDir = tempDir.resolve("op").createDirectories()
        operationDir.resolve("min").createDirectories().resolve("input.xb").writeText("/*caret*/")
        operationDir.resolve("min").resolve("expected.completions.json").writeText(validCompletionJson("Alpha"))
        operationDir.resolve("min").resolve("expected.ranking.json").writeText("{ranked: [{label: A, score: nope, scope: LOCAL, typeCompat: EXACT, tieBreak: ALPHA}]}")
        writeCase(operationDir.resolve("edge"), "/*caret*/", "Beta", "GLOBAL")

        assertThatThrownBy { CompletionFixtureLoader.load(tempDir, "op") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("ranked[0].score")
    }

    private fun writeCase(caseDir: Path, source: String, label: String, scope: String) {
        caseDir.createDirectories()
        caseDir.resolve("input.xb").writeText(source)
        caseDir.resolve("expected.completions.json").writeText(validCompletionJson(label))
        caseDir.resolve("expected.ranking.json").writeText(validRankingJson(label, scope))
    }

    private fun validCompletionJson(label: String): String {
        return """
            {
              entries: [
                {label: $label, kind: FUNCTION, source: LOCAL, insertText: $label}
              ]
            }
        """.trimIndent()
    }

    private fun validRankingJson(label: String, scope: String): String {
        return """
            {
              ranked: [
                {label: $label, score: 0.9, scope: $scope, typeCompat: EXACT, tieBreak: ALPHA}
              ]
            }
        """.trimIndent()
    }
}
