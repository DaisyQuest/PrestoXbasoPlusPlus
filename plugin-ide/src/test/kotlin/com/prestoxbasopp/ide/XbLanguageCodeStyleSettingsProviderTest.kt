package com.prestoxbasopp.ide

import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbLanguageCodeStyleSettingsProviderTest {
    @Test
    fun `language code style provider exposes language and code sample`() {
        val provider = XbLanguageCodeStyleSettingsProvider()

        assertThat(provider.language).isEqualTo(XbLanguage)
        assertThat(provider.getIndentOptionsEditor()).isNotNull
        assertThat(provider.getCodeSample(SettingsType.INDENT_SETTINGS)).contains("function", "if", "endif")
    }

    @Test
    fun `language code style provider exposes standard indent options`() {
        val provider = XbLanguageCodeStyleSettingsProvider()
        val customizable = RecordingCustomizable()

        provider.customizeSettings(customizable, SettingsType.INDENT_SETTINGS)

        assertThat(customizable.shownOptions)
            .contains("INDENT_SIZE", "CONTINUATION_INDENT_SIZE", "TAB_SIZE", "USE_TAB_CHARACTER", "KEEP_INDENTS_ON_EMPTY_LINES")
    }

    private class RecordingCustomizable : CodeStyleSettingsCustomizable {
        val shownOptions = mutableListOf<String>()

        override fun showAllStandardOptions() = Unit

        override fun showStandardOptions(vararg optionNames: String) {
            shownOptions.addAll(optionNames)
        }
    }
}
