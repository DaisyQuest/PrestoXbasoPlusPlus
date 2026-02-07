package com.prestoxbasopp.ide

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbDiagnosticsAnnotatorTest {
    @Test
    fun `maps inspection severity to highlight severity`() {
        assertThat(XbInspectionSeverity.ERROR.toHighlightSeverity()).isEqualTo(HighlightSeverity.ERROR)
        assertThat(XbInspectionSeverity.WARNING.toHighlightSeverity()).isEqualTo(HighlightSeverity.WARNING)
        assertThat(XbInspectionSeverity.INFO.toHighlightSeverity()).isEqualTo(HighlightSeverity.INFORMATION)
    }

    @Test
    fun `clamps diagnostic ranges to document length`() {
        val textRange = toTextRange(XbTextRange(-4, 2), 4)
        assertThat(textRange).isEqualTo(TextRange(0, 2))
    }

    @Test
    fun `skips empty diagnostic ranges`() {
        val textRange = toTextRange(XbTextRange(3, 3), 10)
        assertThat(textRange).isNull()
    }
}
