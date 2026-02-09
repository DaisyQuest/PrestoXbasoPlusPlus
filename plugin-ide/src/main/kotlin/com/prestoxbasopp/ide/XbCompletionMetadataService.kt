package com.prestoxbasopp.ide

class XbCompletionMetadataService(
    private val provider: XbCompletionMetadataProvider = XbCompletionMetadataRepository(),
    private val contextAnalyzer: XbCompletionContextAnalyzer = XbCompletionContextAnalyzer(),
) {
    fun suggest(
        context: XbCompletionContext,
        prefix: String,
        caseSensitive: Boolean,
    ): List<XbCompletionItem> {
        val metadata = provider.load(context.projectBasePath)
        val index = XbCompletionMetadataIndex(metadata)
        val items = mutableListOf<XbCompletionItem>()

        contextAnalyzer.detectCommandContext(context, index)?.let { commandName ->
            index.command(commandName)?.attributes
                ?.filter { matchesPrefix(it.name, prefix, caseSensitive) }
                ?.forEach { attribute ->
                    items += XbCompletionItem(
                        name = attribute.name,
                        type = XbCompletionType.COMMAND_ATTRIBUTE,
                        detail = attribute.typeText(),
                    )
                }
        }

        contextAnalyzer.detectTableContext(context, index)?.let { tableName ->
            index.table(tableName)?.columns
                ?.filter { matchesPrefix(it.name, prefix, caseSensitive) }
                ?.forEach { column ->
                    items += XbCompletionItem(
                        name = column.name,
                        type = XbCompletionType.TABLE_COLUMN,
                        detail = column.typeText(),
                    )
                }
        }

        return items
    }

    private fun matchesPrefix(value: String, prefix: String, caseSensitive: Boolean): Boolean {
        val normalizedPrefix = if (caseSensitive) prefix else prefix.lowercase()
        val normalizedValue = if (caseSensitive) value else value.lowercase()
        return normalizedValue.startsWith(normalizedPrefix)
    }
}
