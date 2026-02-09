package com.prestoxbasopp.ide

import java.nio.file.Path


data class XbCompletionContext(
    val text: String,
    val offset: Int,
    val projectBasePath: Path?,
)

class XbCompletionContextAnalyzer(
    private val tableFocusResolver: XbTableFocusResolver = XbTableFocusResolver(),
) {
    fun detectCommandContext(
        context: XbCompletionContext,
        metadataIndex: XbCompletionMetadataIndex,
    ): String? {
        val linePrefix = context.linePrefix()
        val match = COMMAND_LINE_REGEX.find(linePrefix) ?: return null
        val command = match.groupValues[1]
        return metadataIndex.command(command)?.name
    }

    fun detectTableContext(
        context: XbCompletionContext,
        metadataIndex: XbCompletionMetadataIndex,
    ): String? {
        val linePrefix = context.linePrefix()
        val aliasMatch = TABLE_ALIAS_REGEX.find(linePrefix)
        if (aliasMatch != null) {
            val alias = aliasMatch.groupValues[1]
            return metadataIndex.table(alias)?.name
        }
        val focusMatch = NO_ALIAS_REGEX.find(linePrefix)
        if (focusMatch != null || linePrefix.trimEnd().endsWith("->")) {
            val current = tableFocusResolver.resolve(context.text, context.offset)
            return current?.let { metadataIndex.table(it)?.name }
        }
        return null
    }

    private fun XbCompletionContext.linePrefix(): String {
        val safeOffset = offset.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', safeOffset - 1).let { if (it == -1) 0 else it + 1 }
        return text.substring(lineStart, safeOffset)
    }

    private companion object {
        val COMMAND_LINE_REGEX = Regex("^\\s*([A-Za-z_][\\w]*)\\b")
        val TABLE_ALIAS_REGEX = Regex("([A-Za-z_][\\w]*)->([A-Za-z_\\w]*)$")
        val NO_ALIAS_REGEX = Regex("(?:^|\\s)->([A-Za-z_\\w]*)$")
    }
}
