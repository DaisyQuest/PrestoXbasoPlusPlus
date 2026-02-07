package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationDefinition
import com.prestoxbasopp.testframework.operations.OperationsRegistry
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class CompletionTestHarnessTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `assertOperation compares completion snapshots`() {
        writeFixture(tempDir.resolve("op"))
        val operation = operation("op")
        val provider = CompletionProvider { source ->
            CompletionCase(
                source = source,
                completions = CompletionSnapshot(
                    entries = listOf(
                        CompletionEntry(
                            label = "Alpha",
                            kind = "FUNCTION",
                            source = "LOCAL",
                            insertText = "Alpha",
                            detail = null,
                            type = null,
                        ),
                    ),
                ),
                ranking = RankingSnapshot(
                    ranked = listOf(
                        RankingEntry(
                            label = "Alpha",
                            score = 0.9,
                            scope = "LOCAL",
                            typeCompat = "EXACT",
                            tieBreak = "ALPHA",
                        ),
                    ),
                ),
            )
        }

        CompletionTestHarness.assertOperation(operation, tempDir, provider)
    }

    @Test
    fun `assertOperations skips operations without ids`() {
        writeFixture(tempDir.resolve("op"))
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                operation("op"),
                operation(null),
            ),
        )
        val fixture = CompletionFixtureLoader.load(tempDir, "op")
        val provider = CompletionProvider { source ->
            when (source) {
                fixture.min.source -> fixture.min
                fixture.edge.source -> fixture.edge
                else -> emptyCase(source)
            }
        }

        CompletionTestHarness.assertOperations(registry, tempDir, provider)
    }

    @Test
    fun `assertFixture surfaces mismatches`() {
        writeFixture(tempDir.resolve("op"))
        val fixture = CompletionFixtureLoader.load(tempDir, "op")
        val provider = CompletionProvider { source ->
            CompletionCase(
                source = source,
                completions = CompletionSnapshot(entries = listOf(completionEntry("Wrong"))),
                ranking = RankingSnapshot(
                    ranked = listOf(
                        RankingEntry(
                            label = "Wrong",
                            score = 0.9,
                            scope = "LOCAL",
                            typeCompat = "EXACT",
                            tieBreak = "ALPHA",
                        ),
                    ),
                ),
            )
        }

        assertThatThrownBy { CompletionTestHarness.assertFixture(fixture, provider) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("label")
    }

    private fun operation(id: String?): OperationDefinition {
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

    private fun writeFixture(root: Path) {
        writeCase(root.resolve("min"), "/*caret*/", "Alpha", "LOCAL")
        writeCase(root.resolve("edge"), "x /*caret*/", "Alpha", "LOCAL")
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

    private fun emptyCase(source: String): CompletionCase {
        return CompletionCase(
            source = source,
            completions = CompletionSnapshot(entries = emptyList()),
            ranking = RankingSnapshot(ranked = emptyList()),
        )
    }

    private fun completionEntry(label: String): CompletionEntry {
        return CompletionEntry(
            label = label,
            kind = "FUNCTION",
            source = "LOCAL",
            insertText = label,
            detail = null,
            type = null,
        )
    }
}
