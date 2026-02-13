package com.prestoxbasopp.ide

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.psi.PsiFile

class XbInspectionTool(
    private val inspectionService: XbInspectionService = XbInspectionService(),
) : LocalInspectionTool() {
    override fun getDisplayName(): String = "Xbase++ Inspection"

    override fun getGroupDisplayName(): String = "Xbase++"

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor> {
        if (file.language != XbLanguage) return ProblemDescriptor.EMPTY_ARRAY

        val source = file.text
        val findings = inspectionService.inspect(source)
        if (findings.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY

        return findings.map { finding ->
            val range = toTextRange(finding.range, source.length)
            val quickFixes = quickFixesForFinding(finding, source)
            manager.createProblemDescriptor(
                file,
                range,
                finding.message,
                toProblemHighlightType(finding.severity),
                isOnTheFly,
                *quickFixes,
            )
        }.toTypedArray()
    }
}
