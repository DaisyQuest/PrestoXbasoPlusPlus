package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.parser.TokenType

class XbSyntaxHighlighterAdapter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = XbLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val tokenName = tokenType.toString()
        val token = runCatching { TokenType.valueOf(tokenName) }.getOrNull()
        val attributes = when (token) {
            TokenType.IF,
            TokenType.THEN,
            TokenType.ELSE,
            TokenType.ENDIF,
            TokenType.WHILE,
            TokenType.DO,
            TokenType.ENDDO,
            TokenType.RETURN,
            TokenType.AND,
            TokenType.OR,
            TokenType.NOT,
            -> DefaultLanguageHighlighterColors.KEYWORD
            TokenType.IDENTIFIER -> DefaultLanguageHighlighterColors.IDENTIFIER
            TokenType.NUMBER -> DefaultLanguageHighlighterColors.NUMBER
            TokenType.STRING -> DefaultLanguageHighlighterColors.STRING
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH,
            TokenType.EQ,
            TokenType.NEQ,
            TokenType.LT,
            TokenType.LTE,
            TokenType.GT,
            TokenType.GTE,
            -> DefaultLanguageHighlighterColors.OPERATION_SIGN
            TokenType.LPAREN,
            TokenType.RPAREN,
            TokenType.SEMICOLON,
            TokenType.COMMA,
            -> DefaultLanguageHighlighterColors.PARENTHESES
            TokenType.ERROR -> DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
            TokenType.EOF,
            null,
            -> null
        }
        return attributes?.let { arrayOf(it) } ?: emptyArray()
    }
}
