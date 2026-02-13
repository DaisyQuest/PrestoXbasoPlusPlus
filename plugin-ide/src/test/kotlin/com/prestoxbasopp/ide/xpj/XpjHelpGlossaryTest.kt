package com.prestoxbasopp.ide.xpj

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XpjHelpGlossaryTest {
    @Test
    fun `includes key definitions macros and command options in help text`() {
        val helpText = XpjHelpGlossary.fullHelpText()

        assertThat(helpText).contains("XPJ Visual Editor — Capability Glossary")
        assertThat(helpText).contains("DEBUG")
        assertThat(helpText).contains("$(TARGET_PATH)")
        assertThat(helpText).contains("/g[:name]")
    }
}
