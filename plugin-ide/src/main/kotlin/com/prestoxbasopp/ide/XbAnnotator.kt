package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity

enum class XbSeverity {
    ERROR,
    WARNING,
    INFO,
}

data class XbDiagnostic(
    val textRange: XbTextRange,
    val message: String,
    val severity: XbSeverity,
)

class XbAnnotator(
    private val inspectionService: XbInspectionService = XbInspectionService(),
) {
    fun annotate(source: String): List<XbDiagnostic> {
        return inspectionService.inspect(source).map { finding ->
            XbDiagnostic(
                textRange = finding.range,
                message = finding.message,
                severity = finding.severity.toXbSeverity(),
            )
        }
    }
}

private fun XbInspectionSeverity.toXbSeverity(): XbSeverity = when (this) {
    XbInspectionSeverity.ERROR -> XbSeverity.ERROR
    XbInspectionSeverity.WARNING -> XbSeverity.WARNING
    XbInspectionSeverity.INFO -> XbSeverity.INFO
}
