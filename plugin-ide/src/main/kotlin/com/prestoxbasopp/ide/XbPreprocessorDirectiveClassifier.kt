package com.prestoxbasopp.ide

internal fun isMacroDefinitionDirective(text: String): Boolean {
    val trimmed = text.trimStart()
    if (!trimmed.startsWith("#")) {
        return false
    }
    val afterHash = trimmed.drop(1).trimStart()
    if (!afterHash.startsWith("define", ignoreCase = true)) {
        return false
    }
    val remainder = afterHash.drop(6)
    if (remainder.isEmpty()) {
        return true
    }
    return remainder.first().isWhitespace()
}
