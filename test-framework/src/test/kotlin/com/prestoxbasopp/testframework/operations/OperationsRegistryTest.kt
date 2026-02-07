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
}
