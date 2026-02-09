package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLexerTokenizationTest {
    @Test
    fun `lexes keywords identifiers literals and operators`() {
        val source = """
            function Test()
                #define FOO 1
                local x := 10
                local y := 3.14e-2
                local z := 0x1F
                local s := "hi\\n"
                local t := 'ok'
                local d := {^2024-01-02}
                local sym := #name
                local cb := {|a| a + 1|}
                // comment
                /* block */
                return x + y
            end
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.directives).hasSize(1)
        assertThat(result.directives.first().name).isEqualTo("define")

        val tokens = result.tokens.filter { it.type != XbTokenType.EOF }
        assertThat(tokens).anyMatch { it.type == XbTokenType.PREPROCESSOR && it.text == "#define FOO 1" }
        assertThat(tokens.first().type).isEqualTo(XbTokenType.KEYWORD)
        assertThat(tokens.first().text).isEqualTo("function")
        assertThat(tokens).anyMatch { it.type == XbTokenType.IDENTIFIER && it.text == "Test" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == "10" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == "3.14e-2" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == "0x1F" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "\"hi\\\\n\"" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "'ok'" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.DATE && it.text == "{^2024-01-02}" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.SYMBOL && it.text == "#name" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.CODEBLOCK && it.text == "{|a| a + 1|}" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text.startsWith("//") }
        assertThat(tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text.startsWith("/*") }
        assertThat(tokens).anyMatch { it.type == XbTokenType.KEYWORD && it.text == "return" }
    }

    @Test
    fun `keywords are case insensitive`() {
        val source = "IF x == 1\nendif"
        val result = XbLexer().lex(source)

        val keywordTexts = result.tokens.filter { it.type == XbTokenType.KEYWORD }.map { it.text }
        assertThat(keywordTexts).containsExactly("IF", "endif")
    }

    @Test
    fun `wait and exit are treated as keywords`() {
        val source = "WAIT EXIT"
        val result = XbLexer().lex(source)

        val keywordTexts = result.tokens.filter { it.type == XbTokenType.KEYWORD }.map { it.text }
        assertThat(keywordTexts).contains("WAIT", "EXIT")
    }

    @Test
    fun `handles doubled quotes inside strings without swallowing comments`() {
        val source = """
            local a := "line3 with quotes "" inside"
            local b := 'x''y'
            local c := "x""y" // comment after string
        """.trimIndent()

        val result = XbLexer().lex(source)
        val tokens = result.tokens.filter { it.type != XbTokenType.EOF }

        assertThat(tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "\"line3 with quotes \"\" inside\"" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "'x''y'" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "\"x\"\"y\"" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text.trim() == "// comment after string" }
    }
}
