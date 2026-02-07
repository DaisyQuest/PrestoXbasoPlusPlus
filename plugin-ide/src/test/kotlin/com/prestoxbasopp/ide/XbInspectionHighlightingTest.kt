package com.prestoxbasopp.ide

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionHighlightingTest {
    @Test
    fun `maps diagnostic severity to highlight severity`() {
        assertThat(toHighlightSeverity(XbSeverity.ERROR)).isEqualTo(HighlightSeverity.ERROR)
        assertThat(toHighlightSeverity(XbSeverity.WARNING)).isEqualTo(HighlightSeverity.WARNING)
        assertThat(toHighlightSeverity(XbSeverity.INFO)).isEqualTo(HighlightSeverity.INFORMATION)
    }

    @Test
    fun `maps inspection severity to problem highlight type`() {
        assertThat(toProblemHighlightType(XbInspectionSeverity.ERROR))
            .isEqualTo(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        assertThat(toProblemHighlightType(XbInspectionSeverity.WARNING))
            .isEqualTo(ProblemHighlightType.WARNING)
        assertThat(toProblemHighlightType(XbInspectionSeverity.INFO))
            .isEqualTo(ProblemHighlightType.INFORMATION)
    }

    @Test
    fun `coerces inspection ranges to file bounds`() {
        val range = toTextRange(XbTextRange(10, 12), 5)
        assertThat(range).isEqualTo(TextRange(5, 5))

        val normalRange = toTextRange(XbTextRange(1, 3), 5)
        assertThat(normalRange).isEqualTo(TextRange(1, 3))
    }
}
