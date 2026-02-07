package com.prestoxbasopp.ide

class XbFormatter {
    fun format(source: String, indentSize: Int = 4): String {
        val indentUnit = " ".repeat(indentSize.coerceAtLeast(0))
        var indentLevel = 0
        val formattedLines = mutableListOf<String>()
        val lines = source.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                formattedLines += ""
                continue
            }
            val lower = trimmed.lowercase()
            val shouldDecrease = lower.startsWith("endif") ||
                lower.startsWith("enddo") ||
                lower.startsWith("else")
            if (shouldDecrease) {
                indentLevel = (indentLevel - 1).coerceAtLeast(0)
            }
            formattedLines += indentUnit.repeat(indentLevel) + trimmed
            val shouldIncrease = lower.startsWith("if ") ||
                lower == "if" ||
                lower.startsWith("while ") ||
                lower == "while" ||
                lower.startsWith("else")
            if (shouldIncrease) {
                indentLevel += 1
            }
        }
        return formattedLines.joinToString("\n")
    }
}
