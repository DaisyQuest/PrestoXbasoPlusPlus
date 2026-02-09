package com.prestoxbasopp.testframework.operations

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class OperationsCoverageGateTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `passes when fixtures are complete`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = "OP_0001",
                    category = "literals",
                    exampleMin = "1",
                    exampleEdge = "-1",
                    expectedAstShape = "NumberLiteral",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
            ),
        )
        createFixture("OP_0001_min.xb")
        createFixture("OP_0001_edge.xb")
        createFixture("OP_0001.ast.txt")
        createFixture("OP_0001_edge.ast.txt")

        val result = OperationsCoverageGate.validate(registry, tempDir)

        assertThat(result.isComplete).isTrue()
        assertThat(result.missingFixtures).isEmpty()
    }

    @Test
    fun `reports missing fixtures`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = "OP_0002",
                    category = "literals",
                    exampleMin = "1",
                    exampleEdge = "-1",
                    expectedAstShape = "NumberLiteral",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
            ),
        )
        createFixture("OP_0002_min.xb")

        val result = OperationsCoverageGate.validate(registry, tempDir)

        assertThat(result.isComplete).isFalse()
        assertThat(result.missingFixtures["OP_0002"]).containsExactly(
            "OP_0002_edge.xb",
            "OP_0002.ast.txt",
            "OP_0002_edge.ast.txt",
        )
    }

    @Test
    fun `throws when coverage is incomplete`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = "OP_0003",
                    category = "literals",
                    exampleMin = "1",
                    exampleEdge = "-1",
                    expectedAstShape = "NumberLiteral",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
            ),
        )

        assertThatThrownBy { OperationsCoverageGate.requireCoverage(registry, tempDir) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("OP_0003")
    }

    @Test
    fun `handles operations missing identifiers`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = null,
                    category = "literals",
                    exampleMin = "1",
                    exampleEdge = "-1",
                    expectedAstShape = "NumberLiteral",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
            ),
        )

        val result = OperationsCoverageGate.validate(registry, tempDir)

        assertThat(result.isComplete).isTrue()
    }

    private fun createFixture(name: String) {
        Files.writeString(tempDir.resolve(name), "fixture")
    }
}
