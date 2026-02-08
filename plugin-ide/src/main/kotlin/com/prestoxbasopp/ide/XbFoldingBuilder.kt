package com.prestoxbasopp.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class XbFoldingBuilder(
    private val snapshotBuilder: XbPsiTextBuilder = XbPsiTextBuilder(),
    private val foldingService: XbFoldingService = XbFoldingService(),
) : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val file = root.containingFile ?: return emptyArray()
        if (file !is XbPsiFile) {
            return emptyArray()
        }
        val snapshot = snapshotBuilder.buildSnapshot(document.text, file.name)
        val maxOffset = document.textLength
        return foldingService.foldingRanges(snapshot)
            .mapNotNull { range ->
                val start = range.textRange.startOffset
                val end = range.textRange.endOffset
                if (start < 0 || end > maxOffset || start >= end) {
                    return@mapNotNull null
                }
                FoldingDescriptor(file.node, TextRange(start, end), null, range.placeholder.ifBlank { "..." })
            }
            .toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
