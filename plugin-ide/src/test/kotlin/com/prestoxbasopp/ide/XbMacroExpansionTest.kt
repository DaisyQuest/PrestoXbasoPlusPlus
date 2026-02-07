package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbMacroExpansionTest {
    @Test
    fun `returns empty list when no macro directives exist`() {
        val builder = XbMacroExpansionBuilder()

        val entries = builder.build("function main()\nreturn")

        assertThat(entries).isEmpty()
    }

    @Test
    fun `expands chained macros and reports ok status`() {
        val builder = XbMacroExpansionBuilder()
        val source = """
            #define A 1
            #define B A + 2
            #define C B + A
            #define EMPTY
        """.trimIndent()

        val entries = builder.build(source)

        assertThat(entries.map { it.name }).containsExactly("A", "B", "C", "EMPTY")
        assertThat(entries[0].expandedValue).isEqualTo("1")
        assertThat(entries[1].expandedValue).isEqualTo("1 + 2")
        assertThat(entries[2].expandedValue).isEqualTo("1 + 2 + 1")
        assertThat(entries[3].expandedValue).isEqualTo("")
        assertThat(entries.map { it.status }).containsOnly(XbMacroExpansionStatus.OK)
    }

    @Test
    fun `marks unresolved references when expansion target is missing`() {
        val builder = XbMacroExpansionBuilder()
        val source = """
            #define A B
        """.trimIndent()

        val entries = builder.build(source)

        assertThat(entries).hasSize(1)
        assertThat(entries[0].expandedValue).isEqualTo("B")
        assertThat(entries[0].status).isEqualTo(XbMacroExpansionStatus.UNRESOLVED_REFERENCE)
    }

    @Test
    fun `marks recursive references when macro expands itself`() {
        val builder = XbMacroExpansionBuilder()
        val source = """
            #define A A
        """.trimIndent()

        val entries = builder.build(source)

        assertThat(entries).hasSize(1)
        assertThat(entries[0].expandedValue).isEqualTo("A")
        assertThat(entries[0].status).isEqualTo(XbMacroExpansionStatus.RECURSIVE_REFERENCE)
    }

    @Test
    fun `ignores invalid define directives`() {
        val builder = XbMacroExpansionBuilder()

        val entries = builder.build("#define\n#define 123 1")

        assertThat(entries).isEmpty()
    }

    @Test
    fun `presenter returns guidance when file is missing or unsupported`() {
        val presenter = XbMacroExpansionPresenter()

        val missingFile = presenter.present(null, null)
        val unsupported = presenter.present("notes.txt", "#define A 1")

        assertThat(missingFile.message).isEqualTo("Select an Xbase++ file to see expanded macros.")
        assertThat(unsupported.message).isEqualTo("Expanded macros are available for .xb and .prg files only.")
        assertThat(missingFile.entries).isEmpty()
        assertThat(unsupported.entries).isEmpty()
    }

    @Test
    fun `presenter reports when no macros are defined`() {
        val presenter = XbMacroExpansionPresenter()

        val presentation = presenter.present("sample.prg", "function main()\nreturn")

        assertThat(presentation.entries).isEmpty()
        assertThat(presentation.message).isEqualTo("No macros defined in sample.prg.")
    }

    @Test
    fun `presenter omits message when macros are available`() {
        val presenter = XbMacroExpansionPresenter()
        val source = "#define A 1"

        val presentation = presenter.present("sample.prg", source)

        assertThat(presentation.entries).hasSize(1)
        assertThat(presentation.message).isNull()
    }

    @Test
    fun `file support recognizes xbase extensions`() {
        assertThat(XbMacroExpansionFileSupport.isXbaseFileName("file.xb")).isTrue()
        assertThat(XbMacroExpansionFileSupport.isXbaseFileName("file.prg")).isTrue()
        assertThat(XbMacroExpansionFileSupport.isXbaseFileName("file.txt")).isFalse()
        assertThat(XbMacroExpansionFileSupport.isXbaseFileName(null)).isFalse()
    }

    @Test
    fun `table model exposes macro columns`() {
        val model = XbMacroExpansionTableModel()
        model.entries = listOf(
            XbMacroExpansionEntry(
                name = "A",
                rawValue = "1",
                expandedValue = "1",
                status = XbMacroExpansionStatus.OK,
            ),
        )

        assertThat(model.columnCount).isEqualTo(4)
        assertThat(model.getColumnName(0)).isEqualTo("Macro")
        assertThat(model.getColumnName(1)).isEqualTo("Raw")
        assertThat(model.getColumnName(2)).isEqualTo("Expanded")
        assertThat(model.getColumnName(3)).isEqualTo("Status")
        assertThat(model.getValueAt(0, 0)).isEqualTo("A")
        assertThat(model.getValueAt(0, 1)).isEqualTo("1")
        assertThat(model.getValueAt(0, 2)).isEqualTo("1")
        assertThat(model.getValueAt(0, 3)).isEqualTo("OK")
    }
}
