package com.prestoxbasopp.ide

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType
import com.prestoxbasopp.ui.XbHighlightCategory
import com.prestoxbasopp.ui.XbHighlightingPreferences

class XbLexerAdapter(
    private val preferencesProvider: XbHighlightingPreferencesProvider = XbHighlightingSettingsBridge,
    private val mode: Mode = Mode.HIGHLIGHTING,
) : LexerBase() {
    enum class Mode {
        HIGHLIGHTING,
        PARSING,
    }

    private data class LexerToken(val type: IElementType, val start: Int, val end: Int)

    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var tokens: List<LexerToken> = emptyList()
    private var tokenIndex: Int = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        val source = buffer.subSequence(startOffset, endOffset).toString()
        tokens = buildLexerTokens(source)
        tokenIndex = 0
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? {
        return tokens.getOrNull(tokenIndex)?.type
    }

    override fun getTokenStart(): Int {
        val token = tokens.getOrNull(tokenIndex) ?: return endOffset
        return startOffset + token.start
    }

    override fun getTokenEnd(): Int {
        val token = tokens.getOrNull(tokenIndex) ?: return endOffset
        return startOffset + token.end
    }

    override fun advance() {
        if (tokenIndex < tokens.size) {
            tokenIndex++
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun buildLexerTokens(source: String): List<LexerToken> {
        val coreTokens = XbLexer().lex(source).tokens.filter { it.type != XbLexerTokenType.EOF }
        if (coreTokens.isEmpty()) {
            return if (source.isEmpty()) {
                emptyList()
            } else {
                listOf(LexerToken(TokenType.WHITE_SPACE, 0, source.length))
            }
        }

        val tokenStyles = XbSemanticTokenClassifier().classify(coreTokens)
        val preferences = preferencesProvider.load().withNormalizedOverrides()
        val expanded = mutableListOf<LexerToken>()
        var cursor = 0

        for ((token, style) in coreTokens.zip(tokenStyles)) {
            val start = token.range.startOffset.coerceIn(cursor, source.length)
            val end = token.range.endOffset.coerceIn(start, source.length)
            if (start > cursor) {
                expanded += LexerToken(TokenType.WHITE_SPACE, cursor, start)
            }
            val elementType = elementTypeFor(token, style, preferences)
            if (end > start) {
                expanded += LexerToken(
                    elementType,
                    start,
                    end,
                )
                cursor = end
            }
        }
        if (cursor < source.length) {
            expanded += LexerToken(TokenType.WHITE_SPACE, cursor, source.length)
        }
        return expanded
    }

    private fun elementTypeFor(
        token: XbToken,
        style: XbHighlightStyle,
        preferences: XbHighlightingPreferences,
    ): IElementType {
        if (mode == Mode.PARSING) {
            return XbHighlighterTokenSet.forToken(token.type)
        }

        val baseCategory = style.toCategory()
        val overrideCategory = if (token.type == XbLexerTokenType.IDENTIFIER) {
            preferences.wordOverrides[token.text.lowercase()]
        } else {
            null
        }
        val mapped = preferences.styleMappings[overrideCategory ?: baseCategory] ?: (overrideCategory ?: baseCategory)
        return XbHighlighterTokenSet.forHighlightCategory(mapped)
    }

    private fun XbHighlightStyle.toCategory(): XbHighlightCategory {
        return when (this) {
            XbHighlightStyle.KEYWORD -> XbHighlightCategory.KEYWORD
            XbHighlightStyle.IDENTIFIER -> XbHighlightCategory.IDENTIFIER
            XbHighlightStyle.FUNCTION_DECLARATION -> XbHighlightCategory.FUNCTION_DECLARATION
            XbHighlightStyle.FUNCTION_CALL -> XbHighlightCategory.FUNCTION_CALL
            XbHighlightStyle.NUMBER -> XbHighlightCategory.NUMBER
            XbHighlightStyle.STRING -> XbHighlightCategory.STRING
            XbHighlightStyle.DATE -> XbHighlightCategory.DATE
            XbHighlightStyle.SYMBOL -> XbHighlightCategory.SYMBOL
            XbHighlightStyle.CODEBLOCK -> XbHighlightCategory.CODEBLOCK
            XbHighlightStyle.PREPROCESSOR -> XbHighlightCategory.PREPROCESSOR
            XbHighlightStyle.MACRO_DEFINITION -> XbHighlightCategory.MACRO_DEFINITION
            XbHighlightStyle.OPERATOR -> XbHighlightCategory.OPERATOR
            XbHighlightStyle.PUNCTUATION -> XbHighlightCategory.PUNCTUATION
            XbHighlightStyle.COMMENT -> XbHighlightCategory.COMMENT
            XbHighlightStyle.ERROR -> XbHighlightCategory.ERROR
        }
    }
}
