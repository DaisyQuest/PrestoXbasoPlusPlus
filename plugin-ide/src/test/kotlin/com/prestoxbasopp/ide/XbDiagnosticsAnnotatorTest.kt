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
        val textRange = toNullableTextRange(XbTextRange(2, 10), 4)
        assertThat(textRange).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun `skips empty diagnostic ranges`() {
        val textRange = toNullableTextRange(XbTextRange(3, 3), 10)
        assertThat(textRange).isNull()
    }

    @Test
    fun `returns null when document length is negative`() {
        val textRange = toNullableTextRange(XbTextRange(1, 2), -1)
        assertThat(textRange).isNull()
    }
}
