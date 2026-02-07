package com.prestoxbasopp.testframework.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RepositoryOperationsCoverageTest {
    @Test
    fun `operations registry in spec directory is schema compliant`() {
        val root = repoRoot()
        val registryPath = root.resolve("spec/xbasepp/operations.yaml")

        val registry = OperationsRegistryLoader.load(registryPath)
        val violations = OperationsSchemaValidator.validate(registry)

        assertThat(violations).isEmpty()
    }

    @Test
    fun `operations registry has complete fixture coverage`() {
        val root = repoRoot()
        val registryPath = root.resolve("spec/xbasepp/operations.yaml")
        val fixturesRoot = root.resolve("spec/xbasepp/fixtures/operations")

        val registry = OperationsRegistryLoader.load(registryPath)
        val result = OperationsCoverageGate.validate(registry, fixturesRoot)

        assertThat(result.isComplete).isTrue()
    }

    private fun repoRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (!Files.exists(current.resolve("spec/xbasepp/operations.yaml")) && current.parent != null) {
            current = current.parent
        }
        return current
    }
}
