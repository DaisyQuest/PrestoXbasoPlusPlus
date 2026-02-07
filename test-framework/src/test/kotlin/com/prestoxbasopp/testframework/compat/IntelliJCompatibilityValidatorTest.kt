package com.prestoxbasopp.testframework.compat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class IntelliJCompatibilityValidatorTest {
    @Test
    fun `valid repository configuration passes compatibility check`() {
        val repoRoot = locateRepositoryRoot()
        val report = IntelliJCompatibilityValidator.validate(
            IntelliJCompatibilityInputs(
                pluginXml = repoRoot.resolve("plugin-ide/src/main/resources/META-INF/plugin.xml"),
                gradleBuildFile = repoRoot.resolve("plugin-ide/build.gradle.kts"),
            ),
        )

        assertThat(report.errors).isEmpty()
        assertThat(report.isCompatible).isTrue()
    }

    @Test
    fun `missing fields are reported`(@TempDir tempDir: Path) {
        val pluginXml = tempDir.resolve("plugin.xml")
        val gradleFile = tempDir.resolve("build.gradle.kts")
        pluginXml.toFile().writeText("<idea-plugin></idea-plugin>")
        gradleFile.toFile().writeText("plugins {}")

        val report = IntelliJCompatibilityValidator.validate(
            IntelliJCompatibilityInputs(
                pluginXml = pluginXml,
                gradleBuildFile = gradleFile,
            ),
        )

        assertThat(report.isCompatible).isFalse()
        assertThat(report.errors).anySatisfy { message ->
            assertThat(message).contains("plugin.xml must include <id>")
        }
        assertThat(report.errors).anySatisfy { message ->
            assertThat(message).contains("org.jetbrains.intellij")
        }
    }

    @Test
    fun `text validation aggregates errors`() {
        val report = IntelliJCompatibilityValidator.validateText(
            pluginXmlContent = "<plugin></plugin>",
            gradleContent = "intellij { }",
        )

        assertThat(report.isCompatible).isFalse()
        assertThat(report.errors).hasSizeGreaterThan(1)
    }
}
