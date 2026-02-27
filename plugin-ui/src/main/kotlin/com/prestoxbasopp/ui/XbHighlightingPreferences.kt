package com.prestoxbasopp.ui

enum class XbHighlightCategory {
    KEYWORD,
    IDENTIFIER,
    FUNCTION_DECLARATION,
    FUNCTION_CALL,
    NUMBER,
    STRING,
    DATE,
    SYMBOL,
    CODEBLOCK,
    PREPROCESSOR,
    MACRO_DEFINITION,
    OPERATOR,
    PUNCTUATION,
    COMMENT,
    ERROR,
}

data class XbHighlightingPreferences(
    val styleMappings: Map<XbHighlightCategory, XbHighlightCategory> = defaultStyleMappings(),
    val wordOverrides: Map<String, XbHighlightCategory> = emptyMap(),
) {
    init {
        require(styleMappings.keys.containsAll(XbHighlightCategory.entries)) {
            "Style mappings must contain every highlight category."
        }
    }

    fun withNormalizedOverrides(): XbHighlightingPreferences {
        val normalized = wordOverrides.entries
            .asSequence()
            .map { it.key.trim().lowercase() to it.value }
            .filter { it.first.isNotEmpty() }
            .associate { it }
        return copy(wordOverrides = normalized)
    }

    companion object {
        fun defaultStyleMappings(): Map<XbHighlightCategory, XbHighlightCategory> {
            return XbHighlightCategory.entries.associateWith { it }
        }
    }
}
