package com.prestoxbasopp.ide

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity

class XbDiagnosticsAnnotator(
    private val inspectionService: XbInspectionService = XbInspectionService(),
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element as? PsiFile ?: return
        val source = file.text
        val diagnostics = inspectionService.inspect(source)
        diagnostics.forEach { finding ->
            val range = toTextRange(finding.range, source.length) ?: return@forEach
            val severity = finding.severity.toHighlightSeverity()
            val builder = holder.newAnnotation(severity, finding.message).range(range)
            if (finding.severity == XbInspectionSeverity.ERROR) {
                builder.highlightType(ProblemHighlightType.ERROR)
            }
            builder.create()
        }
    }
}

internal fun XbInspectionSeverity.toHighlightSeverity(): HighlightSeverity = when (this) {
    XbInspectionSeverity.ERROR -> HighlightSeverity.ERROR
    XbInspectionSeverity.WARNING -> HighlightSeverity.WARNING
    XbInspectionSeverity.INFO -> HighlightSeverity.INFORMATION
}

internal fun toTextRange(range: XbTextRange, documentLength: Int): TextRange? {
    if (documentLength < 0) return null
    val safeStart = range.startOffset.coerceIn(0, documentLength)
    val safeEnd = range.endOffset.coerceIn(safeStart, documentLength)
    if (safeStart >= safeEnd) {
        return null
    }
    return TextRange(safeStart, safeEnd)
}
