package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationDefinition
import com.prestoxbasopp.testframework.operations.OperationsRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class CompletionCoverageGateTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `validate reports missing and invalid fixtures`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                operation("op-missing"),
                operation("op-invalid"),
                operation("op-valid"),
            ),
        )
        writeValidFixture(tempDir.resolve("op-valid"))
        writeInvalidFixture(tempDir.resolve("op-invalid"))

        val result = CompletionCoverageGate.validate(registry, tempDir)

        assertThat(result.missingFixtures.keys).containsExactly("op-missing")
        assertThat(result.invalidFixtures.keys).containsExactly("op-invalid")
        assertThat(result.isComplete).isFalse()
    }

    @Test
    fun `requireCoverage throws when fixtures are incomplete`() {
        val registry = OperationsRegistry(version = 1, operations = listOf(operation("op-missing")))

        assertThatThrownBy { CompletionCoverageGate.requireCoverage(registry, tempDir) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Missing completion fixtures")
    }

    private fun operation(id: String): OperationDefinition {
        return OperationDefinition(
            id = id,
            category = "expr",
            exampleMin = "min",
            exampleEdge = "edge",
            expectedAstShape = "AST",
            expectedErrors = emptyList(),
            precedenceGroup = null,
        )
    }

    private fun writeValidFixture(root: Path) {
        writeCase(root.resolve("min"), "/*caret*/", "Alpha", "LOCAL")
        writeCase(root.resolve("edge"), "x /*caret*/", "Beta", "GLOBAL")
    }

    private fun writeInvalidFixture(root: Path) {
        val minDir = root.resolve("min").createDirectories()
        minDir.resolve("input.xb").writeText("no caret")
        minDir.resolve("expected.completions.json").writeText(validCompletionJson("Alpha"))
        minDir.resolve("expected.ranking.json").writeText(validRankingJson("Alpha", "LOCAL"))
        writeCase(root.resolve("edge"), "/*caret*/", "Beta", "GLOBAL")
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
