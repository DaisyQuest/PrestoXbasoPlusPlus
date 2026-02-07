package com.prestoxbasopp.testframework.completion

import com.prestoxbasopp.testframework.operations.OperationDefinition
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

private const val INPUT_FILE = "input.xb"
private const val COMPLETIONS_FILE = "expected.completions.json"
private const val RANKING_FILE = "expected.ranking.json"
private const val CARET_MARKER = "/*caret*/"

data class CompletionEntry(
    val label: String,
    val kind: String,
    val source: String,
    val insertText: String,
    val detail: String?,
    val type: String?,
)

data class CompletionSnapshot(
    val entries: List<CompletionEntry>,
)

data class RankingEntry(
    val label: String,
    val score: Double,
    val scope: String,
    val typeCompat: String,
    val tieBreak: String,
)

data class RankingSnapshot(
    val ranked: List<RankingEntry>,
)

data class CompletionCase(
    val source: String,
    val completions: CompletionSnapshot,
    val ranking: RankingSnapshot,
)

data class CompletionFixture(
    val id: String,
    val min: CompletionCase,
    val edge: CompletionCase,
) {
    fun cases(): List<Pair<String, CompletionCase>> {
        return listOf("$id:min" to min, "$id:edge" to edge)
    }
}

object CompletionFixtureLoader {
    private val yaml = Yaml()

    fun loadForOperation(operation: OperationDefinition, fixturesRoot: Path): CompletionFixture {
        val id = operation.id?.takeIf { it.isNotBlank() }
            ?: error("operation id is required to load completion fixtures")
        return load(fixturesRoot, id)
    }

    fun load(fixturesRoot: Path, id: String): CompletionFixture {
        val operationRoot = fixturesRoot.resolve(id)
        val min = loadCase(operationRoot.resolve("min"), "$id:min")
        val edge = loadCase(operationRoot.resolve("edge"), "$id:edge")
        return CompletionFixture(id = id, min = min, edge = edge)
    }

    private fun loadCase(caseRoot: Path, caseId: String): CompletionCase {
        val source = readRequiredFile(caseRoot.resolve(INPUT_FILE))
        require(source.contains(CARET_MARKER)) {
            "Missing caret marker '$CARET_MARKER' in completion input for $caseId"
        }
        val completionsText = readRequiredFile(caseRoot.resolve(COMPLETIONS_FILE))
        val rankingText = readRequiredFile(caseRoot.resolve(RANKING_FILE))
        val completions = parseCompletions(completionsText, caseId)
        val ranking = parseRanking(rankingText, caseId)
        return CompletionCase(source = source, completions = completions, ranking = ranking)
    }

    private fun parseCompletions(text: String, caseId: String): CompletionSnapshot {
        val root = requireMap(loadJson(text, caseId, "completions"), caseId, "completions")
        val entriesRaw = requireList(root["entries"], caseId, "entries")
        val entries = entriesRaw.mapIndexed { index, entry ->
            val entryMap = requireMap(entry, caseId, "entries[$index]")
            CompletionEntry(
                label = requireString(entryMap["label"], caseId, "entries[$index].label"),
                kind = requireString(entryMap["kind"], caseId, "entries[$index].kind"),
                source = requireString(entryMap["source"], caseId, "entries[$index].source"),
                insertText = requireString(entryMap["insertText"], caseId, "entries[$index].insertText"),
                detail = entryMap["detail"]?.toString(),
                type = entryMap["type"]?.toString(),
            )
        }
        return CompletionSnapshot(entries = entries)
    }

    private fun parseRanking(text: String, caseId: String): RankingSnapshot {
        val root = requireMap(loadJson(text, caseId, "ranking"), caseId, "ranking")
        val rankedRaw = requireList(root["ranked"], caseId, "ranked")
        val ranked = rankedRaw.mapIndexed { index, entry ->
            val entryMap = requireMap(entry, caseId, "ranked[$index]")
            RankingEntry(
                label = requireString(entryMap["label"], caseId, "ranked[$index].label"),
                score = requireDouble(entryMap["score"], caseId, "ranked[$index].score"),
                scope = requireString(entryMap["scope"], caseId, "ranked[$index].scope"),
                typeCompat = requireString(entryMap["typeCompat"], caseId, "ranked[$index].typeCompat"),
                tieBreak = requireString(entryMap["tieBreak"], caseId, "ranked[$index].tieBreak"),
            )
        }
        return RankingSnapshot(ranked = ranked)
    }

    private fun loadJson(text: String, caseId: String, label: String): Any {
        return try {
            yaml.load<Any>(text) ?: error("$label fixture is empty for $caseId")
        } catch (ex: Exception) {
            error("Failed to parse $label fixture for $caseId: ${ex.message}")
        }
    }

    private fun requireMap(value: Any?, caseId: String, label: String): Map<*, *> {
        return value as? Map<*, *> ?: error("Expected object for $label in $caseId")
    }

    private fun requireList(value: Any?, caseId: String, label: String): List<*> {
        return value as? List<*> ?: error("Expected list for $label in $caseId")
    }

    private fun requireString(value: Any?, caseId: String, label: String): String {
        return value as? String ?: error("Expected string for $label in $caseId")
    }

    private fun requireDouble(value: Any?, caseId: String, label: String): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        } ?: error("Expected number for $label in $caseId")
    }

    private fun readRequiredFile(path: Path): String {
        if (!Files.exists(path)) {
            error("Missing required completion fixture file: ${path.toAbsolutePath()}")
        }
        return Files.readString(path)
    }
}
