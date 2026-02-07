package com.prestoxbasopp.ide

import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType

object XbHighlighterTokenSet {
    private val tokenMap: Map<XbLexerTokenType, IElementType> = XbLexerTokenType.entries.associateWith { type ->
        XbTokenType(type.name)
    }

    fun forToken(type: XbLexerTokenType): IElementType = tokenMap.getValue(type)
}
