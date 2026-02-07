package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.lexer.XbTokenType

enum class XbHighlightStyle {
    KEYWORD,
    IDENTIFIER,
    NUMBER,
    STRING,
    DATE,
    SYMBOL,
    CODEBLOCK,
    PREPROCESSOR,
    MACRO_DEFINITION,
    OPERATOR,
    PUNCTUATION,
    COMMENT,
    ERROR,
}

data class XbHighlightSpan(
    val textRange: XbTextRange,
    val style: XbHighlightStyle,
)

class XbSyntaxHighlighter {
    fun highlight(source: String): List<XbHighlightSpan> {
        val lexer = XbLexer()
        return lexer.lex(source).tokens
            .asSequence()
            .filter { it.type != XbTokenType.EOF }
            .mapNotNull { token ->
                val style = styleFor(token) ?: return@mapNotNull null
                XbHighlightSpan(
                    textRange = token.range,
                    style = style,
                )
            }
            .toList()
    }

    private fun styleFor(token: XbToken): XbHighlightStyle? {
        return when (token.type) {
            XbTokenType.KEYWORD -> XbHighlightStyle.KEYWORD
            XbTokenType.IDENTIFIER -> XbHighlightStyle.IDENTIFIER
            XbTokenType.NUMBER -> XbHighlightStyle.NUMBER
            XbTokenType.STRING -> XbHighlightStyle.STRING
            XbTokenType.DATE -> XbHighlightStyle.DATE
            XbTokenType.SYMBOL -> XbHighlightStyle.SYMBOL
            XbTokenType.CODEBLOCK -> XbHighlightStyle.CODEBLOCK
            XbTokenType.PREPROCESSOR -> if (isMacroDefinitionDirective(token.text)) {
                XbHighlightStyle.MACRO_DEFINITION
            } else {
                XbHighlightStyle.PREPROCESSOR
            }
            XbTokenType.OPERATOR -> XbHighlightStyle.OPERATOR
            XbTokenType.PUNCTUATION -> XbHighlightStyle.PUNCTUATION
            XbTokenType.COMMENT -> XbHighlightStyle.COMMENT
            XbTokenType.UNKNOWN -> XbHighlightStyle.ERROR
            XbTokenType.EOF -> null
        }
    }
}
