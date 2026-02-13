package com.prestoxbasopp.ide.xpj

class XpjProjectParser {
    fun parse(text: String): XpjProjectFile {
        val sections = mutableListOf<XpjSection>()
        var currentSectionName: String? = null
        val currentEntries = mutableListOf<XpjEntry>()

        fun flushSection() {
            val name = currentSectionName ?: return
            sections += XpjSection(name = name, entries = currentEntries.toList())
            currentEntries.clear()
        }

        text.lineSequence().forEach { rawLine ->
            val lineWithoutComment = stripComments(rawLine).trim()
            if (lineWithoutComment.isBlank()) {
                return@forEach
            }

            if (lineWithoutComment.startsWith("[") && lineWithoutComment.endsWith("]")) {
                flushSection()
                currentSectionName = lineWithoutComment.removePrefix("[").removeSuffix("]").trim()
                return@forEach
            }

            if (currentSectionName == null) {
                return@forEach
            }

            val equalsIndex = lineWithoutComment.indexOf('=')
            if (equalsIndex >= 0) {
                currentEntries += XpjEntry.Definition(
                    key = lineWithoutComment.substring(0, equalsIndex).trim(),
                    value = lineWithoutComment.substring(equalsIndex + 1).trim(),
                )
            } else {
                currentEntries += XpjEntry.Reference(lineWithoutComment)
            }
        }

        flushSection()
        return XpjProjectFile(sections)
    }

    private fun stripComments(rawLine: String): String {
        var inSingleQuotes = false
        var inDoubleQuotes = false
        var index = 0
        while (index < rawLine.length - 1) {
            when (rawLine[index]) {
                '\'' -> if (!inDoubleQuotes) inSingleQuotes = !inSingleQuotes
                '"' -> if (!inSingleQuotes) inDoubleQuotes = !inDoubleQuotes
                '/' -> {
                    if (!inSingleQuotes && !inDoubleQuotes && rawLine[index + 1] == '/') {
                        val hasWhitespaceBeforeComment = index == 0 || rawLine[index - 1].isWhitespace()
                        if (hasWhitespaceBeforeComment) {
                            return rawLine.substring(0, index)
                        }
                    }
                }
            }
            index++
        }
        return rawLine
    }
}
