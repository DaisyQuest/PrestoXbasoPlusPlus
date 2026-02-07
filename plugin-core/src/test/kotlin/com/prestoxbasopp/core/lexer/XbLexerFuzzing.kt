package com.prestoxbasopp.core.lexer

import kotlin.random.Random

object XbLexerFuzzing {
    private val atoms = listOf(
        "if",
        "else",
        "123",
        "3.14",
        "0xFF",
        "{^2024-01-01}",
        "#sym",
        "{|x|x+1|}",
        "\"str\"",
        "'str'",
        "// comment\n",
        "/* block */",
        "==",
        "!=",
        "(",
        ")",
        "+",
        "-",
        "*",
        "/",
        "\n",
        " ",
    )

    fun randomSource(random: Random, length: Int): String {
        val builder = StringBuilder()
        repeat(length) {
            builder.append(atoms[random.nextInt(atoms.size)])
        }
        return builder.toString()
    }
}
