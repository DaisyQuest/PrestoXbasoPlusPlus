package com.prestoxbasopp.ide

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class XbColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon? = XbFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = XbSyntaxHighlighterAdapter()

    override fun getDemoText(): String {
        return """
            #define <MACRO_DEFINITION>ANSWER</MACRO_DEFINITION> <NUMBER>42</NUMBER>
            #include <STRING>"defs.ch"</STRING>
            function <FUNCTION_DECLARATION>BuildReport</FUNCTION_DECLARATION>()
                local <IDENTIFIER>dt</IDENTIFIER> := <DATE>{^2025-01-01}</DATE>
                local <IDENTIFIER>sym</IDENTIFIER> := <SYMBOL>#token</SYMBOL>
                local <IDENTIFIER>cb</IDENTIFIER> := <CODEBLOCK>{|x| x + 1}</CODEBLOCK>
                // <COMMENT>Generate output</COMMENT>
                return <FUNCTION_CALL>BuildReport</FUNCTION_CALL>(<NUMBER>1</NUMBER>) <OPERATOR>+</OPERATOR> <ERROR>0x</ERROR>
            end
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        return XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.mapKeys { it.key.name }
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return XbSyntaxHighlighterAdapter.KEYS_BY_CATEGORY.entries
            .map { AttributesDescriptor(it.key.name.replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase), it.value) }
            .toTypedArray()
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Xbase++"
}
