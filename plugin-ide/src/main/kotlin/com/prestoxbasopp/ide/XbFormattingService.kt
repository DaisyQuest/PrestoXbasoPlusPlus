package com.prestoxbasopp.ide

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.service.AbstractDocumentFormattingService
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class XbFormattingService(
    private val formatter: XbFormatter = XbFormatter(),
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
        val formatted = formatText(document.text, indentOptions.INDENT_SIZE)
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

    internal fun formatText(source: String, indentSize: Int): String = formatter.format(source, indentSize)
}
