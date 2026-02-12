package com.prestoxbasopp.ide

import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbTokenType
import java.nio.file.Files
import java.nio.file.Path

enum class XbMacroExpansionStatus(val displayName: String, val severity: Int) {
    OK("OK", 0),
    UNRESOLVED_REFERENCE("Unresolved", 1),
    RECURSIVE_REFERENCE("Recursive", 2),
}

data class XbMacroExpansionEntry(
    val name: String,
    val rawValue: String,
    val expandedValue: String,
    val status: XbMacroExpansionStatus,
)

class XbMacroExpansionBuilder(private val lexer: XbLexer = XbLexer()) {
    private data class ExpansionResult(val text: String, val status: XbMacroExpansionStatus)

    fun build(source: String): List<XbMacroExpansionEntry> {
        val directives = lexer.lex(source).directives
        val definitions = directives.mapNotNull { parseDefinition(it.text) }
        if (definitions.isEmpty()) {
            return emptyList()
        }
        val context = ExpansionContext(
            lexer = lexer,
            definitions = definitions.associate { it.name to it.rawValue },
        )
        return definitions.map { definition ->
            val expansion = context.expand(definition.name)
            XbMacroExpansionEntry(
                name = definition.name,
                rawValue = definition.rawValue,
                expandedValue = expansion.text,
                status = expansion.status,
            )
        }
    }

    fun parseDefinitions(source: String): List<MacroDefinition> {
        val directives = lexer.lex(source).directives
        return directives.mapNotNull { parseDefinition(it.text) }
    }

    fun parseDefinition(text: String): MacroDefinition? {
        val match = DEFINE_REGEX.find(text) ?: return null
        val name = match.groupValues[1]
        val rawValue = match.groupValues[2].trimStart().trimEnd()
        if (name.isBlank()) {
            return null
        }
        return MacroDefinition(name, rawValue)
    }

    data class MacroDefinition(val name: String, val rawValue: String)

    private class ExpansionContext(
        private val lexer: XbLexer,
        private val definitions: Map<String, String>,
    ) {
        private val memo = mutableMapOf<String, ExpansionResult>()
        private val visiting = mutableSetOf<String>()

        fun expand(name: String): ExpansionResult {
            memo[name]?.let { return it }
            if (name in visiting) {
                return ExpansionResult(name, XbMacroExpansionStatus.RECURSIVE_REFERENCE)
            }
            val rawValue = definitions[name] ?: return ExpansionResult(name, XbMacroExpansionStatus.UNRESOLVED_REFERENCE)
            visiting += name
            val expansion = expandText(rawValue)
            visiting -= name
            memo[name] = expansion
            return expansion
        }

        private fun expandText(text: String): ExpansionResult {
            val tokens = lexer.lex(text).tokens.filter { it.type != XbTokenType.EOF }
            if (tokens.isEmpty()) {
                return ExpansionResult(text, XbMacroExpansionStatus.OK)
            }
            val builder = StringBuilder()
            var lastIndex = 0
            var status = XbMacroExpansionStatus.OK
            tokens.forEach { token ->
                builder.append(text.substring(lastIndex, token.range.startOffset))
                if (token.type == XbTokenType.IDENTIFIER) {
                    val replacement = definitions[token.text]
                    if (replacement == null) {
                        status = status.worst(XbMacroExpansionStatus.UNRESOLVED_REFERENCE)
                        builder.append(token.text)
                    } else {
                        val expanded = expand(token.text)
                        status = status.worst(expanded.status)
                        builder.append(expanded.text)
                    }
                } else {
                    builder.append(token.text)
                }
                lastIndex = token.range.endOffset
            }
            builder.append(text.substring(lastIndex))
            return ExpansionResult(builder.toString(), status)
        }

        private fun XbMacroExpansionStatus.worst(other: XbMacroExpansionStatus): XbMacroExpansionStatus {
            return if (other.severity > severity) other else this
        }
    }

    private companion object {
        private val DEFINE_REGEX = Regex("""^#\s*define\s+([A-Za-z_][A-Za-z0-9_]*)\b(.*)$""", RegexOption.IGNORE_CASE)
    }
}

enum class XbHeaderDefinitionKind(val displayName: String) {
    DEFINE("define"),
}

data class XbHeaderDefinitionEntry(
    val name: String,
    val value: String,
    val kind: XbHeaderDefinitionKind,
    val sourceFile: String,
)

enum class XbHeaderIncludeStatus(val displayName: String, val severity: Int) {
    LOADED("Loaded", 0),
    MISSING("Missing", 1),
}

data class XbHeaderIncludeEntry(
    val includeTarget: String,
    val resolvedPath: String?,
    val status: XbHeaderIncludeStatus,
)

enum class XbHeaderConflictStatus(val displayName: String) {
    CONFLICT("Conflict"),
}

data class XbHeaderConflictEntry(
    val name: String,
    val firstValue: String,
    val firstSourceFile: String,
    val secondValue: String,
    val secondSourceFile: String,
    val status: XbHeaderConflictStatus = XbHeaderConflictStatus.CONFLICT,
)

data class XbHeaderInsightPresentation(
    val includes: List<XbHeaderIncludeEntry>,
    val definitions: List<XbHeaderDefinitionEntry>,
    val conflicts: List<XbHeaderConflictEntry>,
    val message: String?,
)

