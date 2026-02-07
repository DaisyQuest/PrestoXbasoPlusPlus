package com.prestoxbasopp.testframework.compat

import java.nio.file.Files
import java.nio.file.Path

internal fun locateRepositoryRoot(): Path {
    var current = Path.of("").toAbsolutePath()
    while (current.parent != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
        current = current.parent
    }
    require(Files.exists(current.resolve("settings.gradle.kts"))) {
        "Unable to locate repository root from ${Path.of("").toAbsolutePath()}"
    }
    return current
}
