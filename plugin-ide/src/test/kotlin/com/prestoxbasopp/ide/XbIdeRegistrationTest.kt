package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
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
    fun `plugin xml registers annotator and inspections`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val annotatorClass =
            Regex("""<annotator\s+[^>]*implementationClass="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)
        val inspectionClass =
            Regex("""<localInspection\s+[^>]*implementationClass="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)
        val completionClass =
            Regex("""<completion\.contributor\s+[^>]*implementationClass="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(annotatorClass).isEqualTo("com.prestoxbasopp.ide.XbDiagnosticsAnnotator")
        assertThat(inspectionClass).isEqualTo("com.prestoxbasopp.ide.XbInspectionTool")
        assertThat(completionClass).isEqualTo("com.prestoxbasopp.ide.XbCompletionContributor")
    }

    @Test
    fun `plugin xml registers folding, formatting, and rename handlers`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val foldingBuilderClass =
            Regex("""<lang\.foldingBuilder\s+[^>]*implementationClass="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val formattingServiceClass =
            Regex("""<formattingService\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val renameHandlerClass =
            Regex("""<renameHandler\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val editorListenerClass =
            Regex("""<editorFactoryListener\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(foldingBuilderClass).isEqualTo("com.prestoxbasopp.ide.XbFoldingBuilder")
        assertThat(formattingServiceClass).isEqualTo("com.prestoxbasopp.ide.XbFormattingService")
        assertThat(renameHandlerClass).isEqualTo("com.prestoxbasopp.ide.XbRenameHandler")
        assertThat(editorListenerClass).isEqualTo("com.prestoxbasopp.ide.XbIfBlockHighlighter")
    }

    @Test
    fun `inspection tool provides a display name`() {
        val inspectionTool = XbInspectionTool()

        assertThat(inspectionTool.displayName).isEqualTo("Xbase++ Inspection")
    }

    @Test
    fun `plugin xml registers settings configurable`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val configurableClass =
            Regex("""<applicationConfigurable\s+[^>]*instance="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val configurableId =
            Regex("""<applicationConfigurable\s+[^>]*id="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val configurableName =
            Regex("""<applicationConfigurable\s+[^>]*displayName="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(configurableClass).isEqualTo("com.prestoxbasopp.ide.XbSettingsConfigurable")
        assertThat(configurableId).isEqualTo("com.prestoxbasopp.settings")
        assertThat(configurableName).isEqualTo("xbase++ Settings")
    }

    @Test
    fun `plugin xml registers code style settings provider`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val providerClass =
            Regex("""<langCodeStyleSettingsProvider\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(providerClass).isEqualTo("com.prestoxbasopp.ide.XbLanguageCodeStyleSettingsProvider")
    }

    @Test
    fun `plugin xml registers module type and startup activity`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val moduleTypeId =
            Regex("""<moduleType\s+[^>]*id="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val moduleTypeClass =
            Regex("""<moduleType\s+[^>]*implementationClass="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)
        val startupActivity =
            Regex("""<startupActivity\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(moduleTypeId).isEqualTo("XBASEPP_MODULE")
        assertThat(moduleTypeClass).isEqualTo("com.prestoxbasopp.ide.modules.XbModuleType")
        assertThat(startupActivity).isEqualTo("com.prestoxbasopp.ide.modules.XbModuleStartupActivity")
    }


    @Test
    fun `plugin xml registers xbase debugger run configuration type`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val configurationTypeClass =
            Regex("""<configurationType\s+[^>]*implementation="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(configurationTypeClass).isEqualTo("com.prestoxbasopp.ide.debug.XbDebuggerRunConfigurationType")
    }

    @Test
    fun `syntax highlighter maps known tokens to attributes`() {
        val highlighter = XbSyntaxHighlighterAdapter()

        val keyword = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.KEYWORD))
        val identifier = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.IDENTIFIER))
        val preprocessor = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.PREPROCESSOR))
        val macroDefinition = highlighter.getTokenHighlights(XbHighlighterTokenSet.MACRO_DEFINITION)
        val functionDeclaration = highlighter.getTokenHighlights(XbHighlighterTokenSet.FUNCTION_DECLARATION)
        val functionCall = highlighter.getTokenHighlights(XbHighlighterTokenSet.FUNCTION_CALL)
        val error = highlighter.getTokenHighlights(XbHighlighterTokenSet.forToken(CoreTokenType.UNKNOWN))

        assertThat(keyword).containsExactly(DefaultLanguageHighlighterColors.KEYWORD)
        assertThat(identifier).containsExactly(DefaultLanguageHighlighterColors.IDENTIFIER)
        assertThat(preprocessor).containsExactly(DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
        assertThat(macroDefinition).containsExactly(XbSyntaxHighlighterAdapter.MACRO_DEFINITION)
        assertThat(functionDeclaration).containsExactly(XbSyntaxHighlighterAdapter.FUNCTION_DECLARATION)
        assertThat(functionCall).containsExactly(XbSyntaxHighlighterAdapter.FUNCTION_CALL)
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
    fun `lexer adapter uses semantic token types for directives and functions`() {
        val lexer = XbLexerAdapter()
        val buffer = "#define FOO 1\nfunction Build()\nreturn foo(1)\n#include \"defs.ch\""
        lexer.start(buffer, 0, buffer.length, 0)

        val significantTokens = mutableListOf<IElementType>()
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            if (tokenType != null && tokenType != TokenType.WHITE_SPACE) {
                significantTokens += tokenType
            }
            lexer.advance()
        }

        assertThat(significantTokens).contains(
            XbHighlighterTokenSet.MACRO_DEFINITION,
            XbHighlighterTokenSet.FUNCTION_DECLARATION,
            XbHighlighterTokenSet.FUNCTION_CALL,
            XbHighlighterTokenSet.forToken(CoreTokenType.PREPROCESSOR),
        )
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

    @Test
    fun `lexer adapter handles very large buffers and retains slash operators`() {
        val lexer = XbLexerAdapter()
        val body = buildString {
            repeat(10_000) { index ->
                append("value")
                append(index)
                append(" := ")
                append(index + 10)
                append(" / 2\n")
            }
        }
        lexer.start(body, 0, body.length, 0)

        var slashTokenCount = 0
        var sawAnyToken = false
        while (lexer.tokenType != null) {
            sawAnyToken = true
            if (lexer.tokenType == XbHighlighterTokenSet.forToken(CoreTokenType.OPERATOR) &&
                body.substring(lexer.tokenStart, lexer.tokenEnd) == "/"
            ) {
                slashTokenCount++
            }
            lexer.advance()
        }

        assertThat(sawAnyToken).isTrue()
        assertThat(slashTokenCount).isEqualTo(10_000)
    }

}
