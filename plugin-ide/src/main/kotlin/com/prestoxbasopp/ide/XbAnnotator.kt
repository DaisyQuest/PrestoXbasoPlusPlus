package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbLexer

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
        val result = XbLexer().lex(source)
        return result.errors.map { error ->
            XbDiagnostic(
                textRange = error.range,
                message = formatMessage(error.message),
                severity = XbSeverity.ERROR,
            )
        }
    }

    private fun formatMessage(message: String): String {
        return when {
            message.startsWith("Unexpected character ") -> {
                val suffix = message.removePrefix("Unexpected character ").trim()
                "Unexpected character: $suffix."
            }
            message.endsWith(".") -> message
            else -> "$message."
        }
    }
}
