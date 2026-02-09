package com.prestoxbasopp.testframework.operations

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object OperationsRegistryLoader {
    fun load(path: Path): OperationsRegistry {
        Files.newInputStream(path).use { stream ->
            return load(stream)
        }
    }

    fun load(stream: InputStream): OperationsRegistry {
        val yaml = Yaml()
        val data = yaml.load<Map<String, Any?>>(stream)
        val version = (data["version"] as? Number)?.toInt()
            ?: error("operations.yaml must include integer 'version'")
        val operationsRaw = data["operations"] as? List<*> ?: emptyList<Any?>()
        val operations = operationsRaw.mapIndexed { index, entry ->
            val map = entry as? Map<*, *> ?: error("operation at index $index must be a map")
            OperationDefinition(
                id = map["id"] as? String,
                category = map["category"] as? String,
                exampleMin = map["example_min"] as? String,
                exampleEdge = map["example_edge"] as? String,
                expectedAstShape = map["expected_ast_shape"] as? String,
                expectedErrors = (map["expected_errors"] as? List<*>)?.mapNotNull { it as? String },
                precedenceGroup = map["precedence_group"] as? String,
            )
        }
        return OperationsRegistry(version, operations)
    }
}

data class OperationsRegistry(
    val version: Int,
    val operations: List<OperationDefinition>,
)

data class OperationDefinition(
    val id: String?,
    val category: String?,
    val exampleMin: String?,
    val exampleEdge: String?,
    val expectedAstShape: String?,
    val expectedErrors: List<String>?,
    val precedenceGroup: String?,
)

object OperationsSchemaValidator {
    fun validate(registry: OperationsRegistry): List<SchemaViolation> {
        val violations = mutableListOf<SchemaViolation>()
        if (registry.version != 1) {
            violations += SchemaViolation("version", "Version must be 1")
        }
        val seenIds = mutableSetOf<String>()
        registry.operations.forEachIndexed { index, operation ->
            val basePath = "operations[$index]"
            val id = operation.id
            if (id.isNullOrBlank()) {
                violations += SchemaViolation("$basePath.id", "id is required")
            } else if (!seenIds.add(id)) {
                violations += SchemaViolation("$basePath.id", "id must be unique")
            }
            if (operation.category.isNullOrBlank()) {
                violations += SchemaViolation("$basePath.category", "category is required")
            }
            if (operation.exampleMin.isNullOrBlank()) {
                violations += SchemaViolation("$basePath.example_min", "example_min is required")
            }
            if (operation.exampleEdge.isNullOrBlank()) {
                violations += SchemaViolation("$basePath.example_edge", "example_edge is required")
            }
            if (operation.expectedAstShape.isNullOrBlank()) {
                violations += SchemaViolation("$basePath.expected_ast_shape", "expected_ast_shape is required")
            }
            if (operation.expectedErrors == null) {
                violations += SchemaViolation("$basePath.expected_errors", "expected_errors is required")
            }
        }
        return violations
    }
}

data class SchemaViolation(
    val path: String,
    val message: String,
)

object OperationsCoverageGate {
    fun validate(registry: OperationsRegistry, fixturesRoot: Path): CoverageResult {
        val missingFixtures = mutableMapOf<String, List<String>>()
        registry.operations.forEach { operation ->
            val id = operation.id ?: return@forEach
            val required = listOf(
                "${id}_min.xb",
                "${id}_edge.xb",
                "${id}.ast.txt",
                "${id}_edge.ast.txt",
            )
            val missing = required.filterNot { fixtureName ->
                Files.exists(fixturesRoot.resolve(fixtureName))
            }
            if (missing.isNotEmpty()) {
                missingFixtures[id] = missing
            }
        }
        return CoverageResult(missingFixtures)
    }

    fun requireCoverage(registry: OperationsRegistry, fixturesRoot: Path) {
        val result = validate(registry, fixturesRoot)
        if (result.missingFixtures.isNotEmpty()) {
            val message = result.missingFixtures.entries.joinToString(
                prefix = "Missing fixtures: ",
                separator = "; ",
            ) { (id, missing) ->
                "$id -> ${missing.joinToString()}"
            }
            error(message)
        }
    }
}

data class CoverageResult(
    val missingFixtures: Map<String, List<String>>,
) {
    val isComplete: Boolean = missingFixtures.isEmpty()
}
