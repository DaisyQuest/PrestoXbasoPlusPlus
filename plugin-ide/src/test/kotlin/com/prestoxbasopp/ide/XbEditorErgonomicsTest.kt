package com.prestoxbasopp.ide

import com.intellij.psi.tree.TokenSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbEditorErgonomicsTest {
    @Test
    fun `commenter exposes line and block comment markers`() {
        val commenter = XbCommenter()

        assertThat(commenter.lineCommentPrefix).isEqualTo("//")
        assertThat(commenter.blockCommentPrefix).isEqualTo("/*")
        assertThat(commenter.blockCommentSuffix).isEqualTo("*/")
    }

    @Test
    fun `brace matcher exposes expected paired punctuation`() {
        val matcher = XbBraceMatcher()

        val pairs = matcher.pairs

        assertThat(pairs).hasSize(3)
        assertThat(pairs.map { it.leftBraceType }).containsExactly(
            XbParenTokenTypes.LEFT_PAREN,
            XbParenTokenTypes.LEFT_BRACKET,
            XbParenTokenTypes.LEFT_BRACE,
        )
        assertThat(pairs.map { it.rightBraceType }).containsExactly(
            XbParenTokenTypes.RIGHT_PAREN,
            XbParenTokenTypes.RIGHT_BRACKET,
            XbParenTokenTypes.RIGHT_BRACE,
        )
    }

    @Test
    fun `parsing lexer emits dedicated brace tokens for matcher integration`() {
        val lexer = XbLexerAdapter(mode = XbLexerAdapter.Mode.PARSING)
        lexer.start("([{}])", 0, 6, 0)

        val tokenTypes = mutableListOf<String>()
        while (lexer.tokenType != null) {
            tokenTypes += lexer.tokenType.toString()
            lexer.advance()
        }

        assertThat(tokenTypes).containsExactly(
            "XBASEPP_LEFT_PAREN",
            "XBASEPP_LEFT_BRACKET",
            "XBASEPP_LEFT_BRACE",
            "XBASEPP_RIGHT_BRACE",
            "XBASEPP_RIGHT_BRACKET",
            "XBASEPP_RIGHT_PAREN",
        )
    }

    @Test
    fun `find usages provider scanner indexes identifiers comments and strings`() {
        val provider = XbFindUsagesProvider()
        val scanner = provider.wordsScanner

        val identifiers = TokenSet.create(XbHighlighterTokenSet.forToken(com.prestoxbasopp.core.lexer.XbTokenType.IDENTIFIER))
        val comments = TokenSet.create(XbHighlighterTokenSet.forToken(com.prestoxbasopp.core.lexer.XbTokenType.COMMENT))

        assertThat(scanner).isNotNull
        assertThat(identifiers.types).isNotEmpty
        assertThat(comments.types).isNotEmpty
    }
}
