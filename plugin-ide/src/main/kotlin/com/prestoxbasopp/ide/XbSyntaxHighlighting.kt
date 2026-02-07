package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.parser.TokenType
import com.prestoxbasopp.core.parser.XbLexer

enum class XbHighlightStyle {
    KEYWORD,
    IDENTIFIER,
    NUMBER,
    STRING,
    OPERATOR,
    PUNCTUATION,
    ERROR,
}

data class XbHighlightSpan(
    val textRange: XbTextRange,
    val style: XbHighlightStyle,
)

class XbSyntaxHighlighter {
    fun highlight(source: String): List<XbHighlightSpan> {
        val lexer = XbLexer(source)
        return lexer.lex()
            .asSequence()
            .filter { it.type != TokenType.EOF }
            .mapNotNull { token ->
                val style = styleFor(token.type) ?: return@mapNotNull null
                XbHighlightSpan(
                    textRange = XbTextRange(token.startOffset, token.endOffset),
                    style = style,
                )
            }
            .toList()
    }

    private fun styleFor(type: TokenType): XbHighlightStyle? {
        return when (type) {
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
            -> XbHighlightStyle.KEYWORD
            TokenType.IDENTIFIER -> XbHighlightStyle.IDENTIFIER
            TokenType.NUMBER -> XbHighlightStyle.NUMBER
            TokenType.STRING -> XbHighlightStyle.STRING
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
            -> XbHighlightStyle.OPERATOR
            TokenType.LPAREN,
            TokenType.RPAREN,
            TokenType.SEMICOLON,
            TokenType.COMMA,
            -> XbHighlightStyle.PUNCTUATION
            TokenType.ERROR -> XbHighlightStyle.ERROR
            TokenType.EOF -> null
        }
    }
}
