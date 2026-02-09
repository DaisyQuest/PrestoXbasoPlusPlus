package com.prestoxbasopp.ide

import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbTokenType

class XbTableFocusResolver(
    private val lexer: XbLexer = XbLexer(),
    private val focusKeywords: Set<String> = setOf("select", "sele", "use"),
) {
    fun resolve(source: String, offset: Int): String? {
        val safeOffset = offset.coerceIn(0, source.length)
        val tokens = lexer.lex(source.substring(0, safeOffset)).tokens
        var current: String? = null
        var expectTable = false
        tokens.forEach { token ->
            val normalized = token.text.lowercase()
            if (token.type in setOf(XbTokenType.KEYWORD, XbTokenType.IDENTIFIER) &&
                normalized in focusKeywords
            ) {
                expectTable = true
                return@forEach
            }
            if (expectTable && token.type == XbTokenType.IDENTIFIER) {
                current = token.text
                expectTable = false
            } else if (token.type != XbTokenType.COMMENT && token.type != XbTokenType.PREPROCESSOR) {
                if (token.type != XbTokenType.IDENTIFIER) {
                    expectTable = false
                }
            }
        }
        return current
    }
}
