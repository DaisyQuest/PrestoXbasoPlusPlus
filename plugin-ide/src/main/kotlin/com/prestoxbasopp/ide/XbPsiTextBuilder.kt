package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.core.lexer.XbLexer
import com.prestoxbasopp.core.lexer.XbToken
import com.prestoxbasopp.core.lexer.XbTokenType
import com.prestoxbasopp.core.psi.XbPsiElement
import com.prestoxbasopp.core.psi.XbPsiFile
import com.prestoxbasopp.core.psi.XbPsiFunctionDeclaration
import com.prestoxbasopp.core.psi.XbPsiBlock
import com.prestoxbasopp.core.psi.XbPsiSnapshot
import com.prestoxbasopp.core.psi.XbPsiSymbol
import com.prestoxbasopp.core.psi.XbPsiSymbolReference
import com.prestoxbasopp.core.psi.XbPsiVariableDeclaration
import com.prestoxbasopp.core.psi.XbVariableStorageClass
import kotlin.math.max
import kotlin.math.min

class XbPsiTextBuilder(private val lexer: XbLexer = XbLexer()) {
    private val functionKeywords = setOf("function", "procedure", "method")
    private val declarationBoundaryKeywords = functionKeywords + setOf("class", "endclass")
    private val declarationEndKeywordsByStartKeyword = mapOf(
        "function" to setOf("endfunction", "endfunc"),
        "procedure" to setOf("endprocedure", "endproc"),
        "method" to setOf("endmethod"),
    )
    private val allDeclarationEndKeywords = declarationEndKeywordsByStartKeyword.values.flatten().toSet()
    private val classBoundaryKeywords = setOf("class", "endclass")
    private val variableKeywords = setOf("local", "static", "public", "private", "global")
    private val storageClassByKeyword = mapOf(
        "local" to XbVariableStorageClass.LOCAL,
        "static" to XbVariableStorageClass.STATIC,
        "private" to XbVariableStorageClass.PRIVATE,
        "public" to XbVariableStorageClass.PUBLIC,
        "global" to XbVariableStorageClass.GLOBAL,
    )

    fun build(source: String, fileName: String = "file"): XbPsiFile {
        val tokens = lexer.lex(source).tokens.filter { it.type != XbTokenType.EOF }
        val declaredOffsets = mutableSetOf<Int>()
        val elements = mutableListOf<XbPsiElement>()

        collectDeclarations(tokens, source, declaredOffsets, elements)
        collectReferences(tokens, source, declaredOffsets, elements)
        val sorted = elements.sortedWith(
            compareBy<XbPsiElement> { it.textRange.startOffset }
                .thenByDescending { it.textRange.endOffset - it.textRange.startOffset },
        )
        val textRange = XbTextRange(0, max(source.length, 0))
        return XbPsiFile(
            name = fileName,
            textRange = textRange,
            text = source,
            children = nestElements(sorted, textRange),
        )
    }

    fun buildSnapshot(source: String, fileName: String = "file"): XbPsiSnapshot {
        return XbPsiSnapshot.fromElement(build(source, fileName))
    }

