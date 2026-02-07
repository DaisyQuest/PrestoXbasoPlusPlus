package com.prestoxbasopp.ide

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity

internal fun toHighlightSeverity(severity: XbSeverity): HighlightSeverity = when (severity) {
    XbSeverity.ERROR -> HighlightSeverity.ERROR
    XbSeverity.WARNING -> HighlightSeverity.WARNING
    XbSeverity.INFO -> HighlightSeverity.INFORMATION
}

internal fun toProblemHighlightType(severity: XbInspectionSeverity): ProblemHighlightType = when (severity) {
    XbInspectionSeverity.ERROR -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    XbInspectionSeverity.WARNING -> ProblemHighlightType.WARNING
    XbInspectionSeverity.INFO -> ProblemHighlightType.INFORMATION
}

internal fun toTextRange(range: XbTextRange, maxLength: Int): TextRange {
    val start = range.startOffset.coerceIn(0, maxLength)
    val end = range.endOffset.coerceIn(start, maxLength)
    return TextRange(start, end)
}
