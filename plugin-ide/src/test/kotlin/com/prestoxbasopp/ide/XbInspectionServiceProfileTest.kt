package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.inspections.XbInspectionEngine
import com.prestoxbasopp.ide.inspections.XbInspectionProfile
import com.prestoxbasopp.ide.inspections.XbStandardInspections
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionServiceProfileTest {
    @Test
    fun `uses configured profile provider by default`() {
        val provider = XbMutableInspectionProfileProvider(
            XbInspectionProfile(disabledInspections = setOf("XB220")),
        )
        val service = XbInspectionService(XbInspectionEngine(XbStandardInspections.all), provider)

        val findings = service.inspect("if 1 then return 1 endif")

        assertThat(findings.none { it.id == "XB220" }).isTrue()
    }
}
