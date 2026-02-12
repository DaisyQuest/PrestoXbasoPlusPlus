package com.prestoxbasopp.core.lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class XbLexerEdgeCaseTest {
    @Test
    fun `reports unterminated block comments`() {
        val source = "/* unterminated"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unterminated block comment")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.COMMENT && it.text == source }
    }

    @Test
    fun `reports unexpected characters`() {
        val source = "~"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unexpected character '~'")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == "~" }
    }

    @Test
    fun `lexes dot edge cases for numbers and punctuation`() {
        val source = "a . b .5 1."
        val result = XbLexer().lex(source)

        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "." }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == ".5" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.NUMBER && it.text == "1." }
    }

    @Test
    fun `accepts escaped quotes inside strings`() {
        val source = "\"hello \\\"world\\\"\""
        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == source }
    }

    @Test
    fun `reports invalid date formats`() {
        val source = "{^2024-1-02}"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Invalid date literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.UNKNOWN && it.text == source }
    }

    @Test
    fun `lexes multi character operators`() {
        val source = "a==b != c <= d >= e && f || g :: h -> i"
        val result = XbLexer().lex(source)

        val operatorTexts = result.tokens.filter { it.type == XbTokenType.OPERATOR }.map { it.text }
        assertThat(operatorTexts).contains("==", "!=", "<=", ">=", "&&", "||", "::", "->")
    }

    @Test
    fun `lexes macro operators and at say get tokens`() {
        val source = """
            h["key"] := "value"
            &("QOut")( "Hello " + &cMacro + "!" )
            k := Eval( { |n| IIF( n % 2 == 0, ;
                ( {|m| m*m } )(n), ;
                ( {|m| m+m } )(n) ) } )
            @ 5, 10 SAY "Enter:" GET cInput
            c := "x" $ "y"
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        val tokens = result.tokens.filter { it.type != XbTokenType.EOF }
        assertThat(tokens).noneMatch { it.type == XbTokenType.UNKNOWN }
        assertThat(tokens).anyMatch { it.type == XbTokenType.OPERATOR && it.text == "&" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.OPERATOR && it.text == "$" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "@" }
        assertThat(tokens).anyMatch { it.type == XbTokenType.CODEBLOCK }
    }

    @Test
    fun `accepts tilde inside both quote styles and single quoted backslash as complete string`() {
        val source = """
            @ 6.5,23 DCPUSHBUTTON CAPTION "~CANCEL"
            @ 6.5,10 DCPUSHBUTTON CAPTION '~OK'
            cPath += '\\'
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "\"~CANCEL\"" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "'~OK'" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "'\\\\'" }
    }



    @Test
    fun `lexes bracket backslash literal as string to avoid unexpected character errors`() {
        val source = """
            IF lAddBS .AND. Rat([\],cTemp)<>len(cTemp)
               cTemp := cTemp+[\]
            ENDIF
        """.trimIndent()

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        val stringTexts = result.tokens.filter { it.type == XbTokenType.STRING }.map { it.text }
        assertThat(stringTexts).containsExactly("[\\]", "[\\]")
    }

    @Test
    fun `keeps bracket expressions without backslash as punctuation tokens`() {
        val source = """Rat([abc], cTemp)"""

        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "[" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "]" }
        assertThat(result.tokens).noneMatch { it.type == XbTokenType.STRING && it.text == "[abc]" }
    }

    @Test
    fun `does not treat multiline bracket content with backslash as string literal`() {
        val source = """memo := [line1\
line2]"""

        val result = XbLexer().lex(source)

        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "[" }
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.PUNCTUATION && it.text == "]" }
        assertThat(result.tokens).noneMatch { it.type == XbTokenType.STRING && it.text.contains("line1") }
    }

    @Test
    fun `supports escaped double quotes inside double quoted strings`() {
        val source = "cText := \"hello \\\"world\\\"!\""
        val result = XbLexer().lex(source)

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.STRING && it.text == "\"hello \\\"world\\\"!\"" }
    }

    @Test
    fun `lexes large input with escaped double quotes within timeout`() {
        val source = buildString {
            appendLine("FUNCTION Heavy()")
            repeat(5_000) { index ->
                appendLine("LOCAL c$index := \"value \\\"$index\\\"\"")
            }
            appendLine("RETURN NIL")
            appendLine("ENDFUNCTION")
        }

        val result = assertTimeoutPreemptively(Duration.ofSeconds(4)) {
            XbLexer().lex(source)
        }

        assertThat(result.errors).isEmpty()
        assertThat(result.tokens.count { it.type == XbTokenType.STRING }).isEqualTo(5_000)
    }


    @Test
    fun `reports unterminated date literals`() {
        val source = "{^2024-01-02"
        val result = XbLexer().lex(source)

        assertThat(result.errors.map { it.message }).contains("Unterminated date literal")
        assertThat(result.tokens).anyMatch { it.type == XbTokenType.DATE && it.text == source }
    }

    @Test
    fun `reports empty source without directives`() {
        val result = XbLexer().lex("")

        assertThat(result.tokens).containsExactly(XbToken(XbTokenType.EOF, "", result.tokens.first().range))
        assertThat(result.directives).isEmpty()
        assertThat(result.filteredSource).isEmpty()
    }
}
