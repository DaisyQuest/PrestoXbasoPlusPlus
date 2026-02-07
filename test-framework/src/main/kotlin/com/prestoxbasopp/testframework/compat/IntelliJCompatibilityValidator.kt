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

        if (!gradleContent.contains("id(\"org.jetbrains.intellij\")")) {
            errors.add("Gradle build must apply the org.jetbrains.intellij plugin.")
        }
        if (!Regex("intellij\\s*\\{").containsMatchIn(gradleContent)) {
            errors.add("Gradle build must configure the intellij block.")
        }
        if (!Regex("version\\.set\\(\"").containsMatchIn(gradleContent)) {
            errors.add("Gradle intellij block must set a version.")
        }
        if (!Regex("type\\.set\\(\"").containsMatchIn(gradleContent)) {
            errors.add("Gradle intellij block must set a type.")
        }
        if (!Regex("patchPluginXml\\s*\\{").containsMatchIn(gradleContent)) {
            errors.add("Gradle build must configure patchPluginXml.")
        }
        if (!Regex("sinceBuild\\.set\\(\"").containsMatchIn(gradleContent)) {
            errors.add("patchPluginXml must set sinceBuild.")
        }
        if (!Regex("untilBuild\\.set\\(\"").containsMatchIn(gradleContent)) {
            errors.add("patchPluginXml must set untilBuild.")
        }

        return IntelliJCompatibilityReport(errors)
    }
}
