package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange

data class XbPreprocessResult(
    val filteredSource: String,
    val directives: List<XbPreprocessorDirective>,
    val sourceMap: XbSourceOffsetMapping,
)

object XbPreprocessor {
    fun preprocess(source: String): XbPreprocessResult {
        if (source.isEmpty()) {
            return XbPreprocessResult(
                filteredSource = "",
                directives = emptyList(),
                sourceMap = XbSourceOffsetMapping.identity(0),
            )
        }
        val directives = mutableListOf<XbPreprocessorDirective>()
        val builder = StringBuilder()
        val segments = mutableListOf<XbSourceOffsetMapping.Segment>()
        var logicalIndex = 0
        var index = 0
        var lineStart = 0
        while (index < source.length) {
            val lineEnd = source.indexOf('\n', index).let { if (it == -1) source.length else it }
            val lineSlice = source.substring(lineStart, lineEnd)
            val firstNonWhitespace = lineSlice.indexOfFirst { !it.isWhitespace() }
            val directiveOffset = if (firstNonWhitespace >= 0) lineStart + firstNonWhitespace else -1
            val isDirective = directiveOffset >= 0 && source[directiveOffset] == '#'
            if (isDirective) {
                var nameStart = directiveOffset + 1
                while (nameStart < lineEnd && source[nameStart].isWhitespace()) {
                    nameStart++
                }
                val nameEnd = nameStart + source.substring(nameStart, lineEnd)
                    .takeWhile { it.isLetter() }
                    .length
                val name = if (nameStart < nameEnd) source.substring(nameStart, nameEnd) else ""
                directives += XbPreprocessorDirective(
                    name = name,
                    text = source.substring(directiveOffset, lineEnd),
                    range = XbTextRange(directiveOffset, lineEnd),
                )
                if (lineEnd < source.length) {
                    logicalIndex = copySegment(source, lineEnd, lineEnd + 1, builder, segments, logicalIndex)
                }
            } else {
                if (lineEnd > index) {
                    logicalIndex = copySegment(source, index, lineEnd, builder, segments, logicalIndex)
                }
                if (lineEnd < source.length) {
                    logicalIndex = copySegment(source, lineEnd, lineEnd + 1, builder, segments, logicalIndex)
                }
            }
            index = lineEnd + 1
            lineStart = index
        }
        val sourceMap = if (segments.isEmpty()) {
            XbSourceOffsetMapping.identity(0)
        } else {
            XbSourceOffsetMapping(segments)
        }
        return XbPreprocessResult(
            filteredSource = builder.toString(),
            directives = directives,
            sourceMap = sourceMap,
        )
    }

    private fun copySegment(
        source: String,
        start: Int,
        end: Int,
        builder: StringBuilder,
        segments: MutableList<XbSourceOffsetMapping.Segment>,
        logicalIndex: Int,
    ): Int {
        if (start >= end) {
            return logicalIndex
        }
        builder.append(source, start, end)
        val length = end - start
        segments += XbSourceOffsetMapping.Segment(
            logicalStart = logicalIndex,
            length = length,
            sourceStart = start,
        )
        return logicalIndex + length
    }
}
