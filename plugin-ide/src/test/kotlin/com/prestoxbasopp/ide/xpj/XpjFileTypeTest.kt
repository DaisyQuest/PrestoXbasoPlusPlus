package com.prestoxbasopp.ide.xpj

import com.intellij.openapi.fileTypes.PlainTextLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XpjFileTypeTest {
    @Test
    fun `uses plain text language to avoid xbase parser diagnostics for xpj syntax`() {
        assertThat(XpjFileType.language).isSameAs(PlainTextLanguage.INSTANCE)
        assertThat(XpjFileType.defaultExtension).isEqualTo("xpj")
    }
}
