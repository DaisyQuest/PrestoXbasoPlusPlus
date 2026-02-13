package com.prestoxbasopp.ide.xpj

class XpjProjectSerializer {
    fun serialize(projectFile: XpjProjectFile): String =
        projectFile.sections.joinToString(separator = "\n\n") { section ->
            buildString {
                append("[")
                append(section.name)
                appendLine("]")
                section.entries.forEach { entry ->
                    when (entry) {
                        is XpjEntry.Definition -> appendLine("    ${entry.key} = ${entry.value}")
                        is XpjEntry.Reference -> appendLine("    ${entry.value}")
                    }
                }
            }.trimEnd()
        }
}
