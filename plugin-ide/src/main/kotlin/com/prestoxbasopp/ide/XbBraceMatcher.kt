package com.prestoxbasopp.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class XbBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(XbParenTokenTypes.LEFT_PAREN, XbParenTokenTypes.RIGHT_PAREN, false),
        BracePair(XbParenTokenTypes.LEFT_BRACKET, XbParenTokenTypes.RIGHT_BRACKET, false),
        BracePair(XbParenTokenTypes.LEFT_BRACE, XbParenTokenTypes.RIGHT_BRACE, false),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}

internal object XbParenTokenTypes {
    val LEFT_PAREN = XbTokenType("XBASEPP_LEFT_PAREN")
    val RIGHT_PAREN = XbTokenType("XBASEPP_RIGHT_PAREN")
    val LEFT_BRACKET = XbTokenType("XBASEPP_LEFT_BRACKET")
    val RIGHT_BRACKET = XbTokenType("XBASEPP_RIGHT_BRACKET")
    val LEFT_BRACE = XbTokenType("XBASEPP_LEFT_BRACE")
    val RIGHT_BRACE = XbTokenType("XBASEPP_RIGHT_BRACE")
}
