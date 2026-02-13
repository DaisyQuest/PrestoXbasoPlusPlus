package com.prestoxbasopp.ide.xpj

class XpjEditorModel(
    initialFile: XpjProjectFile,
) {
    private val sections = initialFile.sections.toMutableList()

    fun snapshot(): XpjProjectFile = XpjProjectFile(sections = sections.toList())

    fun sectionNames(): List<String> = sections.map { it.name }

    fun section(name: String): XpjSection? = sections.firstOrNull { it.name == name }

    fun addSection(name: String) {
        require(name.isNotBlank()) { "Section name must not be blank" }
        if (sections.any { it.name.equals(name, ignoreCase = true) }) {
            return
        }
        sections += XpjSection(name.trim(), emptyList())
    }

    fun addDefinition(sectionName: String, key: String, value: String) {
        updateSection(sectionName) { section ->
            section.copy(entries = section.entries + XpjEntry.Definition(key.trim(), value.trim()))
        }
    }

    fun addReference(sectionName: String, value: String) {
        updateSection(sectionName) { section ->
            section.copy(entries = section.entries + XpjEntry.Reference(value.trim()))
        }
    }

    fun removeEntry(sectionName: String, index: Int) {
        updateSection(sectionName) { section ->
            section.copy(entries = section.entries.filterIndexed { position, _ -> position != index })
        }
    }

    private fun updateSection(sectionName: String, updater: (XpjSection) -> XpjSection) {
        val position = sections.indexOfFirst { it.name == sectionName }
        if (position < 0) {
            return
        }
        sections[position] = updater(sections[position])
    }
}
