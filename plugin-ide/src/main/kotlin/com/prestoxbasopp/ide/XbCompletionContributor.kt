package com.prestoxbasopp.ide

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.openapi.editor.Document

class XbCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(XbLanguage),
            XbCompletionProvider(),
        )
    }
}

private class XbCompletionProvider(
    private val service: XbCompletionService = XbCompletionService(),
    private val mapper: XbCompletionLookupMapper = XbCompletionLookupMapper(),
    private val textBuilder: XbPsiTextBuilder = XbPsiTextBuilder(),
) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val document = parameters.editor.document
        val prefix = XbCompletionPrefixExtractor.extract(document, parameters.offset)
        val root = textBuilder.build(document.text, parameters.originalFile.name)
        val caseSensitive = XbCompletionCasePolicy.isCaseSensitive(prefix)
        val items = service.suggest(root, prefix, caseSensitive = caseSensitive)
        val resultSet = result.withPrefixMatcher(prefix)
        val lookups = mapper.map(items)
        lookups.forEach { lookup ->
            val builder = LookupElementBuilder.create(lookup.label)
                .withTypeText(lookup.typeText, true)
            val element = if (lookup.insertText != lookup.label) {
                builder.withInsertHandler(XbInsertHandler(lookup.insertText, lookup.caretOffsetDelta))
            } else {
                builder
            }
            resultSet.addElement(element)
        }
    }
}

private class XbInsertHandler(
    private val insertText: String,
    private val caretOffsetDelta: Int?,
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: com.intellij.codeInsight.completion.InsertionContext, item: LookupElement) {
        val document = context.document
        val start = context.startOffset
        val end = context.tailOffset
        document.replaceString(start, end, insertText)
        val caretOffset = start + insertText.length + (caretOffsetDelta ?: 0)
        context.editor.caretModel.moveToOffset(caretOffset)
    }
}

object XbCompletionPrefixExtractor {
    fun extract(document: Document, offset: Int): String {
        return extract(document.text, offset)
    }

    fun extract(text: String, offset: Int): String {
        val safeOffset = offset.coerceIn(0, text.length)
        var index = safeOffset - 1
        while (index >= 0 && isIdentifierChar(text[index])) {
            index--
        }
        return text.substring(index + 1, safeOffset)
    }

    private fun isIdentifierChar(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_'
    }
}
