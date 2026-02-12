package com.prestoxbasopp.core.lexer

object XbKeywords {
    val controlFlow = setOf(
        "if",
        "then",
        "elseif",
        "else",
        "endif",
        "for",
        "to",
        "step",
        "next",
        "while",
        "enddo",
        "do",
        "begin",
        "sequence",
        "recover",
        "using",
        "break",
        "end",
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
        "say",
        "get",
        "valid",
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
