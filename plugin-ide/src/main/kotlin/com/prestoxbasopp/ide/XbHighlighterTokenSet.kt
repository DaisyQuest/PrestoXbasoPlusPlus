package com.prestoxbasopp.ide

import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.parser.TokenType

object XbHighlighterTokenSet {
    private val tokenMap: Map<TokenType, IElementType> = TokenType.entries.associateWith { type ->
        XbTokenType(type.name)
    }

    fun forToken(type: TokenType): IElementType = tokenMap.getValue(type)
}
