package com.prestoxbasopp.core.lexer

enum class XbTokenType {
    KEYWORD,
    IDENTIFIER,
    NUMBER,
    STRING,
    DATE,
    SYMBOL,
    CODEBLOCK,
    PREPROCESSOR,
    OPERATOR,
    PUNCTUATION,
    COMMENT,
    UNKNOWN,
    EOF,
}
