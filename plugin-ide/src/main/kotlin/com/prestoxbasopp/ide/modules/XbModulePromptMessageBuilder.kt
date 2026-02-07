package com.prestoxbasopp.ide.modules

class XbModulePromptMessageBuilder {
    fun buildMessage(candidates: List<XbModuleCandidate>): String {
        if (candidates.isEmpty()) {
            return "No XBase++ modules were detected."
        }
        val details = candidates.joinToString(separator = "\n") { candidate ->
            "• ${candidate.displayLabel()} — ${candidate.reason}"
        }
        return buildString {
            appendLine("XBase++ sources were detected. Create IntelliJ modules for:")
            append(details)
        }
    }
}
