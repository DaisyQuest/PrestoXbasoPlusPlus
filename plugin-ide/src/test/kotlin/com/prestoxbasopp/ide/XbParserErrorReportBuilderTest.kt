package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbParserErrorReportBuilderTest {
    @Test
    fun `returns empty entries for empty parser errors`() {
        val builder = XbParserErrorReportBuilder()

        val entries = builder.build("line1", emptyList())

        assertThat(entries).isEmpty()
    }

    @Test
    fun `builds parser error entries with line column and bounded evidence`() {
        val text = (1..30).joinToString("\n") { "1234567890" }
        val error = "Expected expression at 100"
        val builder = XbParserErrorReportBuilder(contextLines = 10)

        val entries = builder.build(text, listOf(error))

        assertThat(entries).hasSize(1)
        val entry = entries.single()
        assertThat(entry.message).isEqualTo(error)
        assertThat(entry.offset).isEqualTo(100)
        assertThat(entry.line).isEqualTo(10)
        assertThat(entry.column).isEqualTo(2)
        assertThat(entry.evidence?.startLine).isEqualTo(1)
        assertThat(entry.evidence?.endLine).isEqualTo(20)
        assertThat(entry.evidence?.focusLine).isEqualTo(10)
        assertThat(entry.evidence?.lines?.first()).isEqualTo("1234567890")
        assertThat(entry.evidence?.lines?.last()).isEqualTo("1234567890")
    }

    @Test
    fun `builds parser error entries without evidence when offset is unavailable`() {
        val builder = XbParserErrorReportBuilder()

        val entries = builder.build("line", listOf("Unexpected token"))

        assertThat(entries).hasSize(1)
        val entry = entries.single()
        assertThat(entry.offset).isNull()
        assertThat(entry.line).isNull()
        assertThat(entry.column).isNull()
        assertThat(entry.evidence).isNull()
    }

    @Test
    fun `formats empty parser errors display text`() {
        val builder = XbParserErrorReportBuilder()

        val text = builder.formatForDisplay(emptyList())

        assertThat(text).isEqualTo("No parser errors.")
    }

    @Test
    fun `formats parser errors display with evidence marker`() {
        val builder = XbParserErrorReportBuilder()
        val entries = listOf(
            XbParserErrorEntry(
                message = "Expected expression at 15",
                offset = 15,
                line = 2,
                column = 6,
                evidence = XbParserErrorEvidence(
                    startLine = 1,
                    endLine = 3,
                    focusLine = 2,
                    lines = listOf("first", "second", "third"),
                ),
            ),
        )

        val text = builder.formatForDisplay(entries)

        assertThat(text).contains("• Expected expression at 15 [line 2, column 6]")
        assertThat(text).contains(">    2 | second")
        assertThat(text).contains("     1 | first")
    }

    @Test
    fun `formats parser errors display with fallback locations`() {
        val builder = XbParserErrorReportBuilder()
        val entries = listOf(
            XbParserErrorEntry(
                message = "Unexpected token at 9",
                offset = 9,
                line = null,
                column = null,
                evidence = null,
            ),
            XbParserErrorEntry(
                message = "Unexpected token",
                offset = null,
                line = null,
                column = null,
                evidence = null,
            ),
        )

        val text = builder.formatForDisplay(entries)

        assertThat(text).contains("[offset 9]")
        assertThat(text).contains("[unknown location]")
    }

    @Test
    fun `exports parser errors as pretty json`() {
        val builder = XbParserErrorReportBuilder()
        val entries = listOf(
            XbParserErrorEntry(
                message = "Unexpected token at 0",
                offset = 0,
                line = 1,
                column = 1,
                evidence = XbParserErrorEvidence(
                    startLine = 1,
                    endLine = 1,
                    focusLine = 1,
                    lines = listOf("line"),
                ),
            ),
        )

        val json = builder.toPrettyJson(entries)

        assertThat(json).contains("\n")
        assertThat(json).contains("\"message\": \"Unexpected token at 0\"")
        assertThat(json).contains("\"evidence\"")
    }

    @Test
    fun `clamps negative and out of range offsets`() {
        val builder = XbParserErrorReportBuilder(contextLines = 1)
        val text = "first\nsecond"

        val entries = builder.build(text, listOf("Bad token at -5", "Bad token at 999"))

        assertThat(entries).hasSize(2)
        assertThat(entries[0].offset).isNull()
        assertThat(entries[0].line).isNull()
        assertThat(entries[1].offset).isEqualTo(999)
        assertThat(entries[1].line).isEqualTo(2)
        assertThat(entries[1].column).isEqualTo(7)
    }
}
