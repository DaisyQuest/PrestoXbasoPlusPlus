package com.prestoxbasopp.ide

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbTokenType as XbLexerTokenType

class XbLexerAdapter : LexerBase() {
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

        val expanded = mutableListOf<LexerToken>()
        var cursor = 0
        for (token in coreTokens) {
            if (token.range.startOffset > cursor) {
                expanded += LexerToken(TokenType.WHITE_SPACE, cursor, token.range.startOffset)
            }
            expanded += LexerToken(
                XbHighlighterTokenSet.forToken(token.type),
                token.range.startOffset,
                token.range.endOffset,
            )
            cursor = token.range.endOffset
        }
        if (cursor < source.length) {
            expanded += LexerToken(TokenType.WHITE_SPACE, cursor, source.length)
        }
        return expanded
    }
}
