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
        return XbStructureItem(
            name = displayName(snapshot),
            elementType = snapshot.elementType,
            textRange = snapshot.textRange,
            children = snapshot.children.map { build(it) },
        )
    }

    private fun displayName(snapshot: XbPsiSnapshot): String {
        val explicitName = snapshot.name?.takeIf { it.isNotBlank() }
        if (explicitName != null) {
            return explicitName
        }
        return when (snapshot.elementType) {
            XbPsiElementType.FILE -> "file"
            XbPsiElementType.BLOCK -> "block"
            XbPsiElementType.FUNCTION_DECLARATION -> "function"
            XbPsiElementType.VARIABLE_DECLARATION -> "variable"
            XbPsiElementType.SYMBOL_REFERENCE -> "reference"
            XbPsiElementType.LITERAL -> "literal"
        }
    }
}

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
