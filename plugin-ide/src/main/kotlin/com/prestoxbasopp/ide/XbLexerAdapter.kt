package com.prestoxbasopp.ide

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.core.parser.Token
import com.prestoxbasopp.core.parser.TokenType
import com.prestoxbasopp.core.parser.XbLexer

class XbLexerAdapter : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var tokens: List<Token> = emptyList()
    private var tokenIndex: Int = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        val source = buffer.subSequence(startOffset, endOffset).toString()
        tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }
        tokenIndex = 0
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? {
        val token = tokens.getOrNull(tokenIndex) ?: return null
        return XbHighlighterTokenSet.forToken(token.type)
    }

    override fun getTokenStart(): Int {
        val token = tokens.getOrNull(tokenIndex) ?: return endOffset
        return startOffset + token.startOffset
    }

    override fun getTokenEnd(): Int {
        val token = tokens.getOrNull(tokenIndex) ?: return endOffset
        return startOffset + token.endOffset
    }

    override fun advance() {
        if (tokenIndex < tokens.size) {
            tokenIndex++
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset
}
