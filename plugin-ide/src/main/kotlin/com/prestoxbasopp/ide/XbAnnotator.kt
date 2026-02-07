package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.parser.TokenType
import com.prestoxbasopp.core.parser.XbLexer

enum class XbSeverity {
    ERROR,
    WARNING,
}

data class XbDiagnostic(
    val textRange: XbTextRange,
    val message: String,
    val severity: XbSeverity,
)

class XbAnnotator {
    fun annotate(source: String): List<XbDiagnostic> {
        val lexer = XbLexer(source)
        return lexer.lex()
            .asSequence()
            .filter { it.type == TokenType.ERROR }
            .map { token ->
                val message = errorMessageFor(token.lexeme)
                XbDiagnostic(
                    textRange = XbTextRange(token.startOffset, token.endOffset),
                    message = message,
                    severity = XbSeverity.ERROR,
                )
            }
            .toList()
    }

    private fun errorMessageFor(lexeme: String): String {
        return when {
            lexeme.startsWith("\"") -> "Unterminated string literal."
            lexeme.length == 1 -> "Unexpected character: '$lexeme'."
            else -> "Unexpected token: '$lexeme'."
        }
    }
}
