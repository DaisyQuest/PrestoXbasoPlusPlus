package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType

class XbSyntaxHighlighterAdapter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = XbLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val attributes = when (tokenType) {
            XbHighlighterTokenSet.MACRO_DEFINITION -> MACRO_DEFINITION
            XbHighlighterTokenSet.FUNCTION_DECLARATION -> FUNCTION_DECLARATION
            XbHighlighterTokenSet.FUNCTION_CALL -> FUNCTION_CALL
            else -> {
                val tokenName = tokenType.toString()
                val token = runCatching { XbLexerTokenType.valueOf(tokenName) }.getOrNull()
                when (token) {
                    XbLexerTokenType.KEYWORD -> DefaultLanguageHighlighterColors.KEYWORD
                    XbLexerTokenType.IDENTIFIER -> DefaultLanguageHighlighterColors.IDENTIFIER
                    XbLexerTokenType.NUMBER -> DefaultLanguageHighlighterColors.NUMBER
                    XbLexerTokenType.STRING -> DefaultLanguageHighlighterColors.STRING
                    XbLexerTokenType.DATE -> DefaultLanguageHighlighterColors.CONSTANT
                    XbLexerTokenType.SYMBOL -> DefaultLanguageHighlighterColors.METADATA
                    XbLexerTokenType.CODEBLOCK -> DefaultLanguageHighlighterColors.STRING
                    XbLexerTokenType.PREPROCESSOR -> DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL
                    XbLexerTokenType.OPERATOR -> DefaultLanguageHighlighterColors.OPERATION_SIGN
                    XbLexerTokenType.PUNCTUATION -> DefaultLanguageHighlighterColors.PARENTHESES
                    XbLexerTokenType.COMMENT -> DefaultLanguageHighlighterColors.LINE_COMMENT
                    XbLexerTokenType.UNKNOWN -> DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
                    XbLexerTokenType.EOF,
                    null,
                    -> null
                }
            }
        }
        return attributes?.let { arrayOf(it) } ?: emptyArray()
    }

    companion object {
        internal val MACRO_DEFINITION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "XBASEPP_MACRO_DEFINITION",
            DefaultLanguageHighlighterColors.METADATA,
        )
        internal val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "XBASEPP_FUNCTION_DECLARATION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
        )
        internal val FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "XBASEPP_FUNCTION_CALL",
            DefaultLanguageHighlighterColors.FUNCTION_CALL,
        )
    }
}
