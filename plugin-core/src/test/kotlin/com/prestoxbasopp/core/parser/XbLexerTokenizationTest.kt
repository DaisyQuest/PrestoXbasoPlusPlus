package com.prestoxbasopp.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class XbLexerTokenizationTest {
    @Test
    fun `tokenizes hash as not-equal operator`() {
        val tokens = XbLexer("a # b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.NEQ,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("!=")
    }

    @Test
    fun `tokenizes at symbol for at-say-get statements`() {
        val tokens = XbLexer("@ 1, 2 SAY \"Hi\" GET value").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.AT,
            TokenType.NUMBER,
            TokenType.COMMA,
            TokenType.NUMBER,
            TokenType.SAY,
            TokenType.STRING,
            TokenType.GET,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens.first().lexeme).isEqualTo("@")
    }

    @Test
    fun `tokenizes colon for object method dispatch`() {
        val tokens = XbLexer("p_IniFile:ReadSectionValues()")
            .lex()
            .filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.COLON,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.RPAREN,
        )
    }

    @Test
    fun `reports error token for bare exclamation mark`() {
        val tokens = XbLexer("!").lex().filter { it.type != TokenType.EOF }

        val token = tokens.single()
        assertThat(token.type).isEqualTo(TokenType.ERROR)
        assertThat(token.lexeme).isEqualTo("!")
    }

    @Test
    fun `tokenizes single quoted string with backslash`() {
        val source = "cPath += '\\\\'"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.PLUS,
            TokenType.EQ,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("\\\\")
    }

    @Test
    fun `tokenizes hash not equal operator in conditional expression`() {
        val source = "IF valtype(soSatOK) # \"L\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IF,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.RPAREN,
            TokenType.NEQ,
            TokenType.STRING,
        )
        assertThat(tokens[5].lexeme).isEqualTo("!=")
    }

    @Test
    fun `tokenizes angle bracket not equal operator in conditional expression`() {
        val source = "IF right(cdxMCImportPath,1)<>'\\'"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IF,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.COMMA,
            TokenType.NUMBER,
            TokenType.RPAREN,
            TokenType.NEQ,
            TokenType.STRING,
        )
        assertThat(tokens[7].lexeme).isEqualTo("!=")
        assertThat(tokens[8].lexeme).isEqualTo("\\")
    }

    @Test
    fun `tokenizes single quoted backslash in reassignment statements`() {
        val source = "cdxMCImportPath += '\\\\'"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.PLUS,
            TokenType.EQ,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("\\\\")
    }

    @Test
    fun `tokenizes slash as division operator when not part of comment`() {
        val tokens = XbLexer("a / b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.SLASH,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("/")
    }

    @Test
    fun `skips slash comments without producing slash token`() {
        val tokens = XbLexer("a // trailing comment\n/ b").lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.SLASH,
            TokenType.IDENTIFIER,
        )
        assertThat(tokens[1].lexeme).isEqualTo("/")
    }

    @Test
    fun `returns error token for unterminated block comment that starts with slash`() {
        val token = XbLexer("/* unterminated").lex().first { it.type != TokenType.EOF }

        assertThat(token.type).isEqualTo(TokenType.ERROR)
        assertThat(token.lexeme).isEqualTo("/* unterminated")
    }

    @Test
    fun `tokenizes percent-equals into percent plus equals token pair`() {
        val source = "value %= 2"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.PERCENT,
            TokenType.EQ,
            TokenType.NUMBER,
        )
    }



    @Test
    fun `tokenizes quoted backslash literals in comparison and concatenation`() {
        val source = """IF cCleanString[len(cCleanString)]="\" ; cCleanString := cCleanString + "\" ; ENDIF"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens).anyMatch { it.type == TokenType.STRING && it.lexeme == "\\" }
        assertThat(tokens.count { it.type == TokenType.STRING && it.lexeme == "\\" }).isEqualTo(2)
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes double quoted trailing backslash as complete string literal`() {
        val source = "cCleanString := \"\\\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.ASSIGN,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("\\")
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes double quoted escaped quotes without terminating string`() {
        val source = "cText := \"hello \\\"world\\\"!\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.ASSIGN,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("hello \"world\"!")
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `treats trailing backslash without escaped quote as unterminated string`() {
        val source = "cText := \"C:\\\\"
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.ASSIGN,
            TokenType.ERROR,
        )
        assertThat(tokens.last().lexeme).isEqualTo("\"C:\\\\")
    }

    @Test
    fun `does not treat backslash quote as escape across line breaks`() {
        val source = "\"line1\\\"\nline2\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.STRING,
            TokenType.IDENTIFIER,
            TokenType.ERROR,
        )
    }

    @Test
    fun `keeps doubled quote escape behavior for regular double quoted strings`() {
        val source = "cText := \"a\"\"b\""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IDENTIFIER,
            TokenType.ASSIGN,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("a\"b")
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes backslash question mark inside single quoted string`() {
        val source = """IF cChar$'<>:"/|\?*'"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.map { it.type }).containsExactly(
            TokenType.IF,
            TokenType.IDENTIFIER,
            TokenType.CONTAINS,
            TokenType.STRING,
        )
        assertThat(tokens.last().lexeme).isEqualTo("<>:\"/|\\?*")
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes tilde characters inside caption strings`() {
        val source = """
            @ 6.5,23 DCPUSHBUTTON CAPTION "~CANCEL"
            @ 6.5,10 DCPUSHBUTTON CAPTION "~OK"
        """.trimIndent()
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens).anyMatch { it.type == TokenType.STRING && it.lexeme == "~CANCEL" }
        assertThat(tokens).anyMatch { it.type == TokenType.STRING && it.lexeme == "~OK" }
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes right comparison and append with single quoted backslash`() {
        val source = """IF right(cPath,1)=='\' ; cPath += '\' ; ENDIF"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens).anyMatch { it.type == TokenType.STRING && it.lexeme == "\\" }
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }

    @Test
    fun `tokenizes fexists windows path argument without unexpected slash tokens`() {
        val source = """fExists("C:\"+y2+"s"+x1+"Place\"+z3+".tmw")"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
        assertThat(tokens.filter { it.type == TokenType.STRING }.map { it.lexeme }).containsExactly(
            "C:\\",
            "s",
            "Place\\",
            ".tmw",
        )
    }


    @Test
    fun `lexes large source with many escaped quotes within timeout`() {
        val source = buildString {
            appendLine("FUNCTION Heavy()")
            repeat(5_000) { index ->
                appendLine("LOCAL c$index := \"value \\\"$index\\\"\"")
            }
            appendLine("RETURN NIL")
            appendLine("ENDFUNCTION")
        }

        val tokens = assertTimeoutPreemptively(Duration.ofSeconds(4)) {
            XbLexer(source).lex().filter { it.type != TokenType.EOF }
        }

        assertThat(tokens.count { it.type == TokenType.STRING }).isEqualTo(5_000)
        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
    }


    @Test
    fun `tokenizes bracket path literal with backslash as string`() {
        val source = """File2SEi(rootPath()+datExport,[data\],"NO")"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
        assertThat(tokens).anyMatch { it.type == TokenType.STRING && it.lexeme == "data\\" }
    }

    @Test
    fun `tokenizes bracket literal containing only a backslash as string`() {
        val source = """IF lAddBS .AND. Rat([\],cTemp)<>len(cTemp) ; cTemp := cTemp+[\] ; ENDIF"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
        assertThat(tokens.filter { it.type == TokenType.STRING }.map { it.lexeme }).containsExactly("\\", "\\")
    }



    @Test
    fun `does not coerce bracket expression without backslash into string literal`() {
        val source = """Rat([abc], cTemp)"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens.none { it.type == TokenType.ERROR }).isTrue()
        assertThat(tokens.map { it.type }).containsSequence(
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.LBRACKET,
            TokenType.IDENTIFIER,
            TokenType.RBRACKET,
            TokenType.COMMA,
            TokenType.IDENTIFIER,
            TokenType.RPAREN,
        )
    }

    @Test
    fun `does not coerce multiline bracket literal with backslash into string literal`() {
        val source = """memo := [line1\
line2]"""
        val tokens = XbLexer(source).lex().filter { it.type != TokenType.EOF }

        assertThat(tokens).anyMatch { it.type == TokenType.ERROR && it.lexeme == "\\" }
        assertThat(tokens.none { it.type == TokenType.STRING && it.lexeme.contains("line1") }).isTrue()
        assertThat(tokens).anyMatch { it.type == TokenType.LBRACKET }
    }

}
