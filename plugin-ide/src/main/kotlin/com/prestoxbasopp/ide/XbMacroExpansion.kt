package com.prestoxbasopp.ide

import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbTokenType

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

    private fun parseDefinition(text: String): MacroDefinition? {
        val match = DEFINE_REGEX.find(text) ?: return null
        val name = match.groupValues[1]
        val rawValue = match.groupValues[2].trimStart().trimEnd()
        if (name.isBlank()) {
            return null
        }
        return MacroDefinition(name, rawValue)
    }

    private data class MacroDefinition(val name: String, val rawValue: String)

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

data class XbMacroExpansionPresentation(
    val entries: List<XbMacroExpansionEntry>,
    val message: String?,
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
) {
    fun present(fileName: String?, text: String?): XbMacroExpansionPresentation {
        if (fileName == null || text == null) {
            return XbMacroExpansionPresentation(
                entries = emptyList(),
                message = "Select an Xbase++ file to see expanded macros.",
            )
        }
        if (!XbMacroExpansionFileSupport.isXbaseFileName(fileName)) {
            return XbMacroExpansionPresentation(
                entries = emptyList(),
                message = "Expanded macros are available for .xb and .prg files only.",
            )
        }
        val entries = builder.build(text)
        if (entries.isEmpty()) {
            return XbMacroExpansionPresentation(
                entries = entries,
                message = "No macros defined in $fileName.",
            )
        }
        return XbMacroExpansionPresentation(entries, null)
    }
}
