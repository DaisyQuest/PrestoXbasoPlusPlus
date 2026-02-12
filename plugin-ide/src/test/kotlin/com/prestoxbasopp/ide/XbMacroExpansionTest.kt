package com.prestoxbasopp.ide

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

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
    fun `parse definitions keeps only valid directives`() {
        val builder = XbMacroExpansionBuilder()

        val definitions = builder.parseDefinitions(
            """
            #define GOOD 1
            #define
            #define OTHER hello
            """.trimIndent(),
        )

        assertThat(definitions.map { it.name }).containsExactly("GOOD", "OTHER")
        assertThat(definitions.map { it.rawValue }).containsExactly("1", "hello")
    }

    @Test
    fun `presenter returns guidance when file is missing or unsupported`() {
        val presenter = XbMacroExpansionPresenter()

        val missingFile = presenter.present(null, null, null, null)
        val unsupported = presenter.present("notes.txt", "/project/notes.txt", "#define A 1", "/project")

        assertThat(missingFile.message).isEqualTo("Select an Xbase++ file to see expanded macros.")
        assertThat(unsupported.message).isEqualTo("Expanded macros are available for .xb and .prg files only.")
        assertThat(missingFile.headerInsight.message).isEqualTo("Select an Xbase++ file to see header insight.")
        assertThat(unsupported.headerInsight.message).isEqualTo("Header insight is available for .xb and .prg files only.")
        assertThat(missingFile.entries).isEmpty()
        assertThat(unsupported.entries).isEmpty()
    }

    @Test
    fun `presenter reports when no macros are defined while keeping header insight`() {
        val presenter = XbMacroExpansionPresenter(
            headerInsightBuilder = XbHeaderInsightBuilder(
                fileSystem = FakeHeaderFileSystem(
                    mapOf("/project/inc/main.ch" to "#define A 10"),
                ),
            ),
        )

        val presentation = presenter.present(
            fileName = "sample.prg",
            filePath = "/project/src/sample.prg",
            text = "#include \"../inc/main.ch\"\nfunction main()\nreturn",
            projectBasePath = "/project",
        )

        assertThat(presentation.entries).isEmpty()
        assertThat(presentation.message).isEqualTo("No macros defined in sample.prg.")
        assertThat(presentation.headerInsight.definitions).hasSize(1)
        assertThat(presentation.headerInsight.message).isNull()
    }

    @Test
    fun `presenter omits message when macros are available`() {
        val presenter = XbMacroExpansionPresenter()
        val source = "#define A 1"

        val presentation = presenter.present("sample.prg", "/project/sample.prg", source, "/project")

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
    fun `header insight builds include status definitions and conflicts`() {
        val filesystem = FakeHeaderFileSystem(
            mapOf(
                "/project/includes/one.ch" to "#define CONST 1\n#define SHARED alpha",
                "/project/includes/two.ch" to "#define SHARED beta\n#define TWO 2",
            ),
        )
        val builder = XbHeaderInsightBuilder(fileSystem = filesystem)

        val presentation = builder.build(
            source = """
                #include "../includes/one.ch"
                #include "../includes/missing.ch"
                #include "../includes/two.ch"
            """.trimIndent(),
            sourceFilePath = "/project/src/main.prg",
            projectBasePath = "/project",
        )

        assertThat(presentation.includes).hasSize(3)
        assertThat(presentation.includes[0].status).isEqualTo(XbHeaderIncludeStatus.LOADED)
        assertThat(presentation.includes[1].status).isEqualTo(XbHeaderIncludeStatus.MISSING)
        assertThat(presentation.includes[1].resolvedPath).isNull()
        assertThat(presentation.includes[2].status).isEqualTo(XbHeaderIncludeStatus.LOADED)

        assertThat(presentation.definitions.map { it.name }).containsExactly("CONST", "SHARED", "TWO")
        assertThat(presentation.conflicts).hasSize(1)
        assertThat(presentation.conflicts[0].name).isEqualTo("SHARED")
        assertThat(presentation.conflicts[0].firstValue).isEqualTo("alpha")
        assertThat(presentation.conflicts[0].secondValue).isEqualTo("beta")
        assertThat(presentation.message).isNull()
    }

    @Test
    fun `header insight returns no include message when directives absent`() {
        val builder = XbHeaderInsightBuilder(fileSystem = FakeHeaderFileSystem(emptyMap()))

        val presentation = builder.build(
            source = "function main()\nreturn",
            sourceFilePath = "/project/src/main.prg",
            projectBasePath = "/project",
        )

        assertThat(presentation.includes).isEmpty()
        assertThat(presentation.definitions).isEmpty()
        assertThat(presentation.conflicts).isEmpty()
        assertThat(presentation.message).isEqualTo("No #include directives found.")
    }

    @Test
    fun `header insight shows no definitions message when includes have no defines`() {
        val builder = XbHeaderInsightBuilder(
            fileSystem = FakeHeaderFileSystem(
                mapOf("/project/includes/empty.ch" to "// just comments"),
            ),
        )

        val presentation = builder.build(
            source = "#include \"../includes/empty.ch\"",
            sourceFilePath = "/project/src/main.prg",
            projectBasePath = "/project",
        )

        assertThat(presentation.includes).hasSize(1)
        assertThat(presentation.includes[0].status).isEqualTo(XbHeaderIncludeStatus.LOADED)
        assertThat(presentation.definitions).isEmpty()
        assertThat(presentation.conflicts).isEmpty()
        assertThat(presentation.message).isEqualTo("No header definitions found in included files.")
    }

    @Test
    fun `header insight resolves relative and absolute includes`() {
        val builder = XbHeaderInsightBuilder(
            fileSystem = FakeHeaderFileSystem(
                mapOf(
                    "/project/inc/rel.ch" to "#define REL 1",
                    "/abs/global.ch" to "#define ABS 2",
                ),
            ),
        )

        val presentation = builder.build(
            source = "#include \"../inc/rel.ch\"\n#include \"/abs/global.ch\"",
            sourceFilePath = "/project/src/main.prg",
            projectBasePath = "/project",
        )

        assertThat(presentation.includes.map { it.status }).containsExactly(
            XbHeaderIncludeStatus.LOADED,
            XbHeaderIncludeStatus.LOADED,
        )
        assertThat(presentation.definitions.map { it.name }).containsExactly("REL", "ABS")
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
        assertThat(model.getColumnName(4)).isEqualTo("")
        assertThat(model.getValueAt(0, 99)).isEqualTo("")
        assertThat(model.isCellEditable(0, 0)).isFalse()
    }

    @Test
    fun `header include table model exposes include columns`() {
        val model = XbHeaderIncludeTableModel()
        model.entries = listOf(
            XbHeaderIncludeEntry("inc.ch", "/project/inc.ch", XbHeaderIncludeStatus.LOADED),
        )

        assertThat(model.columnCount).isEqualTo(3)
        assertThat(model.getColumnName(0)).isEqualTo("Include")
        assertThat(model.getColumnName(1)).isEqualTo("Resolved Path")
        assertThat(model.getColumnName(2)).isEqualTo("Status")
        assertThat(model.getValueAt(0, 0)).isEqualTo("inc.ch")
        assertThat(model.getValueAt(0, 1)).isEqualTo("/project/inc.ch")
        assertThat(model.getValueAt(0, 2)).isEqualTo("Loaded")
        assertThat(model.getColumnName(4)).isEqualTo("")
        assertThat(model.getValueAt(0, 4)).isEqualTo("")
        assertThat(model.isCellEditable(0, 0)).isFalse()
    }

    @Test
    fun `header definition table model exposes definition columns`() {
        val model = XbHeaderDefinitionTableModel()
        model.entries = listOf(
            XbHeaderDefinitionEntry("A", "1", XbHeaderDefinitionKind.DEFINE, "main.ch"),
        )

        assertThat(model.columnCount).isEqualTo(4)
        assertThat(model.getColumnName(0)).isEqualTo("Name")
        assertThat(model.getColumnName(1)).isEqualTo("Value")
        assertThat(model.getColumnName(2)).isEqualTo("Kind")
        assertThat(model.getColumnName(3)).isEqualTo("Source")
        assertThat(model.getValueAt(0, 0)).isEqualTo("A")
        assertThat(model.getValueAt(0, 1)).isEqualTo("1")
        assertThat(model.getValueAt(0, 2)).isEqualTo("define")
        assertThat(model.getValueAt(0, 3)).isEqualTo("main.ch")
        assertThat(model.getColumnName(9)).isEqualTo("")
        assertThat(model.getValueAt(0, 9)).isEqualTo("")
        assertThat(model.isCellEditable(0, 0)).isFalse()
    }

    @Test
    fun `header conflict table model exposes conflict columns`() {
        val model = XbHeaderConflictTableModel()
        model.entries = listOf(
            XbHeaderConflictEntry("A", "1", "one.ch", "2", "two.ch"),
        )

        assertThat(model.columnCount).isEqualTo(6)
        assertThat(model.getColumnName(0)).isEqualTo("Name")
        assertThat(model.getColumnName(1)).isEqualTo("First Value")
        assertThat(model.getColumnName(2)).isEqualTo("First Source")
        assertThat(model.getColumnName(3)).isEqualTo("Second Value")
        assertThat(model.getColumnName(4)).isEqualTo("Second Source")
        assertThat(model.getColumnName(5)).isEqualTo("Status")
        assertThat(model.getValueAt(0, 0)).isEqualTo("A")
        assertThat(model.getValueAt(0, 1)).isEqualTo("1")
        assertThat(model.getValueAt(0, 2)).isEqualTo("one.ch")
        assertThat(model.getValueAt(0, 3)).isEqualTo("2")
        assertThat(model.getValueAt(0, 4)).isEqualTo("two.ch")
        assertThat(model.getValueAt(0, 5)).isEqualTo("Conflict")
        assertThat(model.getColumnName(7)).isEqualTo("")
        assertThat(model.getValueAt(0, 7)).isEqualTo("")
        assertThat(model.isCellEditable(0, 0)).isFalse()
    }

    private class FakeHeaderFileSystem(private val files: Map<String, String>) : XbHeaderFileSystem {
        override fun pathOf(rawPath: String): Path = Path.of(rawPath)

        override fun exists(path: Path): Boolean = files.containsKey(path.normalize().toString())

        override fun readText(path: Path): String = files[path.normalize().toString()]
            ?: error("Missing fake file: $path")
    }
}
