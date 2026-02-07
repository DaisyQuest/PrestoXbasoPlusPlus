package com.prestoxbasopp.testframework.compat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CiValidationValidatorTest {
    @Test
    fun `repository configuration passes CI validation`() {
        val repoRoot = locateRepositoryRoot()
        val report = CiValidationValidator.validate(
            CiValidationInputs(
                settingsFile = repoRoot.resolve("settings.gradle.kts"),
                rootBuildFile = repoRoot.resolve("build.gradle.kts"),
            ),
        )

        assertThat(report.errors).isEmpty()
        assertThat(report.isValid).isTrue()
    }

    @Test
    fun `missing modules and test configuration are reported`(@TempDir tempDir: Path) {
        val settingsFile = tempDir.resolve("settings.gradle.kts")
        val rootBuildFile = tempDir.resolve("build.gradle.kts")
        settingsFile.toFile().writeText("rootProject.name = \"temp\"")
        rootBuildFile.toFile().writeText("plugins { }")

        val report = CiValidationValidator.validate(
            CiValidationInputs(
                settingsFile = settingsFile,
                rootBuildFile = rootBuildFile,
            ),
        )

        assertThat(report.isValid).isFalse()
        assertThat(report.errors).anySatisfy { message ->
            assertThat(message).contains(":plugin-core")
        }
        assertThat(report.errors).anySatisfy { message ->
            assertThat(message).contains("Test tasks")
        }
    }

    @Test
    fun `text validation collects errors`() {
        val report = CiValidationValidator.validateText(
            settingsContent = "include(\"plugin-core\")",
            buildContent = "",
        )

        assertThat(report.isValid).isFalse()
        assertThat(report.errors).isNotEmpty
    }
}
