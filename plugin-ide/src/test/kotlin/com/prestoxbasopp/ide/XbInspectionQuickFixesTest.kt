package com.prestoxbasopp.ide

import com.prestoxbasopp.core.api.XbTextRange
import com.prestoxbasopp.ide.inspections.XbInspectionFinding
import com.prestoxbasopp.ide.inspections.XbInspectionSeverity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbInspectionQuickFixesTest {
    @Test
    fun `provides semicolon removal fix`() {
        val finding = finding("XB200", "Empty statement is unnecessary.", XbTextRange(3, 4))

        val fixes = quickFixesForFinding(finding, "if ;")

        assertThat(fixes.single().name).isEqualTo("Remove redundant semicolon")
    }

    @Test
    fun `provides parentheses removal fix`() {
        val finding = finding("XB210", "Parentheses around a single value are redundant.", XbTextRange(0, 5))

        val fixes = quickFixesForFinding(finding, "(foo)")

        assertThat(fixes.single().name).isEqualTo("Remove redundant parentheses")
    }

    @Test
    fun `provides keyword replacement fix from suggestion message`() {
        val finding = finding("XB250", "Did you mean \"FUNCTION\"?", XbTextRange(0, 7))

        val fixes = quickFixesForFinding(finding, "funcion")

        assertThat(fixes.single().name).isEqualTo("Replace with FUNCTION")
    }

    @Test
    fun `provides array index fix`() {
        val finding = finding("XB270", "Arrays in Xbase++ are 1-based. Index 0 is invalid.", XbTextRange(2, 3))

        val fixes = quickFixesForFinding(finding, "a[0]")

        assertThat(fixes.single().name).isEqualTo("Replace index 0 with 1")
    }

    @Test
    fun `provides scope keyword replacement fix`() {
        val finding = finding("XB274", "Avoid public variables; prefer LOCAL or STATIC for safer scope.", XbTextRange(0, 6))

        val fixes = quickFixesForFinding(finding, "public x")

        assertThat(fixes.single().name).isEqualTo("Replace with LOCAL")
    }

    @Test
    fun `does not provide fixes for unsupported or invalid ranges`() {
        val unknown = finding("XB999", "unknown", XbTextRange(0, 1))
        val invalidRange = finding("XB200", "invalid", XbTextRange(10, 11))

        assertThat(quickFixesForFinding(unknown, "x")).isEmpty()
        assertThat(quickFixesForFinding(invalidRange, "if ;")).isEmpty()
    }

    private fun finding(id: String, message: String, range: XbTextRange): XbInspectionFinding {
        return XbInspectionFinding(
            id = id,
            title = "title",
            message = message,
            severity = XbInspectionSeverity.WARNING,
            range = range,
        )
    }
}
