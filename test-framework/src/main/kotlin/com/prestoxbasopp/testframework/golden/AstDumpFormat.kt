package com.prestoxbasopp.testframework.golden

data class AstDumpNode(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<AstDumpNode> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "AST node name must not be blank" }
    }
}

object AstDumpFormat {
    private val safeValueRegex = Regex("^[A-Za-z0-9_.-]+$")

    fun render(node: AstDumpNode, indent: String = "  "): String {
        require(indent.isNotEmpty()) { "Indent must not be empty" }
        val lines = mutableListOf<String>()
        renderNode(node, indent, 0, lines)
        return lines.joinToString("\n")
    }

    private fun renderNode(node: AstDumpNode, indent: String, depth: Int, lines: MutableList<String>) {
        val prefix = indent.repeat(depth)
        val attributes = formatAttributes(node.attributes)
        lines += if (attributes.isEmpty()) {
            "${prefix}${node.name}"
        } else {
            "${prefix}${node.name}$attributes"
        }
        node.children.forEach { child ->
            renderNode(child, indent, depth + 1, lines)
        }
    }

    private fun formatAttributes(attributes: Map<String, String>): String {
        if (attributes.isEmpty()) return ""
        val sorted = attributes.toSortedMap()
        val formatted = sorted.entries.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ", ",
        ) { (key, value) ->
            "${key}=${formatValue(value)}"
        }
        return formatted
    }

    private fun formatValue(value: String): String {
        if (safeValueRegex.matches(value)) {
            return value
        }
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
