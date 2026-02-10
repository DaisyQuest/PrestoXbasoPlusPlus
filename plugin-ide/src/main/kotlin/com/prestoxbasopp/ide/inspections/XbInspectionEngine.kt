package com.prestoxbasopp.ide.inspections

class XbInspectionEngine(
    private val inspections: List<XbInspectionRule>,
    private val policy: XbInspectionPerformancePolicy = XbInspectionPerformancePolicy(),
) {
    fun inspect(
        source: String,
        profile: XbInspectionProfile = XbInspectionProfile(),
        contextProvider: (String) -> XbInspectionContext = { text ->
            if (policy.shouldUseLexOnlyContext(text)) {
                XbInspectionContext.fromLexOnly(text)
            } else {
                XbInspectionContext.fromSource(text)
            }
        },
    ): List<XbInspectionFinding> {
        val context = contextProvider(source)
        val findings = inspections
            .filter { profile.isEnabled(it.id) }
            .flatMap { rule ->
                rule.inspect(context).map { finding ->
                    val severity = profile.severityFor(rule.id, finding.severity)
                    if (severity == finding.severity) finding else finding.copy(severity = severity)
                }
            }
        return findings.sortedWith(compareBy<XbInspectionFinding> { it.range.startOffset }
            .thenBy { it.range.endOffset }
            .thenBy { it.severity.ordinal }
            .thenBy { it.id })
    }
}


data class XbInspectionPerformancePolicy(
    val maxSourceLengthForFullInspection: Int = 350_000,
) {
    fun shouldUseLexOnlyContext(source: String): Boolean = source.length > maxSourceLengthForFullInspection
}
