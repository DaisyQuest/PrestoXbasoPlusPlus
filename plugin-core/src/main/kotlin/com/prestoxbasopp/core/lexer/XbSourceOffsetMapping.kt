package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange

class XbSourceOffsetMapping(
    private val segments: List<Segment>,
) {
    data class Segment(
        val logicalStart: Int,
        val length: Int,
        val sourceStart: Int,
    ) {
        val logicalEnd: Int = logicalStart + length
    }

    val logicalLength: Int = segments.maxOfOrNull { it.logicalEnd } ?: 0

    fun toSourceOffset(logicalOffset: Int): Int? {
        if (logicalOffset < 0) {
            return null
        }
        val segment = segments.firstOrNull { logicalOffset >= it.logicalStart && logicalOffset < it.logicalEnd }
            ?: return null
        return segment.sourceStart + (logicalOffset - segment.logicalStart)
    }

    fun toSourceRange(range: XbTextRange): XbTextRange? {
        val start = toSourceOffset(range.startOffset) ?: return null
        val end = toSourceOffsetForRangeEnd(range.endOffset) ?: return null
        return XbTextRange(start, end)
    }

    private fun toSourceOffsetForRangeEnd(logicalOffset: Int): Int? {
        if (logicalOffset < 0) {
            return null
        }
        if (logicalOffset == 0) {
            return segments.firstOrNull()?.sourceStart
        }
        val segment = segments.lastOrNull { logicalOffset > it.logicalStart && logicalOffset <= it.logicalEnd }
            ?: return null
        return segment.sourceStart + (logicalOffset - segment.logicalStart)
    }

    companion object {
        fun identity(length: Int): XbSourceOffsetMapping {
            return XbSourceOffsetMapping(listOf(Segment(0, length, 0)))
        }
    }
}
