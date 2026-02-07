package com.prestoxbasopp.testframework.golden

import com.prestoxbasopp.testframework.operations.OperationDefinition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class GoldenFixtureLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads fixtures for operation`() {
        tempDir.resolve("op_min.xb").writeText("min")
        tempDir.resolve("op_edge.xb").writeText("edge")
        tempDir.resolve("op.ast.txt").writeText("AST\n")
        val operation = OperationDefinition(
            id = "op",
            category = "expr",
            exampleMin = "min",
            exampleEdge = "edge",
            expectedAstShape = "AST",
            expectedErrors = listOf("E1"),
            precedenceGroup = null,
        )

        val fixture = GoldenFixtureLoader.loadForOperation(operation, tempDir)

        assertThat(fixture.id).isEqualTo("op")
        assertThat(fixture.minSource).isEqualTo("min")
        assertThat(fixture.edgeSource).isEqualTo("edge")
        assertThat(fixture.expectedAst).isEqualTo("AST")
        assertThat(fixture.expectedErrors).containsExactly("E1")
        val cases = fixture.toTestCases<String>()
        assertThat(cases.map { it.id }).containsExactly("op:min", "op:edge")
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

        assertThatThrownBy { GoldenFixtureLoader.loadForOperation(operation, tempDir) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("operation id is required")
    }

    @Test
    fun `throws when fixture is missing`() {
        tempDir.resolve("op_min.xb").writeText("min")
        tempDir.resolve("op_edge.xb").writeText("edge")

        assertThatThrownBy { GoldenFixtureLoader.load(tempDir, "op") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Missing required fixture file")
            .hasMessageContaining("op.ast.txt")
    }
}
