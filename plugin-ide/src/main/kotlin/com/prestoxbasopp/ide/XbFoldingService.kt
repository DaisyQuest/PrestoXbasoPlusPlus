package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbForStatement
import com.prestoxbasopp.core.ast.XbFunctionDeclaration
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbProcedureDeclaration
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.ast.XbWhileStatement
import com.prestoxbasopp.core.parser.XbParser
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
        collectAstRanges(snapshot.text, ranges)
        return uniqueRanges(ranges)
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
            XbPsiElementType.FUNCTION_DECLARATION -> {
                if (isMethodDeclaration(snapshot.text)) {
                    "METHOD {...}"
                } else {
                    "${snapshot.name.orEmpty()}(...)"
                }
            }
            else -> "..."
        }
    }

    private fun isMethodDeclaration(text: String): Boolean {
        return text.trimStart().startsWith("METHOD ", ignoreCase = true)
    }

    private fun collectAstRanges(source: String, ranges: MutableList<XbFoldingRange>) {
        if (source.isBlank()) {
            return
        }
        val parseResult = XbParser.parse(source)
        val program = parseResult.program ?: return
        collectAstStatementRanges(program.statements, ranges)
    }

    private fun collectAstStatementRanges(statements: List<XbStatement>, ranges: MutableList<XbFoldingRange>) {
        statements.forEach { statement ->
            placeholderFor(statement)?.let { placeholder ->
                if (isMultiChar(statement.range)) {
                    ranges += XbFoldingRange(statement.range, placeholder)
                }
            }
            when (statement) {
                is XbIfStatement -> {
                    collectAstStatementRanges(statement.thenBlock.statements, ranges)
                    statement.elseBlock?.let { collectAstStatementRanges(it.statements, ranges) }
                }
                is XbWhileStatement -> collectAstStatementRanges(statement.body.statements, ranges)
                is XbForStatement -> collectAstStatementRanges(statement.body.statements, ranges)
                is XbFunctionDeclaration -> collectAstStatementRanges(statement.body.statements, ranges)
                is XbProcedureDeclaration -> collectAstStatementRanges(statement.body.statements, ranges)
                else -> Unit
            }
        }
    }

    private fun placeholderFor(statement: XbStatement): String? {
        return when (statement) {
            is XbFunctionDeclaration -> "${statement.name}(...)"
            is XbProcedureDeclaration -> "${statement.name}(...)"
            is XbIfStatement -> "if (...)"
            is XbWhileStatement -> "while (...)"
            is XbForStatement -> "for ${statement.iterator.name} ..."
            else -> null
        }
    }

    private fun uniqueRanges(ranges: List<XbFoldingRange>): List<XbFoldingRange> {
        val seen = LinkedHashSet<String>()
        val unique = mutableListOf<XbFoldingRange>()
        ranges.forEach { range ->
            val key = "${range.textRange.startOffset}:${range.textRange.endOffset}:${range.placeholder}"
            if (seen.add(key)) {
                unique += range
            }
        }
        return unique
    }
}
