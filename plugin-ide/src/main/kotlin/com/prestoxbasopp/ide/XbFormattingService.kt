package com.prestoxbasopp.ide

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AbstractDocumentFormattingService
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.prestoxbasopp.ui.XbUiSettingsState
import com.prestoxbasopp.ui.XbUiSettingsStore

class XbFormattingService(
    private val formatter: XbFormatter = XbFormatter(),
    private val settingsStore: XbUiSettingsStore = XbUiSettingsStore.defaultsStore(),
) : AbstractDocumentFormattingService() {
    override fun getFeatures(): Set<FormattingService.Feature> = emptySet()

    override fun canFormat(file: PsiFile): Boolean = file is XbPsiFile

    override fun formatDocument(
        document: Document,
        formattingRanges: List<TextRange>,
        formattingContext: FormattingContext,
        canChangeWhiteSpaceOnly: Boolean,
        quickFormat: Boolean,
    ) {
        val file = formattingContext.containingFile
        if (file !is XbPsiFile) {
            return
        }
        val indentOptions = formattingContext.codeStyleSettings.getIndentOptionsByFile(file)
        val formatted = formatText(
            source = document.text,
            codeStyleIndentSize = indentOptions.INDENT_SIZE,
            codeStyleTabSize = indentOptions.TAB_SIZE,
            useTabCharacter = indentOptions.USE_TAB_CHARACTER,
        )
        if (formatted == document.text) {
            return
        }
        val applyChange = Runnable { document.setText(formatted) }
        val application = ApplicationManager.getApplication()
        if (application.isWriteAccessAllowed) {
            applyChange.run()
        } else {
            application.runWriteAction(applyChange)
        }
    }

    internal fun formatText(
        source: String,
        codeStyleIndentSize: Int,
        codeStyleTabSize: Int,
        useTabCharacter: Boolean,
    ): String {
        val resolvedIndentSize = resolveIndentSize(codeStyleIndentSize)
        val indentUnit = if (useTabCharacter) {
            "\t"
        } else {
            " ".repeat(resolveSpaceIndentSize(resolvedIndentSize, codeStyleTabSize))
        }
        return formatter.formatWithIndentUnit(source, indentUnit)
    }

    internal fun resolveIndentSize(codeStyleIndentSize: Int): Int {
        val defaultIndent = XbUiSettingsState().tabSize
        val settingsIndent = settingsStore.load().tabSize
        return if (codeStyleIndentSize == defaultIndent) settingsIndent else codeStyleIndentSize
    }

    internal fun resolveSpaceIndentSize(indentSize: Int, tabSize: Int): Int {
        return if (indentSize > 0) indentSize else tabSize.coerceAtLeast(0)
    }
}
