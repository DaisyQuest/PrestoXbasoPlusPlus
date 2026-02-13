package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.inspections.XbInspectionEngine
import com.prestoxbasopp.ide.inspections.XbInspectionFinding
import com.prestoxbasopp.ide.inspections.XbInspectionProfile
import com.prestoxbasopp.ide.inspections.XbStandardInspections

class XbInspectionService(
    private val engine: XbInspectionEngine = XbInspectionEngine(XbStandardInspections.all),
    private val profileProvider: XbInspectionProfileProvider = XbDefaultInspectionProfileProvider(),
) {
    fun inspect(
        source: String,
        profile: XbInspectionProfile = profileProvider.profile(),
    ): List<XbInspectionFinding> {
        return engine.inspect(source, profile)
    }
}
