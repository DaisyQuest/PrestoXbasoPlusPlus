package com.prestoxbasopp.testframework.compat

import java.nio.file.Files
import java.nio.file.Path

data class CiValidationInputs(
    val settingsFile: Path,
    val rootBuildFile: Path,
)

data class CiValidationReport(
    val errors: List<String>,
) {
    val isValid: Boolean = errors.isEmpty()
}

object CiValidationValidator {
    fun validate(inputs: CiValidationInputs): CiValidationReport {
        val settingsContent = Files.readString(inputs.settingsFile)
        val buildContent = Files.readString(inputs.rootBuildFile)
        return validateText(settingsContent, buildContent)
    }

    fun validateText(settingsContent: String, buildContent: String): CiValidationReport {
        val errors = mutableListOf<String>()
        listOf(
            ":plugin-core",
            ":plugin-ide",
            ":plugin-ui",
            ":test-framework",
        ).forEach { module ->
            if (!settingsContent.contains("include(\"$module\")") && !settingsContent.contains("\"${module.removePrefix(":")}\"")) {
                errors.add("settings.gradle.kts must include module $module.")
            }
        }

        if (!buildContent.contains("tasks.withType<Test>()")) {
            errors.add("Root build must configure Test tasks for JUnit.")
        }
        if (!buildContent.contains("org.jetbrains.intellij")) {
            errors.add("Root build must declare the org.jetbrains.intellij plugin version.")
        }

        return CiValidationReport(errors)
    }
}
