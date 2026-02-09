package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.ast.XbIfStatement
import com.prestoxbasopp.core.ast.XbStatement
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbTokenType
import com.prestoxbasopp.core.parser.XbParser

class XbIfBlockRangeFinder(
    private val lexer: XbLexer = XbLexer(),
) {
    fun buildIndex(source: String): XbIfBlockIndex {
        val tokens = lexer.lex(source).tokens
        val keywordRanges = tokens
            .filter { token ->
                token.type == XbTokenType.KEYWORD && token.text.lowercase() in KEYWORDS
            }
            .map { it.range }
        val parseResult = XbParser.parse(source)
        val ifRanges = parseResult.program?.statements
            ?.let { collectIfRanges(it) }
            ?: emptyList()
        return XbIfBlockIndex(ifRanges = ifRanges, keywordRanges = keywordRanges)
    }

    fun findRange(index: XbIfBlockIndex, offset: Int): XbTextRange? {
        val safeOffset = offset.coerceAtLeast(0)
        if (index.keywordRanges.none { it.contains(safeOffset) }) {
            return null
        }
        return index.ifRanges
            .filter { it.contains(safeOffset) }
            .minByOrNull { it.endOffset - it.startOffset }
    }

    private fun collectIfRanges(statements: List<XbStatement>): List<XbTextRange> {
        val ranges = mutableListOf<XbTextRange>()
        statements.forEach { statement ->
            when (statement) {
                is XbIfStatement -> {
                    ranges += statement.range
                    ranges += collectIfRanges(statement.thenBlock.statements)
                    statement.elseBlock?.let { ranges += collectIfRanges(it.statements) }
                }
                is com.prestoxbasopp.core.ast.XbWhileStatement -> {
                    ranges += collectIfRanges(statement.body.statements)
                }
                is com.prestoxbasopp.core.ast.XbForStatement -> {
                    ranges += collectIfRanges(statement.body.statements)
                }
                is com.prestoxbasopp.core.ast.XbFunctionDeclaration -> {
                    ranges += collectIfRanges(statement.body.statements)
                }
                is com.prestoxbasopp.core.ast.XbProcedureDeclaration -> {
                    ranges += collectIfRanges(statement.body.statements)
                }
                is com.prestoxbasopp.core.ast.XbBlock -> ranges += collectIfRanges(statement.statements)
                else -> Unit
            }
        }
        return ranges
    }

    private companion object {
        val KEYWORDS = setOf("if", "else", "endif")
    }
}

data class XbIfBlockIndex(
    val ifRanges: List<XbTextRange>,
    val keywordRanges: List<XbTextRange>,
)

private fun XbTextRange.contains(offset: Int): Boolean = offset in startOffset until endOffset
