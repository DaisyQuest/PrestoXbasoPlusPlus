package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.TokenType
import com.prestoxbasopp.core.lexer.XbTokenType as CoreTokenType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class XbIdeRegistrationTest {
    @Test
    fun `file type declares expected metadata`() {
        assertThat(XbFileType.name).isEqualTo("Xbase++ File")
        assertThat(XbFileType.description).isEqualTo("Xbase++ source file")
        assertThat(XbFileType.defaultExtension).isEqualTo("xb")
        assertThat(XbFileType.icon).isNull()
    }

    @Test
    fun `plugin xml registers file type name consistently`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val fileTypeName =
            Regex("""<fileType\s+[^>]*name="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)
        val extensions =
            Regex("""<fileType\s+[^>]*extensions="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)

        assertThat(fileTypeName).isEqualTo(XbFileType.name)
        assertThat(extensions).isNotNull
        assertThat(extensions!!.split(';')).contains("ch")
    }

    @Test
    fun `syntax highlighter maps known tokens to attributes`() {
        val highlighter = XbSyntaxHighlighterAdapter()

        val keyword = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.KEYWORD))
        val identifier = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.IDENTIFIER))
        val preprocessor = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.PREPROCESSOR))
        val error = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.UNKNOWN))

        assertThat(keyword).containsExactly(DefaultLanguageHighlighterColors.KEYWORD)
        assertThat(identifier).containsExactly(DefaultLanguageHighlighterColors.IDENTIFIER)
        assertThat(preprocessor).containsExactly(DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
        assertThat(error).containsExactly(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
    }

    @Test
    fun `syntax highlighter ignores unknown token types`() {
        val highlighter = XbSyntaxHighlighterAdapter()
        val unknown = highlighter.getTokenHighlights(XbTokenType("NOT_A_TOKEN"))
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `lexer adapter reports offsets relative to the original buffer`() {
        val lexer = XbLexerAdapter()
        val buffer = "xx if"
        lexer.start(buffer, 3, buffer.length, 0)

        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forToken(CoreTokenType.KEYWORD))
        assertThat(lexer.tokenStart).isEqualTo(3)
        assertThat(lexer.tokenEnd).isEqualTo(5)

        lexer.advance()
        assertThat(lexer.tokenType).isNull()
        assertThat(lexer.tokenStart).isEqualTo(buffer.length)
        assertThat(lexer.tokenEnd).isEqualTo(buffer.length)
    }

    @Test
    fun `lexer adapter fills whitespace gaps between tokens`() {
        val lexer = XbLexerAdapter()
        val buffer = "if  return  "
        lexer.start(buffer, 0, buffer.length, 0)

        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forToken(CoreTokenType.KEYWORD))
        assertThat(lexer.tokenStart).isEqualTo(0)
        assertThat(lexer.tokenEnd).isEqualTo(2)

        lexer.advance()
        assertThat(lexer.tokenType).isEqualTo(TokenType.WHITE_SPACE)
        assertThat(lexer.tokenStart).isEqualTo(2)
        assertThat(lexer.tokenEnd).isEqualTo(4)

        lexer.advance()
        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forToken(CoreTokenType.KEYWORD))
        assertThat(lexer.tokenStart).isEqualTo(4)
        assertThat(lexer.tokenEnd).isEqualTo(10)

        lexer.advance()
        assertThat(lexer.tokenType).isEqualTo(TokenType.WHITE_SPACE)
        assertThat(lexer.tokenStart).isEqualTo(10)
        assertThat(lexer.tokenEnd).isEqualTo(buffer.length)
    }

    @Test
    fun `lexer adapter emits whitespace token when only spaces are present`() {
        val lexer = XbLexerAdapter()
        val buffer = "   "
        lexer.start(buffer, 0, buffer.length, 0)

        assertThat(lexer.tokenType).isEqualTo(TokenType.WHITE_SPACE)
        assertThat(lexer.tokenStart).isEqualTo(0)
        assertThat(lexer.tokenEnd).isEqualTo(buffer.length)

        lexer.advance()
        assertThat(lexer.tokenType).isNull()
    }
}
