package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange

data class XbToken(
    val type: XbTokenType,
    val text: String,
    val range: XbTextRange,
)

data class XbLexerError(
    val message: String,
    val range: XbTextRange,
)

data class XbPreprocessorDirective(
    val name: String,
    val text: String,
    val range: XbTextRange,
)

data class XbLexResult(
    val tokens: List<XbToken>,
    val directives: List<XbPreprocessorDirective>,
    val errors: List<XbLexerError>,
    val sourceMap: XbSourceOffsetMapping,
    val filteredSource: String,
)
