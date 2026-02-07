package com.prestoxbasopp.testframework.operations

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class OperationsRegistryTest {
    @Test
    fun `loads valid registry and passes schema validation`() {
        val path = Paths.get(javaClass.getResource("/operations/sample-valid.yaml")!!.toURI())

        val registry = OperationsRegistryLoader.load(path)
        val violations = OperationsSchemaValidator.validate(registry)

        assertThat(registry.version).isEqualTo(1)
        assertThat(registry.operations).hasSize(1)
        assertThat(violations).isEmpty()
    }

    @Test
    fun `reports schema violations for invalid registry`() {
        val path = Paths.get(javaClass.getResource("/operations/sample-invalid.yaml")!!.toURI())

        val registry = OperationsRegistryLoader.load(path)
        val violations = OperationsSchemaValidator.validate(registry)

        assertThat(violations).extracting("path")
            .contains(
                "version",
                "operations[0].id",
                "operations[0].category",
                "operations[0].example_min",
                "operations[0].example_edge",
                "operations[0].expected_ast_shape",
                "operations[0].expected_errors",
            )
    }

    @Test
    fun `errors on missing version`() {
        val yaml = """
            operations: []
        """.trimIndent()

        assertThatThrownBy { OperationsRegistryLoader.load(yaml.byteInputStream()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("version")
    }

    @Test
    fun `loads empty registry when operations are omitted`() {
        val yaml = """
            version: 1
        """.trimIndent()

        val registry = OperationsRegistryLoader.load(yaml.byteInputStream())

        assertThat(registry.version).isEqualTo(1)
        assertThat(registry.operations).isEmpty()
    }

    @Test
    fun `rejects non-map operation entries`() {
        val yaml = """
            version: 1
            operations:
              - "not a map"
        """.trimIndent()

        assertThatThrownBy { OperationsRegistryLoader.load(yaml.byteInputStream()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("operation at index 0 must be a map")
    }

    @Test
    fun `rejects non-integer version`() {
        val yaml = """
            version: "one"
            operations: []
        """.trimIndent()

        assertThatThrownBy { OperationsRegistryLoader.load(yaml.byteInputStream()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("version")
    }

    @Test
    fun `filters non-string expected error entries`() {
        val yaml = """
            version: 1
            operations:
              - id: OP_0099
                category: expr
                example_min: "min"
                example_edge: "edge"
                expected_ast_shape: "Shape"
                expected_errors: ["ERR", 42, null]
        """.trimIndent()

        val registry = OperationsRegistryLoader.load(yaml.byteInputStream())

        assertThat(registry.operations).hasSize(1)
        assertThat(registry.operations.first().expectedErrors).containsExactly("ERR")
    }

    @Test
    fun `detects duplicate identifiers and missing expected errors`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = "OP_DUP",
                    category = "expr",
                    exampleMin = "min",
                    exampleEdge = "edge",
                    expectedAstShape = "Shape",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
                OperationDefinition(
                    id = "OP_DUP",
                    category = "expr",
                    exampleMin = "min",
                    exampleEdge = "edge",
                    expectedAstShape = "Shape",
                    expectedErrors = null,
                    precedenceGroup = null,
                ),
            ),
        )

        val violations = OperationsSchemaValidator.validate(registry)

        assertThat(violations).extracting("path")
            .contains(
                "operations[1].id",
                "operations[1].expected_errors",
            )
    }

    @Test
    fun `accepts empty expected errors list`() {
        val registry = OperationsRegistry(
            version = 1,
            operations = listOf(
                OperationDefinition(
                    id = "OP_EMPTY",
                    category = "expr",
                    exampleMin = "min",
                    exampleEdge = "edge",
                    expectedAstShape = "Shape",
                    expectedErrors = emptyList(),
                    precedenceGroup = null,
                ),
            ),
        )

        val violations = OperationsSchemaValidator.validate(registry)

        assertThat(violations).isEmpty()
    }
}
