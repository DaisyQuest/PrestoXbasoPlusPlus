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
            val lineWithoutComment = rawLine.substringBefore("//").trim()
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
}
