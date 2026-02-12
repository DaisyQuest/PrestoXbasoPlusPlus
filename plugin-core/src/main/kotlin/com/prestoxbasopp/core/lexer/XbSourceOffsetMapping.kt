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
    private val logicalStarts: IntArray = IntArray(segments.size) { index -> segments[index].logicalStart }

    fun toSourceOffset(logicalOffset: Int): Int? {
        if (logicalOffset < 0) {
            return null
        }
        val segment = findContainingSegment(logicalOffset) ?: return null
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
        val segment = findContainingSegmentForRangeEnd(logicalOffset)
            ?: return null
        return segment.sourceStart + (logicalOffset - segment.logicalStart)
    }

    private fun findContainingSegment(logicalOffset: Int): Segment? {
        if (segments.isEmpty()) {
            return null
        }
        var low = 0
        var high = segments.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val segment = segments[mid]
            when {
                logicalOffset < segment.logicalStart -> high = mid - 1
                logicalOffset >= segment.logicalEnd -> low = mid + 1
                else -> return segment
            }
        }
        return null
    }

    private fun findContainingSegmentForRangeEnd(logicalOffset: Int): Segment? {
        if (segments.isEmpty()) {
            return null
        }
        val insertionPoint = logicalStarts.binarySearch(logicalOffset)
        val index = when {
            insertionPoint >= 0 -> insertionPoint - 1
            else -> -insertionPoint - 2
        }
        if (index !in segments.indices) {
            return null
        }
        val segment = segments[index]
        return if (logicalOffset > segment.logicalStart && logicalOffset <= segment.logicalEnd) {
            segment
        } else {
            null
        }
    }

    companion object {
        fun identity(length: Int): XbSourceOffsetMapping {
            return XbSourceOffsetMapping(listOf(Segment(0, length, 0)))
        }
    }
}
