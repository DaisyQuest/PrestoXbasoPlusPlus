package com.prestoxbasopp.ide

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.prestoxbasopp.ui.XbHighlightCategory

class XbSyntaxHighlighterAdapter(
    private val preferencesProvider: XbHighlightingPreferencesProvider = XbHighlightingSettingsBridge,
) : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = XbLexerAdapter(preferencesProvider)

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val category = XbHighlighterTokenSet.tokenTypeForStyleElement(tokenType) ?: return emptyArray()
        return arrayOf(KEYS_BY_CATEGORY.getValue(category))
    }

    companion object {
        internal val KEYS_BY_CATEGORY: Map<XbHighlightCategory, TextAttributesKey> = mapOf(
            XbHighlightCategory.KEYWORD to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_KEYWORD",
                DefaultLanguageHighlighterColors.KEYWORD,
            ),
            XbHighlightCategory.IDENTIFIER to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_IDENTIFIER",
                DefaultLanguageHighlighterColors.IDENTIFIER,
            ),
            XbHighlightCategory.FUNCTION_DECLARATION to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_FUNCTION_DECLARATION",
                DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
            ),
            XbHighlightCategory.FUNCTION_CALL to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_FUNCTION_CALL",
                DefaultLanguageHighlighterColors.FUNCTION_CALL,
            ),
            XbHighlightCategory.NUMBER to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_NUMBER",
                DefaultLanguageHighlighterColors.NUMBER,
            ),
            XbHighlightCategory.STRING to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_STRING",
                DefaultLanguageHighlighterColors.STRING,
            ),
            XbHighlightCategory.DATE to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_DATE",
                DefaultLanguageHighlighterColors.CONSTANT,
            ),
            XbHighlightCategory.SYMBOL to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_SYMBOL",
                DefaultLanguageHighlighterColors.METADATA,
            ),
            XbHighlightCategory.CODEBLOCK to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_CODEBLOCK",
                DefaultLanguageHighlighterColors.STRING,
            ),
            XbHighlightCategory.PREPROCESSOR to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_PREPROCESSOR",
                DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL,
            ),
            XbHighlightCategory.MACRO_DEFINITION to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_MACRO_DEFINITION",
                DefaultLanguageHighlighterColors.METADATA,
            ),
            XbHighlightCategory.OPERATOR to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_OPERATOR",
                DefaultLanguageHighlighterColors.OPERATION_SIGN,
            ),
            XbHighlightCategory.PUNCTUATION to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_PUNCTUATION",
                DefaultLanguageHighlighterColors.PARENTHESES,
            ),
            XbHighlightCategory.COMMENT to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_COMMENT",
                DefaultLanguageHighlighterColors.LINE_COMMENT,
            ),
            XbHighlightCategory.ERROR to TextAttributesKey.createTextAttributesKey(
                "XBASEPP_ERROR",
                DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE,
            ),
        )

        internal val MACRO_DEFINITION: TextAttributesKey = KEYS_BY_CATEGORY.getValue(XbHighlightCategory.MACRO_DEFINITION)
        internal val FUNCTION_DECLARATION: TextAttributesKey = KEYS_BY_CATEGORY.getValue(XbHighlightCategory.FUNCTION_DECLARATION)
        internal val FUNCTION_CALL: TextAttributesKey = KEYS_BY_CATEGORY.getValue(XbHighlightCategory.FUNCTION_CALL)
    }
}
