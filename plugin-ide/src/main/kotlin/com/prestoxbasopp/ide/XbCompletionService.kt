package com.prestoxbasopp.ide

import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.stubs.XbStubType

data class XbCompletionItem(
    val name: String,
    val type: XbStubType,
)

class XbCompletionService {
    fun suggest(
        root: XbPsiElement,
        prefix: String,
        caseSensitive: Boolean = true,
    ): List<XbCompletionItem> {
        val normalizedPrefix = if (caseSensitive) prefix else prefix.lowercase()
        val items = root.walk()
            .mapNotNull { element ->
                when (element) {
                    is XbPsiFunctionDeclaration -> element.symbolName to XbStubType.FUNCTION
                    is XbPsiVariableDeclaration -> element.symbolName to XbStubType.VARIABLE
                    else -> null
                }
            }
            .mapNotNull { (name, type) ->
                val normalizedName = if (caseSensitive) name else name.lowercase()
                if (normalizedName.startsWith(normalizedPrefix)) {
                    XbCompletionItem(name, type)
                } else {
                    null
                }
            }
            .distinctBy { it.name to it.type }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.type.name }))
            .toList()
        return items
    }
}
