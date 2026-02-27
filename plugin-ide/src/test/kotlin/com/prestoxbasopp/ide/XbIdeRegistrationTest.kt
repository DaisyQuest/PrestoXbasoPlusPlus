package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.ide.dbf.DbfFileType
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
    fun `dbf file type declares expected metadata`() {
        assertThat(DbfFileType.name).isEqualTo("DBF File")
        assertThat(DbfFileType.description).isEqualTo("dBASE database file")
        assertThat(DbfFileType.defaultExtension).isEqualTo("dbf")
        assertThat(DbfFileType.icon).isNull()
    }


    @Test
    fun `plugin xml registers xpj file type name consistently`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val xpjFileType =
            Regex(
                """<fileType\s+[^>]*name="([^"]+)"[^>]*extensions="xpj"[^>]*implementationClass="com\.prestoxbasopp\.ide\.xpj\.XpjFileType"""",
            ).find(pluginXml)

        assertThat(xpjFileType).isNotNull
        assertThat(xpjFileType!!.groupValues[1]).isEqualTo("XPJ Project")
        val xpjLanguage =
            Regex(
                """<fileType\s+[^>]*name="XPJ Project"[^>]*language="([^"]+)"""",
            ).find(pluginXml)?.groupValues?.get(1)
        assertThat(xpjLanguage).isEqualTo("TEXT")
    }

    @Test
    fun `plugin xml registers dbf file association and editor provider`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val dbfFileType =
            Regex(
                """<fileType\s+[^>]*name=\"DBF File\"[^>]*extensions=\"([^\"]+)\"[^>]*implementationClass=\"([^\"]+)\"""",
            ).find(pluginXml)
        val editorProviderClass =
            Regex("""<fileEditorProvider\s+[^>]*implementation=\"([^\"]+)\"""")
                .findAll(pluginXml)
                .map { it.groupValues[1] }
                .toList()

        assertThat(dbfFileType).isNotNull
        assertThat(dbfFileType!!.groupValues[1]).isEqualTo("dbf")
        assertThat(dbfFileType.groupValues[2]).isEqualTo("com.prestoxbasopp.ide.dbf.DbfFileType")
        assertThat(editorProviderClass).contains("com.prestoxbasopp.ide.dbf.DbfFileEditorProvider")
    }

    @Test
    fun `plugin xml keeps reverse engineering action broadly accessible`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val actionBlock =
            Regex(
                """<action\s+[^>]*id=\"com\.prestoxbasopp\.ide\.dbf\.ReverseEngineerDbfAction\"[\s\S]*?</action>""",
            ).find(pluginXml)

        assertThat(actionBlock).isNotNull
        val groups =
            Regex("""<add-to-group\s+[^>]*group-id=\"([^\"]+)\"""")
                .findAll(actionBlock!!.value)
                .map { it.groupValues[1] }
                .toList()

        assertThat(groups).contains("ToolsMenu", "ProjectViewPopupMenu", "EditorPopupMenu")
    }

    @Test
    fun `plugin xml registers inspections without duplicate annotators`() {
        val pluginXmlUrl: URL? = javaClass.classLoader.getResource("META-INF/plugin.xml")
        assertThat(pluginXmlUrl).withFailMessage("Expected plugin.xml to be on the test classpath.").isNotNull()

        val pluginXml = pluginXmlUrl!!.readText()
        val annotators =
            Regex("""<annotator\s+[^>]*implementationClass="([^"]+)"""")
                .findAll(pluginXml)
                .map { it.groupValues[1] }
                .toList()
        val inspectionClass =
            Regex("""<localInspection\s+[^>]*implementationClass="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)
        val completionClass =
            Regex("""<completion\.contributor\s+[^>]*implementationClass="([^"]+)"""")
                .find(pluginXml)
                ?.groupValues
                ?.get(1)

        assertThat(annotators).isEmpty()
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
    fun `inspection tool provides display and group names`() {
        val inspectionTool = XbInspectionTool()

        assertThat(inspectionTool.displayName).isEqualTo("Xbase++ Inspection")
        assertThat(inspectionTool.groupDisplayName).isEqualTo("Xbase++")
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
    fun `syntax highlighter maps known style token categories to attributes`() {
        val highlighter = XbSyntaxHighlighterAdapter()

        val keyword = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD))
        val identifier = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.IDENTIFIER))
        val preprocessor = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.PREPROCESSOR))
        val macroDefinition = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.MACRO_DEFINITION))
        val functionDeclaration = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.FUNCTION_DECLARATION))
        val functionCall = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.FUNCTION_CALL))
        val error = highlighter.getTokenHighlights(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.ERROR))

        assertThat(keyword).containsExactly(XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.getValue(com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD))
        assertThat(identifier).containsExactly(XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.getValue(com.prestoxbasopp.ui.XbHighlightCategory.IDENTIFIER))
        assertThat(preprocessor).containsExactly(XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.getValue(com.prestoxbasopp.ui.XbHighlightCategory.PREPROCESSOR))
        assertThat(macroDefinition).containsExactly(XbSyntaxHighlighterAdapter.MACRO_DEFINITION)
        assertThat(functionDeclaration).containsExactly(XbSyntaxHighlighterAdapter.FUNCTION_DECLARATION)
        assertThat(functionCall).containsExactly(XbSyntaxHighlighterAdapter.FUNCTION_CALL)
        assertThat(error).containsExactly(XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.getValue(com.prestoxbasopp.ui.XbHighlightCategory.ERROR))
    }

    @Test
    fun `syntax highlighter ignores unknown token types`() {
        val highlighter = XbSyntaxHighlighterAdapter()
        val unknown = highlighter.getTokenHighlights(XbTokenType("NOT_A_TOKEN"))
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `plugin xml registers color settings page and prg support`() {
        val pluginXml = javaClass.classLoader.getResource("META-INF/plugin.xml")!!.readText()
        val colorPage = Regex("""<colorSettingsPage\s+[^>]*implementation="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)
        val extensions = Regex("""<fileType\s+[^>]*name="Xbase\+\+ File"[^>]*extensions="([^"]+)"""").find(pluginXml)?.groupValues?.get(1)

        assertThat(colorPage).isEqualTo("com.prestoxbasopp.ide.XbColorSettingsPage")
        assertThat(extensions).isNotNull
        assertThat(extensions!!.split(';')).contains("prg", "ch")
    }

    @Test
    fun `lexer adapter reports offsets relative to the original buffer`() {
        val lexer = XbLexerAdapter()
        val buffer = "xx if"
        lexer.start(buffer, 3, buffer.length, 0)

        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD))
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

        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD))
        assertThat(lexer.tokenStart).isEqualTo(0)
        assertThat(lexer.tokenEnd).isEqualTo(2)

        lexer.advance()
        assertThat(lexer.tokenType).isEqualTo(TokenType.WHITE_SPACE)
        assertThat(lexer.tokenStart).isEqualTo(2)
        assertThat(lexer.tokenEnd).isEqualTo(4)

        lexer.advance()
        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD))
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
        val buffer = """
            #define FOO 1
            function Build()
            return foo(1)
            #include "defs.ch"
        """.trimIndent()
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
            XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.MACRO_DEFINITION),
            XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.FUNCTION_DECLARATION),
            XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.FUNCTION_CALL),
            XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.PREPROCESSOR),
        )
    }

    @Test
    fun `lexer adapter applies manual override and style remapping preferences`() {
        val provider = object : XbHighlightingPreferencesProvider {
            override fun load() = com.prestoxbasopp.ui.XbHighlightingPreferences(
                styleMappings = com.prestoxbasopp.ui.XbHighlightCategory.entries.associateWith {
                    if (it == com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD) com.prestoxbasopp.ui.XbHighlightCategory.STRING else it
                },
                wordOverrides = mapOf("foo" to com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD),
            )
        }
        val lexer = XbLexerAdapter(provider)
        val buffer = "foo bar"

        lexer.start(buffer, 0, buffer.length, 0)

        assertThat(lexer.tokenType).isEqualTo(XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.STRING))
    }


    @Test
    fun `parser mode preserves core token types for comments and strings`() {
        val provider = object : XbHighlightingPreferencesProvider {
            override fun load() = com.prestoxbasopp.ui.XbHighlightingPreferences(
                styleMappings = com.prestoxbasopp.ui.XbHighlightCategory.entries.associateWith { com.prestoxbasopp.ui.XbHighlightCategory.ERROR },
                wordOverrides = mapOf("comment" to com.prestoxbasopp.ui.XbHighlightCategory.KEYWORD),
            )
        }
        val lexer = XbLexerAdapter(provider, XbLexerAdapter.Mode.PARSING)
        val buffer = """
            // note
            local s := "value"
        """.trimIndent()
        lexer.start(buffer, 0, buffer.length, 0)

        val seen = mutableListOf<IElementType>()
        while (lexer.tokenType != null) {
            lexer.tokenType?.let { seen += it }
            lexer.advance()
        }

        assertThat(seen).contains(
            XbHighlighterTokenSet.forToken(CoreTokenType.COMMENT),
            XbHighlighterTokenSet.forToken(CoreTokenType.STRING),
        )
        assertThat(seen).doesNotContain(
            XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.ERROR),
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
            if (lexer.tokenType == XbHighlighterTokenSet.forHighlightCategory(com.prestoxbasopp.ui.XbHighlightCategory.OPERATOR) &&
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
