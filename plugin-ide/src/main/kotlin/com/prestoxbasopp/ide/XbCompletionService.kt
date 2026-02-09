package com.prestoxbasopp.ide

import com.prestoxbasopp.core.lexer.XbKeywords
import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration

data class XbCompletionItem(
    val name: String,
    val type: XbCompletionType,
    val detail: String? = null,
)

enum class XbCompletionType {
    FUNCTION,
    VARIABLE,
    KEYWORD,
    COMMAND_ATTRIBUTE,
    TABLE_COLUMN,
}

class XbCompletionService(
    private val keywords: Set<String> = XbKeywords.all,
    private val metadataService: XbCompletionMetadataService = XbCompletionMetadataService(),
) {
    fun suggest(
        root: XbPsiElement,
        prefix: String,
        caseSensitive: Boolean = true,
        context: XbCompletionContext? = null,
    ): List<XbCompletionItem> {
        val normalizedPrefix = if (caseSensitive) prefix else prefix.lowercase()
        val items = buildList {
            root.walk()
                .mapNotNull { element ->
                    when (element) {
                        is XbPsiFunctionDeclaration -> element.symbolName to XbCompletionType.FUNCTION
                        is XbPsiVariableDeclaration -> element.symbolName to XbCompletionType.VARIABLE
                        else -> null
                    }
                }
                .mapNotNull { (name, type) ->
                    if (matchesPrefix(name, normalizedPrefix, caseSensitive)) {
                        XbCompletionItem(name, type)
                    } else {
                        null
                    }
                }
                .forEach { add(it) }
            keywords.asSequence()
                .map { it.uppercase() }
                .filter { matchesPrefix(it, normalizedPrefix, caseSensitive) }
                .map { XbCompletionItem(it, XbCompletionType.KEYWORD) }
                .forEach { add(it) }
            context?.let { completionContext ->
                addAll(metadataService.suggest(completionContext, prefix, caseSensitive))
            }
        }
        return items
            .distinctBy { it.name to it.type }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.type.name }))
            .toList()
    }

    private fun matchesPrefix(value: String, normalizedPrefix: String, caseSensitive: Boolean): Boolean {
        val normalizedValue = if (caseSensitive) value else value.lowercase()
        return normalizedValue.startsWith(normalizedPrefix)
    }
}
