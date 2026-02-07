package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.inspections.XbInspectionEngine
import com.prestoxbasopp.ide.inspections.XbInspectionFinding
import com.prestoxbasopp.ide.inspections.XbInspectionProfile
import com.prestoxbasopp.ide.inspections.XbStandardInspections

class XbInspectionService(
    private val engine: XbInspectionEngine = XbInspectionEngine(XbStandardInspections.all),
) {
    fun inspect(source: String, profile: XbInspectionProfile = XbInspectionProfile()): List<XbInspectionFinding> {
        return engine.inspect(source, profile)
    }
}
