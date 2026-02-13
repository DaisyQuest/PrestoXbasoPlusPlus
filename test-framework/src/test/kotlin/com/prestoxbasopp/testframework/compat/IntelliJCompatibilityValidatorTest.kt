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
            assertThat(message).contains("org.jetbrains.intellij.platform")
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

    @Test
    fun `legacy plugin id is accepted to preserve migration compatibility`() {
        val report = IntelliJCompatibilityValidator.validateText(
            pluginXmlContent = """
                <idea-plugin>
                  <id>test</id>
                  <name>test</name>
                  <vendor>vendor</vendor>
                  <depends>com.intellij.modules.platform</depends>
                </idea-plugin>
            """.trimIndent(),
            gradleContent = """
                plugins { id("org.jetbrains.intellij") }
                intellijPlatform { pluginConfiguration { ideaVersion { sinceBuild = "233"; untilBuild = "241.*" } } }
            """.trimIndent(),
        )

        assertThat(report.isCompatible).isTrue()
    }
}
