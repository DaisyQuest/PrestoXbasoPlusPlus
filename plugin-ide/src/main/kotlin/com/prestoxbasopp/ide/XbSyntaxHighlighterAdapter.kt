package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType

class XbSyntaxHighlighterAdapter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = XbLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val tokenName = tokenType.toString()
        val token = runCatching { XbLexerTokenType.valueOf(tokenName) }.getOrNull()
        val attributes = when (token) {
            XbLexerTokenType.KEYWORD -> DefaultLanguageHighlighterColors.KEYWORD
            XbLexerTokenType.IDENTIFIER -> DefaultLanguageHighlighterColors.IDENTIFIER
            XbLexerTokenType.NUMBER -> DefaultLanguageHighlighterColors.NUMBER
            XbLexerTokenType.STRING -> DefaultLanguageHighlighterColors.STRING
            XbLexerTokenType.DATE -> DefaultLanguageHighlighterColors.CONSTANT
            XbLexerTokenType.SYMBOL -> DefaultLanguageHighlighterColors.METADATA
            XbLexerTokenType.CODEBLOCK -> DefaultLanguageHighlighterColors.STRING
            XbLexerTokenType.OPERATOR -> DefaultLanguageHighlighterColors.OPERATION_SIGN
            XbLexerTokenType.PUNCTUATION -> DefaultLanguageHighlighterColors.PARENTHESES
            XbLexerTokenType.COMMENT -> DefaultLanguageHighlighterColors.LINE_COMMENT
            XbLexerTokenType.UNKNOWN -> DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
            XbLexerTokenType.EOF,
            null,
            -> null
        }
        return attributes?.let { arrayOf(it) } ?: emptyArray()
    }
}
