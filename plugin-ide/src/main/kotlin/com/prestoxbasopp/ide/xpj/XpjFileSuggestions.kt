package com.prestoxbasopp.ide.xpj

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class XpjFileSuggestions(
    private val projectRoot: Path,
    private val maxDepth: Int = 6,
    private val allowedExtensions: Set<String> = setOf("prg", "arc", "ch", "lib", "def", "obj", "exe", "dll", "xpj"),
) {
    fun suggest(prefix: String): List<String> {
        val normalized = prefix.trim().lowercase()
        if (normalized.isEmpty()) {
            return emptyList()
        }

        return Files.walk(projectRoot, maxDepth)
            .use { stream ->
                stream
                    .filter { it.isRegularFile() }
                    .map { projectRoot.relativize(it).toString().replace('\\', '/') }
                    .filter { path -> hasAllowedExtension(path) }
                    .filter { path ->
                        val fileName = Path.of(path).name.lowercase()
                        fileName.startsWith(normalized) || path.lowercase().contains(normalized)
                    }
                    .sorted(compareBy<String> { score(normalized, it) }.thenBy { it })
                    .limit(12)
                    .collect(java.util.stream.Collectors.toList())
            }
    }

    private fun hasAllowedExtension(path: String): Boolean {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in allowedExtensions
    }

    private fun score(prefix: String, candidate: String): Int {
        val fileName = Path.of(candidate).name.lowercase()
        return when {
            fileName == prefix -> 0
            fileName.startsWith(prefix) -> 1
            candidate.lowercase().contains(prefix) -> 2
            else -> 3
        }
    }
}
