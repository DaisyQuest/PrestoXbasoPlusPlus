package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot

data class XbStructureItem(
    val name: String,
    val elementType: XbPsiElementType,
    val textRange: XbTextRange,
    val children: List<XbStructureItem>,
)

class XbStructureViewBuilder {
    fun build(snapshot: XbPsiSnapshot): XbStructureItem {
        val children = buildChildren(snapshot)
        return XbStructureItem(
            name = displayName(snapshot),
            elementType = snapshot.elementType,
            textRange = snapshot.textRange,
            children = children,
        )
    }

    private fun buildChildren(snapshot: XbPsiSnapshot): List<XbStructureItem> {
        val includedSnapshots = collectIncludedSnapshots(snapshot)
            .sortedWith(
                compareBy<XbPsiSnapshot> { it.textRange.startOffset }
                    .thenByDescending { it.textRange.endOffset - it.textRange.startOffset },
            )
        val rootNode = StructureNode(snapshot)
        val stack = ArrayDeque<StructureNode>()
        stack.add(rootNode)

        includedSnapshots.forEach { child ->
            while (stack.isNotEmpty() && !containsRange(stack.last().snapshot.textRange, child.textRange)) {
                stack.removeLast()
            }
            val parent = stack.lastOrNull() ?: rootNode
            val childNode = StructureNode(child)
            parent.children += childNode
            if (canContainChildren(child)) {
                stack.add(childNode)
            }
        }

        return rootNode.children.map { toStructureItem(it) }
    }

    private fun collectIncludedSnapshots(snapshot: XbPsiSnapshot): List<XbPsiSnapshot> {
        val collected = mutableListOf<XbPsiSnapshot>()
        snapshot.children.forEach { child ->
            if (shouldInclude(child)) {
                collected += child
            }
            collected += collectIncludedSnapshots(child)
        }
        return collected
    }

    private fun toStructureItem(node: StructureNode): XbStructureItem {
        return XbStructureItem(
            name = displayName(node.snapshot),
            elementType = node.snapshot.elementType,
            textRange = node.snapshot.textRange,
            children = node.children.map { toStructureItem(it) },
        )
    }

    private fun shouldInclude(snapshot: XbPsiSnapshot): Boolean {
        return when (snapshot.elementType) {
            XbPsiElementType.FILE,
            XbPsiElementType.FUNCTION_DECLARATION,
            XbPsiElementType.VARIABLE_DECLARATION,
            -> true
            else -> false
        }
    }

    private fun canContainChildren(snapshot: XbPsiSnapshot): Boolean {
        return snapshot.elementType == XbPsiElementType.FUNCTION_DECLARATION
    }

    private fun containsRange(parent: XbTextRange, child: XbTextRange): Boolean {
        return parent.startOffset <= child.startOffset && parent.endOffset >= child.endOffset
    }

    private fun displayName(snapshot: XbPsiSnapshot): String {
        val explicitName = snapshot.name?.takeIf { it.isNotBlank() }
        if (explicitName != null) {
            if (snapshot.elementType == XbPsiElementType.FUNCTION_DECLARATION) {
                return formatFunctionName(explicitName, snapshot.parameters)
            }
            return explicitName
        }
        return when (snapshot.elementType) {
            XbPsiElementType.FILE -> "file"
            XbPsiElementType.BLOCK -> "block"
            XbPsiElementType.FUNCTION_DECLARATION -> formatFunctionName("function", snapshot.parameters)
            XbPsiElementType.VARIABLE_DECLARATION -> "variable"
            XbPsiElementType.SYMBOL_REFERENCE -> "reference"
            XbPsiElementType.LITERAL -> "literal"
        }
    }

    private fun formatFunctionName(name: String, parameters: List<String>): String {
        val suffix = if (parameters.isEmpty()) "" else parameters.joinToString(prefix = "(", postfix = ")")
        return "$name$suffix"
    }
}

private data class StructureNode(
    val snapshot: XbPsiSnapshot,
    val children: MutableList<StructureNode> = mutableListOf(),
)

class XbBreadcrumbsService(private val structureViewBuilder: XbStructureViewBuilder = XbStructureViewBuilder()) {
    fun breadcrumbs(snapshot: XbPsiSnapshot, offset: Int): List<XbStructureItem> {
        val root = structureViewBuilder.build(snapshot)
        return findPath(root, offset).orEmpty()
    }

    private fun findPath(node: XbStructureItem, offset: Int): List<XbStructureItem>? {
        if (!containsOffset(node.textRange, offset)) {
            return null
        }
        for (child in node.children) {
            val childPath = findPath(child, offset)
            if (childPath != null) {
                return listOf(node) + childPath
            }
        }
        return listOf(node)
    }

    private fun containsOffset(range: XbTextRange, offset: Int): Boolean {
        return offset >= range.startOffset && offset <= range.endOffset
    }
}
