package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationsRegistry
import java.nio.file.Path

object CompletionCoverageGate {
    fun validate(registry: OperationsRegistry, fixturesRoot: Path): CompletionCoverageResult {
        val missingFixtures = mutableMapOf<String, List<String>>()
        val invalidFixtures = mutableMapOf<String, String>()
        registry.operations.forEach { operation ->
            val id = operation.id ?: return@forEach
            val missing = requiredPaths(fixturesRoot, id)
                .filterNot { it.toFile().exists() }
                .map { fixturesRoot.relativize(it).toString() }
            if (missing.isNotEmpty()) {
                missingFixtures[id] = missing
                return@forEach
            }
            try {
                CompletionFixtureLoader.load(fixturesRoot, id)
            } catch (ex: Exception) {
                val message = ex.message ?: "Unknown completion fixture error"
                invalidFixtures[id] = message
            }
        }
        return CompletionCoverageResult(missingFixtures, invalidFixtures)
    }

    fun requireCoverage(registry: OperationsRegistry, fixturesRoot: Path) {
        val result = validate(registry, fixturesRoot)
        if (!result.isComplete) {
            val missingMessage = result.missingFixtures.entries.joinToString(
                prefix = "Missing completion fixtures: ",
                separator = "; ",
            ) { (id, missing) ->
                "$id -> ${missing.joinToString()}"
            }
            val invalidMessage = result.invalidFixtures.entries.joinToString(
                prefix = "Invalid completion fixtures: ",
                separator = "; ",
            ) { (id, message) ->
                "$id -> $message"
            }
            val combined = listOf(missingMessage, invalidMessage)
                .filterNot { it.endsWith(": ") }
                .joinToString(" | ")
            error(combined)
        }
    }
}

data class CompletionCoverageResult(
    val missingFixtures: Map<String, List<String>>,
    val invalidFixtures: Map<String, String>,
) {
    val isComplete: Boolean = missingFixtures.isEmpty() && invalidFixtures.isEmpty()
}

private fun requiredPaths(fixturesRoot: Path, id: String): List<Path> {
    val root = fixturesRoot.resolve(id)
    return listOf(
        root.resolve("min").resolve("input.xb"),
        root.resolve("min").resolve("expected.completions.json"),
        root.resolve("min").resolve("expected.ranking.json"),
        root.resolve("edge").resolve("input.xb"),
        root.resolve("edge").resolve("expected.completions.json"),
        root.resolve("edge").resolve("expected.ranking.json"),
    )
}
