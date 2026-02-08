package com.prestoxbasopp.ide

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class XbLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = XbLanguage

    override fun getCodeSample(settingsType: SettingsType): String = """
        function Main()
          if ready
            ? "Hello Xbase++"
          else
            ? "Not ready"
          endif
        return
    """.trimIndent()

    override fun getIndentOptionsEditor(): IndentOptionsEditor = IndentOptionsEditor()

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showStandardOptions(
                "INDENT_SIZE",
                "CONTINUATION_INDENT_SIZE",
                "TAB_SIZE",
                "USE_TAB_CHARACTER",
                "KEEP_INDENTS_ON_EMPTY_LINES",
            )
        }
    }
}
