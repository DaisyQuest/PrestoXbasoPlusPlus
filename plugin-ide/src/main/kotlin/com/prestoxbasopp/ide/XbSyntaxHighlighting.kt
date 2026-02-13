package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.lexer.XbTokenType

enum class XbHighlightStyle {
    KEYWORD,
    IDENTIFIER,
    FUNCTION_DECLARATION,
    FUNCTION_CALL,
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
    private val classifier = XbSemanticTokenClassifier()
    fun highlight(source: String): List<XbHighlightSpan> {
        val lexer = XbLexer()
        val tokens = lexer.lex(source).tokens.filter { it.type != XbTokenType.EOF }
        val styles = classifier.classify(tokens)
        return tokens.zip(styles)
            .map { (token, style) ->
                XbHighlightSpan(
                    textRange = token.range,
                    style = style,
                )
            }
    }
}

internal class XbSemanticTokenClassifier {
    fun classify(tokens: List<XbToken>): List<XbHighlightStyle> {
        val styles = mutableListOf<XbHighlightStyle>()
        var expectDeclarationIdentifier = false

        tokens.forEachIndexed { index, token ->
            val style = when {
                token.type == XbTokenType.PREPROCESSOR && isMacroDefinitionDirective(token.text) ->
                    XbHighlightStyle.MACRO_DEFINITION
                expectDeclarationIdentifier && token.type == XbTokenType.IDENTIFIER ->
                    XbHighlightStyle.FUNCTION_DECLARATION
                token.type == XbTokenType.IDENTIFIER && isFunctionCallIdentifier(tokens, index) ->
                    XbHighlightStyle.FUNCTION_CALL
                else -> defaultStyleFor(token)
            }
            styles += style

            expectDeclarationIdentifier = when {
                token.type == XbTokenType.KEYWORD && DECLARATION_KEYWORDS.contains(token.text.lowercase()) -> true
                expectDeclarationIdentifier && token.type == XbTokenType.IDENTIFIER -> false
                expectDeclarationIdentifier -> false
                else -> false
            }
        }

        return styles
    }

    private fun isFunctionCallIdentifier(tokens: List<XbToken>, index: Int): Boolean {
        if (tokens[index].type != XbTokenType.IDENTIFIER) {
            return false
        }
        val nextToken = tokens.getOrNull(index + 1) ?: return false
        return nextToken.type == XbTokenType.PUNCTUATION && nextToken.text == "("
    }

    private fun defaultStyleFor(token: XbToken): XbHighlightStyle {
        return when (token.type) {
            XbTokenType.KEYWORD -> XbHighlightStyle.KEYWORD
            XbTokenType.IDENTIFIER -> XbHighlightStyle.IDENTIFIER
            XbTokenType.NUMBER -> XbHighlightStyle.NUMBER
            XbTokenType.STRING -> XbHighlightStyle.STRING
            XbTokenType.DATE -> XbHighlightStyle.DATE
            XbTokenType.SYMBOL -> XbHighlightStyle.SYMBOL
            XbTokenType.CODEBLOCK -> XbHighlightStyle.CODEBLOCK
            XbTokenType.PREPROCESSOR -> XbHighlightStyle.PREPROCESSOR
            XbTokenType.OPERATOR -> XbHighlightStyle.OPERATOR
            XbTokenType.PUNCTUATION -> XbHighlightStyle.PUNCTUATION
            XbTokenType.COMMENT -> XbHighlightStyle.COMMENT
            XbTokenType.UNKNOWN -> XbHighlightStyle.ERROR
            XbTokenType.EOF -> error("EOF should be filtered before classification")
        }
    }

    private companion object {
        val DECLARATION_KEYWORDS: Set<String> = setOf("function", "procedure")
    }
}
