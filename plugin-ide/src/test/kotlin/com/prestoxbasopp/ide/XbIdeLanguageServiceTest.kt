package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbIdeLanguageServiceTest {
    @Test
    fun `returns stable language id`() {
        val service = XbIdeLanguageService()

        assertThat(service.languageId()).isEqualTo("xbase++")
    }
}
