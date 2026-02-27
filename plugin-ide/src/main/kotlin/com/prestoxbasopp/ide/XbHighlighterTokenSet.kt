package com.prestoxbasopp.ide

import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType
import com.prestoxbasopp.ui.XbHighlightCategory

object XbHighlighterTokenSet {
    val MACRO_DEFINITION: IElementType = XbTokenType("MACRO_DEFINITION")
    val FUNCTION_DECLARATION: IElementType = XbTokenType("FUNCTION_DECLARATION")
    val FUNCTION_CALL: IElementType = XbTokenType("FUNCTION_CALL")

    private val tokenMap: Map<XbLexerTokenType, IElementType> = XbLexerTokenType.entries.associateWith { type ->
        XbTokenType(type.name)
    }
    private val styleMap: Map<XbHighlightCategory, IElementType> = XbHighlightCategory.entries.associateWith { category ->
        XbTokenType("XBASEPP_STYLE_${category.name}")
    }

    fun forToken(type: XbLexerTokenType): IElementType = tokenMap.getValue(type)

    fun forHighlightCategory(category: XbHighlightCategory): IElementType = styleMap.getValue(category)

    fun tokenTypeForStyleElement(type: IElementType): XbHighlightCategory? {
        return styleMap.entries.firstOrNull { it.value == type }?.key
    }
}
