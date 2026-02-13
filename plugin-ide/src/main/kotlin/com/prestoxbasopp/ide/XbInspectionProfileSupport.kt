package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.inspections.XbInspectionProfile
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity

interface XbInspectionProfileProvider {
    fun profile(): XbInspectionProfile
}

class XbMutableInspectionProfileProvider(
    initialProfile: XbInspectionProfile = XbInspectionProfile(),
) : XbInspectionProfileProvider {
    @Volatile
    private var currentProfile: XbInspectionProfile = initialProfile

    override fun profile(): XbInspectionProfile = currentProfile

    fun enableInspection(id: String) {
        val profile = currentProfile
        currentProfile = profile.copy(disabledInspections = profile.disabledInspections - id)
    }

    fun disableInspection(id: String) {
        val profile = currentProfile
        currentProfile = profile.copy(disabledInspections = profile.disabledInspections + id)
    }

    fun setSeverityOverride(id: String, severity: XbInspectionSeverity?) {
        val profile = currentProfile
        currentProfile = profile.copy(
            severityOverrides = if (severity == null) {
                profile.severityOverrides - id
            } else {
                profile.severityOverrides + (id to severity)
            },
        )
    }

    fun replace(profile: XbInspectionProfile) {
        currentProfile = profile
    }
}

class XbDefaultInspectionProfileProvider : XbInspectionProfileProvider {
    override fun profile(): XbInspectionProfile = XbInspectionProfile()
}
