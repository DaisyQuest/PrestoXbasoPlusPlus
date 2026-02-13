package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.inspections.XbInspectionSeverity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionProfileSupportTest {
    @Test
    fun `supports disabling and re-enabling inspections`() {
        val provider = XbMutableInspectionProfileProvider()

        provider.disableInspection("XB220")
        assertThat(provider.profile().isEnabled("XB220")).isFalse()

        provider.enableInspection("XB220")
        assertThat(provider.profile().isEnabled("XB220")).isTrue()
    }

    @Test
    fun `supports configuring and clearing severity overrides`() {
        val provider = XbMutableInspectionProfileProvider()

        provider.setSeverityOverride("XB220", XbInspectionSeverity.INFO)
        assertThat(provider.profile().severityFor("XB220", XbInspectionSeverity.WARNING))
            .isEqualTo(XbInspectionSeverity.INFO)

        provider.setSeverityOverride("XB220", null)
        assertThat(provider.profile().severityFor("XB220", XbInspectionSeverity.WARNING))
            .isEqualTo(XbInspectionSeverity.WARNING)
    }
}
