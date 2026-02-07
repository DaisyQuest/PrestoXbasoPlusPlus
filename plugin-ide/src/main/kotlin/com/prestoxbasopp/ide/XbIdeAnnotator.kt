package com.prestoxbasopp.ide

import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class XbIdeAnnotator(
    private val inspectionService: XbInspectionService = XbInspectionService(),
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element as? PsiFile ?: return
        if (file.language != XbLanguage) return

        val source = file.text
        if (source.isEmpty()) return

        val diagnostics = XbAnnotator(inspectionService).annotate(source)
        diagnostics.forEach { diagnostic ->
            val textRange = toTextRange(diagnostic.textRange, source.length)
            holder.newAnnotation(toHighlightSeverity(diagnostic.severity), diagnostic.message)
                .range(textRange)
                .create()
        }
    }
}
