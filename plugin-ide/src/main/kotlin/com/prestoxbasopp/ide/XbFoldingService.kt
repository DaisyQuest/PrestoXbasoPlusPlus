package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.psi.XbPsiElementType
import com.prestoxbasopp.core.psi.XbPsiSnapshot

data class XbFoldingRange(
    val textRange: XbTextRange,
    val placeholder: String,
)

class XbFoldingService {
    fun foldingRanges(snapshot: XbPsiSnapshot): List<XbFoldingRange> {
        val ranges = mutableListOf<XbFoldingRange>()
        collectRanges(snapshot, ranges)
        return ranges
    }

    private fun collectRanges(snapshot: XbPsiSnapshot, ranges: MutableList<XbFoldingRange>) {
        val isFoldable = snapshot.elementType == XbPsiElementType.BLOCK ||
            snapshot.elementType == XbPsiElementType.FUNCTION_DECLARATION
        if (isFoldable && isMultiChar(snapshot.textRange)) {
            ranges += XbFoldingRange(snapshot.textRange, placeholderFor(snapshot))
        }
        snapshot.children.forEach { collectRanges(it, ranges) }
    }

    private fun isMultiChar(range: XbTextRange): Boolean {
        return range.endOffset - range.startOffset > 1
    }

    private fun placeholderFor(snapshot: XbPsiSnapshot): String {
        return when (snapshot.elementType) {
            XbPsiElementType.BLOCK -> "{...}"
            XbPsiElementType.FUNCTION_DECLARATION -> "${snapshot.name.orEmpty()}(...)"
            else -> "..."
        }
    }
}
