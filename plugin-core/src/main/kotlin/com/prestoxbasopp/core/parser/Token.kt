package com.prestoxbasopp.core.parser

data class Token(
    val type: TokenType,
    val lexeme: String,
    val startOffset: Int,
    val endOffset: Int,
)
