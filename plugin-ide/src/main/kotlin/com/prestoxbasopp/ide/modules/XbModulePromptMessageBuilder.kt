package com.prestoxbasopp.ide.modules

class XbModulePromptMessageBuilder {
    fun buildMessage(candidates: List<XbModuleCandidate>): String {
        if (candidates.isEmpty()) {
            return "No Xbase++ modules detected."
        }
        val details = candidates.joinToString(separator = "\n") { candidate ->
            "• ${candidate.displayLabel()} — ${candidate.reason}"
        }
        return buildString {
            appendLine("Xbase++ sources detected. Create IntelliJ modules for:")
            append(details)
        }
    }
}
