package com.prestoxbasopp.core.lexer

import com.prestoxbasopp.core.api.XbTextRange

class XbLexer(
    private val keywordSet: Set<String> = XbKeywords.all,
) {
    fun lex(source: String): XbLexResult {
        val preprocess = preprocess(source)
        val lexer = LexerState(preprocess.filteredSource, preprocess.sourceMap, keywordSet)
        return lexer.lex(preprocess.directives)
    }

    private fun preprocess(source: String): PreprocessResult {
        if (source.isEmpty()) {
            return PreprocessResult(
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
                    copySegment(source, lineEnd, lineEnd + 1, builder, segments, logicalIndex).also {
                        logicalIndex = it
                    }
                }
            } else {
                if (lineEnd > index) {
                    copySegment(source, index, lineEnd, builder, segments, logicalIndex).also {
                        logicalIndex = it
                    }
                }
                if (lineEnd < source.length) {
                    copySegment(source, lineEnd, lineEnd + 1, builder, segments, logicalIndex).also {
                        logicalIndex = it
                    }
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
        return PreprocessResult(
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

    private data class PreprocessResult(
        val filteredSource: String,
        val directives: List<XbPreprocessorDirective>,
        val sourceMap: XbSourceOffsetMapping,
    )

    private class LexerState(
        private val source: String,
        private val sourceMap: XbSourceOffsetMapping,
        private val keywordSet: Set<String>,
    ) {
        private val tokens = mutableListOf<XbToken>()
        private val errors = mutableListOf<XbLexerError>()
        private var index = 0

        fun lex(directives: List<XbPreprocessorDirective>): XbLexResult {
            while (index < source.length) {
                val current = source[index]
                when {
                    current.isWhitespace() -> {
                        index++
                    }
                    current == '/' && peek(1) == '/' -> {
                        val start = index
                        index += 2
                        while (index < source.length && source[index] != '\n') {
                            index++
                        }
                        emitToken(XbTokenType.COMMENT, start, index)
                    }
                    current == '/' && peek(1) == '*' -> {
                        val start = index
                        index += 2
                        while (index < source.length && !(source[index] == '*' && peek(1) == '/')) {
                            index++
                        }
                        if (index < source.length) {
                            index += 2
                        } else {
                            errors += error("Unterminated block comment", start, index)
                        }
                        emitToken(XbTokenType.COMMENT, start, index)
                    }
                    current == '"' || current == '\'' -> {
                        lexString(current)
                    }
                    current == '{' && peek(1) == '^' -> {
                        lexDate()
                    }
                    current == '{' && peek(1) == '|' -> {
                        lexCodeblock()
                    }
                    current == '#' -> {
                        lexSymbol()
                    }
                    current.isDigit() || (current == '.' && peek(1)?.isDigit() == true) -> {
                        lexNumber()
                    }
                    current.isLetter() || current == '_' -> {
                        lexIdentifierOrKeyword()
                    }
                    else -> {
                        lexOperatorOrPunctuation()
                    }
                }
            }
            tokens += XbToken(XbTokenType.EOF, "", mapRange(XbTextRange(source.length, source.length)))
            return XbLexResult(tokens, directives, errors, sourceMap, source)
        }

        private fun lexString(quote: Char) {
            val start = index
            index++
            var escaped = false
            while (index < source.length) {
                val char = source[index]
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    index++
                    emitToken(XbTokenType.STRING, start, index)
                    return
                }
                index++
            }
            errors += error("Unterminated string literal", start, index)
            emitToken(XbTokenType.STRING, start, index)
        }

        private fun lexDate() {
            val start = index
            index += 2
            val contentStart = index
            while (index < source.length && source[index] != '}') {
                index++
            }
            if (index >= source.length) {
                errors += error("Unterminated date literal", start, index)
                emitToken(XbTokenType.DATE, start, index)
                return
            }
            val content = source.substring(contentStart, index)
            val isValid = isValidDateLiteral(content)
            index++
            if (isValid) {
                emitToken(XbTokenType.DATE, start, index)
            } else {
                errors += error("Invalid date literal", start, index)
                emitToken(XbTokenType.UNKNOWN, start, index)
            }
        }

        private fun lexCodeblock() {
            val start = index
            index += 2
            while (index < source.length && !(source[index] == '|' && peek(1) == '}')) {
                index++
            }
            if (index >= source.length) {
                errors += error("Unterminated codeblock literal", start, index)
                emitToken(XbTokenType.CODEBLOCK, start, index)
                return
            }
            index += 2
            emitToken(XbTokenType.CODEBLOCK, start, index)
        }

        private fun lexSymbol() {
            val start = index
            index++
            val symbolStart = index
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) {
                index++
            }
            if (symbolStart == index) {
                errors += error("Empty symbol literal", start, index)
                emitToken(XbTokenType.UNKNOWN, start, index)
                return
            }
            emitToken(XbTokenType.SYMBOL, start, index)
        }

        private fun lexNumber() {
            val start = index
            if (source[index] == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
                index += 2
                val digitsStart = index
                while (index < source.length && source[index].isHexDigit()) {
                    index++
                }
                if (digitsStart == index) {
                    errors += error("Invalid hex literal", start, index)
                    emitToken(XbTokenType.UNKNOWN, start, index)
                    return
                }
                emitToken(XbTokenType.NUMBER, start, index)
                return
            }
            while (index < source.length && source[index].isDigit()) {
                index++
            }
            if (index < source.length && source[index] == '.') {
                index++
                while (index < source.length && source[index].isDigit()) {
                    index++
                }
            }
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && (source[index] == '+' || source[index] == '-')) {
                    index++
                }
                val expStart = index
                while (index < source.length && source[index].isDigit()) {
                    index++
                }
                if (expStart == index) {
                    errors += error("Invalid exponent", start, index)
                    emitToken(XbTokenType.UNKNOWN, start, index)
                    return
                }
            }
            emitToken(XbTokenType.NUMBER, start, index)
        }

        private fun lexIdentifierOrKeyword() {
            val start = index
            index++
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) {
                index++
            }
            val text = source.substring(start, index)
            val type = if (keywordSet.contains(text.lowercase())) XbTokenType.KEYWORD else XbTokenType.IDENTIFIER
            emitToken(type, start, index)
        }

        private fun lexOperatorOrPunctuation() {
            val start = index
            val twoChar = if (index + 1 < source.length) source.substring(index, index + 2) else ""
            val type = when (twoChar) {
                "==", "!=", ">=", "<=", "+=", "-=", "*=", "/=", "&&", "||", "::", "->" -> {
                    index += 2
                    XbTokenType.OPERATOR
                }
                else -> {
                    val current = source[index]
                    if (OPERATORS.contains(current)) {
                        index++
                        XbTokenType.OPERATOR
                    } else if (PUNCTUATION.contains(current)) {
                        index++
                        XbTokenType.PUNCTUATION
                    } else {
                        index++
                        errors += error("Unexpected character '$current'", start, index)
                        XbTokenType.UNKNOWN
                    }
                }
            }
            emitToken(type, start, index)
        }

        private fun emitToken(type: XbTokenType, start: Int, end: Int) {
            val mappedRange = mapRange(XbTextRange(start, end))
            val text = source.substring(start, end)
            tokens += XbToken(type, text, mappedRange)
        }

        private fun mapRange(range: XbTextRange): XbTextRange {
            return sourceMap.toSourceRange(range) ?: range
        }

        private fun error(message: String, start: Int, end: Int): XbLexerError {
            return XbLexerError(message, mapRange(XbTextRange(start, end)))
        }

        private fun peek(offset: Int): Char? {
            val target = index + offset
            return if (target in source.indices) source[target] else null
        }

        companion object {
            private val OPERATORS = setOf('+', '-', '*', '/', '=', '<', '>', '!', '^', '%', '?')
            private val PUNCTUATION = setOf('(', ')', '{', '}', '[', ']', ';', ',', ':', '.')
        }

        private fun isValidDateLiteral(content: String): Boolean {
            val parts = content.split('-')
            if (parts.size != 3) {
                return false
            }
            val (yearPart, monthPart, dayPart) = parts
            if (yearPart.length != 4 || monthPart.length != 2 || dayPart.length != 2) {
                return false
            }
            if (!yearPart.all { it.isDigit() } || !monthPart.all { it.isDigit() } || !dayPart.all { it.isDigit() }) {
                return false
            }
            val month = monthPart.toInt()
            val day = dayPart.toInt()
            return month in 1..12 && day in 1..31
        }
    }
}

private fun Char.isHexDigit(): Boolean = this.isDigit() || this.lowercaseChar() in 'a'..'f'
