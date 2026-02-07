package com.prestoxbasopp.core.psi

import com.prestoxbasopp.core.api.XbPsiElementContract
import com.prestoxbasopp.core.api.XbTextRange

enum class XbPsiElementType {
    FILE,
    FUNCTION_DECLARATION,
    VARIABLE_DECLARATION,
    BLOCK,
    SYMBOL_REFERENCE,
    LITERAL,
}

enum class XbSymbolRole {
    DECLARATION,
    USAGE,
}

interface XbPsiElement : XbPsiElementContract {
    val elementType: XbPsiElementType
    val text: String
    val children: List<XbPsiElement>
    var parent: XbPsiElement?

    fun walk(): Sequence<XbPsiElement> = sequence {
        yield(this@XbPsiElement)
        children.forEach { child ->
            yieldAll(child.walk())
        }
    }
}

interface XbPsiSymbol : XbPsiElement {
    val symbolName: String
    val role: XbSymbolRole
}

abstract class XbBasePsiElement(
    final override val name: String?,
    final override val textRange: XbTextRange,
    final override val text: String,
    final override val children: List<XbPsiElement>,
    final override val elementType: XbPsiElementType,
) : XbPsiElement {
    final override var parent: XbPsiElement? = null

    init {
        children.forEach { child -> child.parent = this }
    }
}
