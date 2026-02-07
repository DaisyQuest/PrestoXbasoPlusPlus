package com.prestoxbasopp.ui

import com.prestoxbasopp.core.api.XbLanguageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbUiSettingsModelTest {
    @Test
    fun `builds display name from language service`() {
        val service = object : XbLanguageService {
            override fun languageId(): String = "xbase++"
        }

        val model = XbUiSettingsModel(service)

        assertThat(model.displayName()).isEqualTo("xbase++ Settings")
    }
}
