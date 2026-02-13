package com.prestoxbasopp.ide.xpj

data class XpjProjectFile(
    val sections: List<XpjSection>,
) {
    fun section(name: String): XpjSection? = sections.firstOrNull { it.name.equals(name, ignoreCase = true) }

    companion object {
        fun empty(): XpjProjectFile = XpjProjectFile(emptyList())
    }
}

data class XpjSection(
    val name: String,
    val entries: List<XpjEntry>,
)

sealed interface XpjEntry {
    data class Definition(val key: String, val value: String) : XpjEntry

    data class Reference(val value: String) : XpjEntry
}
