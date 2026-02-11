package com.prestoxbasopp.ide

import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLanguageCodeStyleSettingsProviderTest {
    @Test
    fun `language code style provider exposes language and code sample`() {
        val provider = XbLanguageCodeStyleSettingsProvider()

        assertThat(provider.language).isEqualTo(XbLanguage)
        assertThat(provider.getCodeSample(SettingsType.INDENT_SETTINGS)).contains("function", "if", "endif")
    }

    @Test
    fun `language code style provider declares indent customization for indent settings`() {
        val provider = XbLanguageCodeStyleSettingsProvider()

        val settingsAware = provider.getCodeSample(SettingsType.INDENT_SETTINGS)
        val wrappingAware = provider.getCodeSample(SettingsType.WRAPPING_AND_BRACES_SETTINGS)

        assertThat(settingsAware).isEqualTo(wrappingAware)
    }
}