class XbHeaderInsightBuilder(
    private val macroExpansionBuilder: XbMacroExpansionBuilder = XbMacroExpansionBuilder(),
    private val fileSystem: XbHeaderFileSystem = XbDefaultHeaderFileSystem,
) {
    fun build(source: String, sourceFilePath: String?, projectBasePath: String?): XbHeaderInsightPresentation {
        val directives = source.lineSequence()
            .map { it.trim() }
            .mapNotNull { parseIncludeDirective(it) }
            .toList()
        if (directives.isEmpty()) {
            return XbHeaderInsightPresentation(
                includes = emptyList(),
                definitions = emptyList(),
                conflicts = emptyList(),
                message = "No #include directives found.",
            )
        }

        val sourcePath = sourceFilePath?.let { fileSystem.pathOf(it) }
        val projectPath = projectBasePath?.let { fileSystem.pathOf(it) }

        val includeEntries = mutableListOf<XbHeaderIncludeEntry>()
        val definitions = mutableListOf<XbHeaderDefinitionEntry>()
        val conflicts = mutableListOf<XbHeaderConflictEntry>()
        val seenDefinitions = linkedMapOf<String, XbHeaderDefinitionEntry>()

        directives.forEach { includeTarget ->
            val resolved = resolveIncludePath(includeTarget, sourcePath, projectPath)
            if (resolved == null || !fileSystem.exists(resolved)) {
                includeEntries += XbHeaderIncludeEntry(includeTarget, null, XbHeaderIncludeStatus.MISSING)
                return@forEach
            }

            includeEntries += XbHeaderIncludeEntry(includeTarget, resolved.toString(), XbHeaderIncludeStatus.LOADED)
            val content = fileSystem.readText(resolved)
            val parsedDefinitions = macroExpansionBuilder.parseDefinitions(content)
            parsedDefinitions.forEach { definition ->
                val newEntry = XbHeaderDefinitionEntry(
                    name = definition.name,
                    value = definition.rawValue,
                    kind = XbHeaderDefinitionKind.DEFINE,
                    sourceFile = resolved.fileName?.toString() ?: resolved.toString(),
                )
                val previous = seenDefinitions[definition.name]
                if (previous == null) {
                    seenDefinitions[definition.name] = newEntry
                    definitions += newEntry
                } else if (previous.value != newEntry.value) {
                    conflicts += XbHeaderConflictEntry(
                        name = definition.name,
                        firstValue = previous.value,
                        firstSourceFile = previous.sourceFile,
                        secondValue = newEntry.value,
                        secondSourceFile = newEntry.sourceFile,
                    )
                }
            }
        }

        val message = if (definitions.isEmpty() && conflicts.isEmpty()) {
            "No header definitions found in included files."
        } else {
            null
        }

        return XbHeaderInsightPresentation(
            includes = includeEntries,
            definitions = definitions,
            conflicts = conflicts,
            message = message,
        )
    }

    private fun parseIncludeDirective(line: String): String? {
        val match = INCLUDE_REGEX.find(line) ?: return null
        return match.groupValues[1].ifBlank { null }
    }

    private fun resolveIncludePath(includeTarget: String, sourcePath: Path?, projectPath: Path?): Path? {
        val includePath = fileSystem.pathOf(includeTarget)
        if (includePath.isAbsolute) {
            return includePath
        }
        sourcePath?.parent?.resolve(includePath)?.normalize()?.let { return it }
        projectPath?.resolve(includePath)?.normalize()?.let { return it }
        return includePath.normalize()
    }

    private companion object {
        private val INCLUDE_REGEX = Regex("""^#\s*include\s+[\"<]([^\">]+)[\">]""", RegexOption.IGNORE_CASE)
    }
}

interface XbHeaderFileSystem {
    fun pathOf(rawPath: String): Path
    fun exists(path: Path): Boolean
    fun readText(path: Path): String
}

object XbDefaultHeaderFileSystem : XbHeaderFileSystem {
    override fun pathOf(rawPath: String): Path = Path.of(rawPath)

    override fun exists(path: Path): Boolean = Files.exists(path)

    override fun readText(path: Path): String = Files.readString(path)
}

data class XbMacroExpansionPresentation(
    val entries: List<XbMacroExpansionEntry>,
    val message: String?,
    val headerInsight: XbHeaderInsightPresentation,
)

object XbMacroExpansionFileSupport {
    private val supportedExtensions = setOf("xb", "prg")

    fun isXbaseFileName(fileName: String?): Boolean {
        val extension = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return extension in supportedExtensions
    }
}

class XbMacroExpansionPresenter(
    private val builder: XbMacroExpansionBuilder = XbMacroExpansionBuilder(),
    private val headerInsightBuilder: XbHeaderInsightBuilder = XbHeaderInsightBuilder(),
) {
    fun present(fileName: String?, filePath: String?, text: String?, projectBasePath: String?): XbMacroExpansionPresentation {
        if (fileName == null || text == null) {
            return XbMacroExpansionPresentation(
                entries = emptyList(),
                message = "Select an Xbase++ file to see expanded macros.",
                headerInsight = XbHeaderInsightPresentation(emptyList(), emptyList(), emptyList(), "Select an Xbase++ file to see header insight."),
            )
        }
        if (!XbMacroExpansionFileSupport.isXbaseFileName(fileName)) {
            return XbMacroExpansionPresentation(
                entries = emptyList(),
                message = "Expanded macros are available for .xb and .prg files only.",
                headerInsight = XbHeaderInsightPresentation(emptyList(), emptyList(), emptyList(), "Header insight is available for .xb and .prg files only."),
            )
        }
        val entries = builder.build(text)
        val headerInsight = headerInsightBuilder.build(text, filePath, projectBasePath)
        if (entries.isEmpty()) {
            return XbMacroExpansionPresentation(
                entries = entries,
                message = "No macros defined in $fileName.",
                headerInsight = headerInsight,
            )
        }
        return XbMacroExpansionPresentation(entries, null, headerInsight)
    }
}