    private fun collectDeclarations(
        tokens: List<XbToken>,
        source: String,
        declaredOffsets: MutableSet<Int>,
        elements: MutableList<XbPsiElement>,
    ) {
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            val tokenKeyword = token.text.lowercase()
            if (tokenKeyword == "class" && !isClassMethodDeclaration(tokens, index)) {
                val classEndOffset = findClassEndOffset(
                    tokens = tokens,
                    searchStartIndex = index + 1,
                    fallbackEndOffset = token.range.endOffset,
                )
                elements += XbPsiBlock(
                    textRange = XbTextRange(token.range.startOffset, classEndOffset),
                    text = slice(source, token.range.startOffset, classEndOffset),
                )
            }
            if (token.type == XbTokenType.KEYWORD) {
                val keyword = token.text.lowercase()
                if (keyword in functionKeywords) {
                    val nameToken = tokens.getOrNull(index + 1)?.takeIf { it.type == XbTokenType.IDENTIFIER }
                    if (nameToken != null) {
                        declaredOffsets += nameToken.range.startOffset
                        val (parameterTokens, endIndex) = parseParameters(tokens, index + 2)
                        val parameters = parameterTokens.map { it.text }
                        val declaredEndOffset = tokens.getOrNull(endIndex)?.range?.endOffset
                            ?: nameToken.range.endOffset
                        val endOffset = findDeclarationEndOffset(
                            tokens = tokens,
                            startKeyword = keyword,
                            searchStartIndex = endIndex + 1,
                            fallbackEndOffset = declaredEndOffset,
                        )
                        elements += XbPsiFunctionDeclaration(
                            symbolName = nameToken.text,
                            parameters = parameters,
                            textRange = XbTextRange(token.range.startOffset, endOffset),
                            text = slice(source, token.range.startOffset, endOffset),
                        )
                        val bodyStartOffset = tokens.getOrNull(endIndex + 1)?.range?.startOffset
                            ?: nameToken.range.endOffset
                        val blockStartOffset = max(bodyStartOffset, token.range.startOffset)
                        val normalizedBlockStartOffset = min(blockStartOffset, endOffset)
                        elements += XbPsiBlock(
                            textRange = XbTextRange(normalizedBlockStartOffset, endOffset),
                            text = slice(source, normalizedBlockStartOffset, endOffset),
                        )
                        parameterTokens.forEach { parameterToken ->
                            declaredOffsets += parameterToken.range.startOffset
                            elements += XbPsiVariableDeclaration(
                                symbolName = parameterToken.text,
                                isMutable = true,
                                storageClass = XbVariableStorageClass.LOCAL,
                                textRange = parameterToken.range,
                                text = slice(source, parameterToken.range.startOffset, parameterToken.range.endOffset),
                            )
                        }
                    }
                }
                if (keyword in variableKeywords) {
                    index = collectVariableDeclarations(tokens, source, index, keyword, declaredOffsets, elements)
                }
            }
            index++
        }
    }

    private fun nestElements(elements: List<XbPsiElement>, rootRange: XbTextRange): List<XbPsiElement> {
        val rootNode = MutablePsiNode(
            element = XbPsiBlock(rootRange, "", emptyList()),
            children = mutableListOf(),
        )
        val stack = ArrayDeque<MutablePsiNode>()
        stack.add(rootNode)

        elements.forEach { element ->
            while (stack.isNotEmpty() && !containsRange(stack.last().element.textRange, element.textRange)) {
                stack.removeLast()
            }
            val parent = stack.lastOrNull() ?: rootNode
            val node = MutablePsiNode(element, mutableListOf())
            parent.children += node
            if (canContainChildren(element)) {
                stack.add(node)
            }
        }

        return rootNode.children.map { cloneWithChildren(it) }
    }

    private fun cloneWithChildren(node: MutablePsiNode): XbPsiElement {
        val nestedChildren = node.children.map { cloneWithChildren(it) }
        val element = node.element
        return when (element) {
            is XbPsiFunctionDeclaration -> XbPsiFunctionDeclaration(
                symbolName = element.symbolName,
                parameters = element.parameters,
                textRange = element.textRange,
                text = element.text,
                children = nestedChildren,
            )
            is XbPsiVariableDeclaration -> XbPsiVariableDeclaration(
                symbolName = element.symbolName,
                isMutable = element.isMutable,
                storageClass = element.storageClass,
                textRange = element.textRange,
                text = element.text,
                children = nestedChildren,
            )
            is XbPsiSymbolReference -> XbPsiSymbolReference(
                symbolName = element.symbolName,
                textRange = element.textRange,
                text = element.text,
                children = nestedChildren,
            )
            is XbPsiBlock -> XbPsiBlock(
                textRange = element.textRange,
                text = element.text,
                children = nestedChildren,
            )
            else -> element
        }
    }

    private fun canContainChildren(element: XbPsiElement): Boolean {
        return element is XbPsiFunctionDeclaration || element is XbPsiBlock
    }

    private fun containsRange(parent: XbTextRange, child: XbTextRange): Boolean {
        return parent.startOffset <= child.startOffset && parent.endOffset >= child.endOffset
    }

    private fun isClassMethodDeclaration(tokens: List<XbToken>, classKeywordIndex: Int): Boolean {
        val nextToken = tokens.getOrNull(classKeywordIndex + 1) ?: return false
        return nextToken.type == XbTokenType.KEYWORD && nextToken.text.equals("method", ignoreCase = true)
    }

    private fun findClassEndOffset(
        tokens: List<XbToken>,
        searchStartIndex: Int,
        fallbackEndOffset: Int,
    ): Int {
        var nestedClassDepth = 0
        var lastBodyTokenEndOffset = fallbackEndOffset
        for (index in searchStartIndex until tokens.size) {
            val token = tokens[index]
            val keyword = token.text.lowercase()
            if (keyword != "class" && keyword != "endclass" && keyword !in classBoundaryKeywords) {
                lastBodyTokenEndOffset = token.range.endOffset
                continue
            }
            if (keyword == "class" && !isClassMethodDeclaration(tokens, index)) {
                nestedClassDepth++
            } else if (keyword == "endclass") {
                if (nestedClassDepth == 0) {
                    return token.range.endOffset
                }
                nestedClassDepth--
            } else if (keyword in classBoundaryKeywords && nestedClassDepth == 0) {
                return lastBodyTokenEndOffset
            }
            lastBodyTokenEndOffset = token.range.endOffset
        }
        return fallbackEndOffset
    }

    private fun collectVariableDeclarations(
        tokens: List<XbToken>,
        source: String,
        startIndex: Int,
        keyword: String,
        declaredOffsets: MutableSet<Int>,
        elements: MutableList<XbPsiElement>,
    ): Int {
        var index = startIndex + 1
        val isMutable = keyword != "static"
        val storageClass = storageClassByKeyword[keyword] ?: XbVariableStorageClass.LOCAL
        while (index < tokens.size) {
            val token = tokens[index]
            when (token.type) {
                XbTokenType.IDENTIFIER -> {
                    declaredOffsets += token.range.startOffset
                    elements += XbPsiVariableDeclaration(
                        symbolName = token.text,
                        isMutable = isMutable,
                        storageClass = storageClass,
                        textRange = token.range,
                        text = slice(source, token.range.startOffset, token.range.endOffset),
                    )
                    val next = tokens.getOrNull(index + 1)
                    if (next != null && next.text == ",") {
                        index += 2
                        continue
                    }
                    return index
                }
                XbTokenType.PUNCTUATION -> {
                    if (token.text == ",") {
                        index++
                        continue
                    }
                    return index
                }
                else -> return index
            }
        }
        return index
    }

    private fun collectReferences(
        tokens: List<XbToken>,
        source: String,
        declaredOffsets: Set<Int>,
        elements: MutableList<XbPsiElement>,
    ) {
        tokens.forEach { token ->
            if (token.type == XbTokenType.IDENTIFIER && token.range.startOffset !in declaredOffsets) {
                elements += XbPsiSymbolReference(
                    symbolName = token.text,
                    textRange = token.range,
                    text = slice(source, token.range.startOffset, token.range.endOffset),
                )
            }
        }
    }

    private fun parseParameters(tokens: List<XbToken>, startIndex: Int): Pair<List<XbToken>, Int> {
        val parameters = mutableListOf<XbToken>()
        val openParen = tokens.getOrNull(startIndex) ?: return parameters to (startIndex - 1)
        if (openParen.text != "(") {
            return parameters to (startIndex - 1)
        }
        var index = startIndex + 1
        while (index < tokens.size) {
            val token = tokens[index]
            when (token.type) {
                XbTokenType.IDENTIFIER -> parameters += token
                XbTokenType.PUNCTUATION -> if (token.text == ")") {
                    return parameters to index
                }
                else -> Unit
            }
            index++
        }
        return parameters to (tokens.lastIndex)
    }

    private fun findDeclarationEndOffset(
        tokens: List<XbToken>,
        startKeyword: String,
        searchStartIndex: Int,
        fallbackEndOffset: Int,
    ): Int {
        if (searchStartIndex < 0) {
            return fallbackEndOffset
        }
        val declarationEndKeywords = declarationEndKeywordsByStartKeyword[startKeyword].orEmpty()
        var lastBodyTokenEndOffset = fallbackEndOffset
        for (index in searchStartIndex until tokens.size) {
            val token = tokens[index]
            val keyword = token.text.lowercase()
            if (keyword in declarationEndKeywords) {
                return token.range.endOffset
            }
            if (keyword in allDeclarationEndKeywords) {
                return token.range.endOffset
            }
            if (keyword in declarationBoundaryKeywords) {
                return lastBodyTokenEndOffset
            }
            lastBodyTokenEndOffset = token.range.endOffset
        }
        return fallbackEndOffset
    }

    private fun slice(source: String, start: Int, end: Int): String {
        val safeStart = min(max(start, 0), source.length)
        val safeEnd = min(max(end, safeStart), source.length)
        return source.substring(safeStart, safeEnd)
    }
}

private data class MutablePsiNode(
    val element: XbPsiElement,
    val children: MutableList<MutablePsiNode>,
)

object XbPsiSymbolLocator {
    fun findSymbol(root: XbPsiElement, offset: Int): XbPsiSymbol? {
        return root.walk()
            .filterIsInstance<XbPsiSymbol>()
            .filter { offset >= it.textRange.startOffset && offset <= it.textRange.endOffset }
            .minByOrNull { it.textRange.endOffset - it.textRange.startOffset }
    }
}
