package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class XbLexerFuzzTest {
    @Test
    fun `lexer does not throw on random inputs`() {
        val random = Random(42)
        val lexer = XbLexer()
        repeat(200) {
            val source = XbLexerFuzzing.randomSource(random, length = 50)
            val result = lexer.lex(source)
            assertThat(result.tokens).isNotEmpty
            assertThat(result.filteredSource.length).isLessThanOrEqualTo(source.length)
        }
    }
}
