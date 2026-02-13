package com.prestoxbasopp.ide

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.IncorrectOperationException
import com.prestoxbasopp.ide.inspections.XbInspectionFinding

internal fun quickFixesForFinding(finding: XbInspectionFinding, source: String): Array<LocalQuickFix> {
    val replacement = suggestedReplacement(finding, source) ?: return emptyArray()
    return arrayOf(ReplaceTextQuickFix(replacement.label, replacement.value))
}

private data class SuggestedReplacement(
    val label: String,
    val value: String,
)

private fun suggestedReplacement(finding: XbInspectionFinding, source: String): SuggestedReplacement? {
    val range = finding.range
    if (range.startOffset < 0 || range.endOffset > source.length || range.startOffset >= range.endOffset) return null
    val text = source.substring(range.startOffset, range.endOffset)
    return when (finding.id) {
        "XB200" -> if (text == ";") SuggestedReplacement("Remove redundant semicolon", "") else null
        "XB210" -> {
            if (text.length >= 2 && text.first() == '(' && text.last() == ')') {
                SuggestedReplacement("Remove redundant parentheses", text.substring(1, text.length - 1))
            } else {
                null
            }
        }
        "XB250" -> {
            val match = Regex("Did you mean \\\"([^\\\"]+)\\\"\\?").find(finding.message)
            val keyword = match?.groupValues?.getOrNull(1) ?: return null
            SuggestedReplacement("Replace with $keyword", keyword)
        }
        "XB270" -> if (text == "0") SuggestedReplacement("Replace index 0 with 1", "1") else null
        "XB274" -> if (text.equals("public", ignoreCase = true) || text.equals("private", ignoreCase = true)) {
            SuggestedReplacement("Replace with LOCAL", "LOCAL")
        } else {
            null
        }
        else -> null
    }
}

private class ReplaceTextQuickFix(
    private val name: String,
    private val replacement: String,
) : LocalQuickFix {
    override fun getFamilyName(): String = "Xbase++ Inspection Fixes"

    override fun getName(): String = name

    @Throws(IncorrectOperationException::class)
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val document = descriptor.psiElement.containingFile.viewProvider.document ?: return
        val hostRange = descriptor.psiElement.textRange ?: return
        val inElementRange = descriptor.textRangeInElement
        val startOffset = hostRange.startOffset + inElementRange.startOffset
        val endOffset = hostRange.startOffset + inElementRange.endOffset
        if (startOffset < 0 || endOffset > document.textLength || startOffset > endOffset) return

        document.replaceString(startOffset, endOffset, replacement)
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}
