package com.prestoxbasopp.testframework.compat

import java.nio.file.Files
import java.nio.file.Path

data class IntelliJCompatibilityInputs(
    val pluginXml: Path,
    val gradleBuildFile: Path,
)

data class IntelliJCompatibilityReport(
    val errors: List<String>,
) {
    val isCompatible: Boolean = errors.isEmpty()
}

object IntelliJCompatibilityValidator {
    private val intellijPluginRegex = Regex("""id\("org\.jetbrains\.intellij(\.platform)?"\)""")
    private val intellijPlatformBlockRegex = Regex("""intellijPlatform\s*\{""")
    private val pluginConfigurationBlockRegex = Regex("""pluginConfiguration\s*\{""")
    private val ideaVersionBlockRegex = Regex("""ideaVersion\s*\{""")
    private val sinceBuildRegex = Regex("""sinceBuild\s*=\s*""")
    private val untilBuildRegex = Regex("""untilBuild\s*=\s*""")

    fun validate(inputs: IntelliJCompatibilityInputs): IntelliJCompatibilityReport {
        val pluginXmlContent = Files.readString(inputs.pluginXml)
        val gradleContent = Files.readString(inputs.gradleBuildFile)
        return validateText(pluginXmlContent, gradleContent)
    }

    fun validateText(pluginXmlContent: String, gradleContent: String): IntelliJCompatibilityReport {
        val errors = mutableListOf<String>()
        if (!pluginXmlContent.contains("<idea-plugin>")) {
            errors.add("plugin.xml must declare <idea-plugin> root element.")
        }
        if (!pluginXmlContent.contains("<id>")) {
            errors.add("plugin.xml must include <id>.")
        }
        if (!pluginXmlContent.contains("<name>")) {
            errors.add("plugin.xml must include <name>.")
        }
        if (!pluginXmlContent.contains("<vendor")) {
            errors.add("plugin.xml must include <vendor>.")
        }
        if (!pluginXmlContent.contains("<depends>com.intellij.modules.platform</depends>")) {
            errors.add("plugin.xml must depend on com.intellij.modules.platform.")
        }

        if (!intellijPluginRegex.containsMatchIn(gradleContent)) {
            errors.add("Gradle build must apply the org.jetbrains.intellij.platform plugin.")
        }
        if (!intellijPlatformBlockRegex.containsMatchIn(gradleContent)) {
            errors.add("Gradle build must configure the intellijPlatform block.")
        }
        if (!pluginConfigurationBlockRegex.containsMatchIn(gradleContent)) {
            errors.add("Gradle intellijPlatform block must configure pluginConfiguration.")
        }
        if (!ideaVersionBlockRegex.containsMatchIn(gradleContent)) {
            errors.add("Gradle pluginConfiguration must configure ideaVersion.")
        }
        if (!sinceBuildRegex.containsMatchIn(gradleContent)) {
            errors.add("ideaVersion must set sinceBuild.")
        }
        if (!untilBuildRegex.containsMatchIn(gradleContent)) {
            errors.add("ideaVersion must set untilBuild.")
        }

        return IntelliJCompatibilityReport(errors)
    }
}
