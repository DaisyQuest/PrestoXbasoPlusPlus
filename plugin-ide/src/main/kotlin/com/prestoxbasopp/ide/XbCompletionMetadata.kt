package com.prestoxbasopp.ide

data class XbCompletionMetadata(
    val commands: List<XbCommandMetadata> = emptyList(),
    val tables: List<XbTableMetadata> = emptyList(),
)

data class XbCommandMetadata(
    val name: String = "",
    val attributes: List<XbCommandAttributeMetadata> = emptyList(),
)

data class XbCommandAttributeMetadata(
    val name: String = "",
    val type: String? = null,
)

data class XbTableMetadata(
    val name: String = "",
    val columns: List<XbTableColumnMetadata> = emptyList(),
)

data class XbTableColumnMetadata(
    val name: String = "",
    val type: String? = null,
    val length: Int? = null,
)

class XbCompletionMetadataIndex(metadata: XbCompletionMetadata) {
    private val commands: Map<String, XbCommandMetadata> = metadata.commands
        .associateBy { it.name.uppercase() }
    private val tables: Map<String, XbTableMetadata> = metadata.tables
        .associateBy { it.name.uppercase() }

    fun command(name: String): XbCommandMetadata? = commands[name.uppercase()]

    fun table(name: String): XbTableMetadata? = tables[name.uppercase()]
}

fun XbCommandAttributeMetadata.typeText(): String = type?.uppercase() ?: "ATTRIBUTE"

fun XbTableColumnMetadata.typeText(): String {
    val normalizedType = type?.uppercase() ?: "COLUMN"
    return length?.let { "$normalizedType($it)" } ?: normalizedType
}
