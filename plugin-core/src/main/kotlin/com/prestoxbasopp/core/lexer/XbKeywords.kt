package com.prestoxbasopp.core.lexer

object XbKeywords {
    val controlFlow = setOf(
        "if",
        "elseif",
        "else",
        "endif",
        "for",
        "next",
        "while",
        "enddo",
        "do",
        "case",
        "otherwise",
        "switch",
        "endswitch",
        "return",
        "endfunction",
        "endfunc",
        "endproc",
        "endprocedure",
        "wait",
        "exit",
    )

    val declarations = setOf(
        "function",
        "procedure",
        "class",
        "method",
        "local",
        "static",
        "public",
        "private",
        "global",
    )

    val literals = setOf(
        "nil",
        "true",
        "false",
        "self",
        "super",
    )

    val operators = setOf(
        "and",
        "or",
        "not",
    )

    val all: Set<String> = controlFlow + declarations + literals + operators
}
