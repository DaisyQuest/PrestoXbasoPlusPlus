package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import java.nio.file.Files
import java.nio.file.Path

class ReverseEngineeringWorkspacePanelTest {
    @Test
    fun `analyze warns when no dbf input is available`() {
        val panel = ReverseEngineeringWorkspacePanel(inputProvider = { null })

        clickButton(panel, "reverse.analyze")

        assertThat(findAreaContaining(panel, "Blocking: import a DBF file before running Analyze.")).isNotNull()
        assertThat(findAreaContaining(panel, "Analyze failed: no DBF input is currently loaded.")).isNotNull()
    }

    @Test
    fun `generate warns when analysis has not been run`() {
        val panel = ReverseEngineeringWorkspacePanel(inputProvider = { null })

        clickButton(panel, "reverse.generate")

        assertThat(findAreaContaining(panel, "Blocking: run Analyze before Generate.")).isNotNull()
        assertThat(findAreaContaining(panel, "Generate failed: metadata is missing. Run Analyze first.")).isNotNull()
    }

    @Test
    fun `analyze populates metadata mapping relation tabs and input title`() {
        val table = DbfTable(
            header = DbfHeader(3, 124, 2, 1, 2, 100, 10, 0, 0, false, 0),
            fields = listOf(
                DbfFieldDescriptor("ID", DbfFieldType.Numeric, 8, 0, 0, false),
                DbfFieldDescriptor("OWNER_ID", DbfFieldType.Numeric, 8, 0, 0, false),
                DbfFieldDescriptor("NAME", DbfFieldType.Character, 20, 0, 0, false),
            ),
            records = mutableListOf(
                DbfRecord(false, mutableMapOf("ID" to "1", "OWNER_ID" to "7", "NAME" to "DOG")),
                DbfRecord(false, mutableMapOf("ID" to "2", "OWNER_ID" to "", "NAME" to "DOG")),
            ),
        )
        val panel = ReverseEngineeringWorkspacePanel(inputProvider = {
            ReverseEngineeringInput("DOG_TABLE", "fixtures/dog.dbf", table)
        })

        clickButton(panel, "reverse.analyze")

        assertThat(findAreaContaining(panel, "Table: DOG_TABLE")).isNotNull()
        assertThat(findAreaContaining(panel, "Class: DOG_TABLE")).isNotNull()
        assertThat(findAreaContaining(panel, "DOG_TABLE.OWNER_ID -> OWNER.ID (MANY_TO_ONE)")).isNotNull()
        assertThat(findAreaContaining(panel, "Loaded input table: DOG_TABLE")).isNotNull()
        assertThat(findAreaContaining(panel, "Profile: READ_ONLY")).isNotNull()
        assertThat(findAreaContaining(panel, "Awaiting generation. Output directory: generated")).isNotNull()
        assertThat(findAreaContaining(panel, "Analysis complete. Use Generate to render class output.")).isNotNull()
        assertThat(findAreaContaining(panel, "Analyze complete: DOG_TABLE (3 fields, 2 records).")).isNotNull()

        val tabs = panel.findByType(JTabbedPane::class.java).first { it.tabCount == ReverseEngineeringTab.entries.size }
        assertThat(tabs.getTitleAt(1)).contains("Inputs").contains("DOG_TABLE")
    }

    @Test
    fun `generate renders methods according to selected profile and alias toggle`(@TempDir tempDir: Path) {
        val table = DbfTable(
            header = DbfHeader(3, 124, 2, 1, 1, 100, 10, 0, 0, false, 0),
            fields = listOf(
                DbfFieldDescriptor("ID", DbfFieldType.Numeric, 8, 0, 0, false),
                DbfFieldDescriptor("OWNER_ID", DbfFieldType.Numeric, 8, 0, 0, false),
            ),
            records = mutableListOf(
                DbfRecord(false, mutableMapOf("ID" to "1", "OWNER_ID" to "2")),
            ),
        )
        val dbfPath = tempDir.resolve("fixtures").resolve("dog.dbf")
        Files.createDirectories(dbfPath.parent)
        Files.writeString(dbfPath, "placeholder")

        val panel = ReverseEngineeringWorkspacePanel(inputProvider = {
            ReverseEngineeringInput("DOG_TABLE", dbfPath.toString(), table)
        })

        clickButton(panel, "reverse.analyze")
        selectProfile(panel, ApiProfile.READ_ONLY)
        findComponentByName(panel, "reverse.alias", JCheckBox::class.java).isSelected = false
        findComponentByName(panel, "reverse.outputDir", JTextField::class.java).text = "out/reverse"

        clickButton(panel, "reverse.generate")

        val preview = findAreaContaining(panel, "CLASS DogTable")
        assertThat(preview).isNotNull()
        val previewText = preview!!.text
        assertThat(previewText).contains("#define DOGTABLE_FIELD_ID \"ID\"")
        assertThat(previewText).contains("METHOD load(...)")
        assertThat(previewText).contains("METHOD findBy(...)")
        assertThat(previewText).doesNotContain("METHOD insert(...)")
        assertThat(previewText).doesNotContain("INLINE")
        assertThat(findAreaContaining(panel, "Profile: READ_ONLY")).isNotNull()
        assertThat(findAreaContaining(panel, "Method aliases: disabled")).isNotNull()
        assertThat(findAreaContaining(panel, "Output directory: out/reverse")).isNotNull()
        assertThat(findAreaContaining(panel, "Artifacts generated: 1")).isNotNull()
        assertThat(findAreaContaining(panel, "Files written: 1")).isNotNull()
        assertThat(findAreaContaining(panel, "OK: generation completed without warnings.")).isNotNull()
        assertThat(findAreaContaining(panel, "Generate complete: 1 artifacts into out/reverse.")).isNotNull()

        val outputFile = tempDir.resolve("fixtures").resolve("out/reverse/DogTable.prg")
        assertThat(outputFile).exists()
        assertThat(Files.readString(outputFile)).contains("CLASS DogTable")
    }

    @Test
    fun `generate surfaces non-blocking warnings when generation skips invalid class`() {
        val table = DbfTable(
            header = DbfHeader(3, 124, 2, 1, 1, 100, 10, 0, 0, false, 0),
            fields = listOf(
                DbfFieldDescriptor("ID", DbfFieldType.Numeric, 8, 0, 0, false),
            ),
            records = mutableListOf(
                DbfRecord(false, mutableMapOf("ID" to "1")),
            ),
        )
        val panel = ReverseEngineeringWorkspacePanel(inputProvider = {
            ReverseEngineeringInput("___", "fixtures/blank.dbf", table)
        })

        clickButton(panel, "reverse.analyze")
        clickButton(panel, "reverse.generate")

        assertThat(findAreaContaining(panel, "Non-blocking warnings:")).isNotNull()
        assertThat(findAreaContaining(panel, "Skipped table '___' due to blank class name.")).isNotNull()
        assertThat(findAreaContaining(panel, "Generate complete: 0 artifacts into generated.")).isNotNull()
    }

    private fun clickButton(panel: ReverseEngineeringWorkspacePanel, name: String) {
        findComponentByName(panel, name, JButton::class.java).doClick()
    }

    private fun selectProfile(panel: ReverseEngineeringWorkspacePanel, profile: ApiProfile) {
        @Suppress("UNCHECKED_CAST")
        val combo = findComponentByName(panel, "reverse.profile", JComboBox::class.java) as JComboBox<ApiProfile>
        combo.selectedItem = profile
    }

    private fun findAreaContaining(panel: ReverseEngineeringWorkspacePanel, text: String): JTextArea? =
        panel.findByType(JTextArea::class.java).firstOrNull { it.text.contains(text) }

    private fun <T : java.awt.Component> findComponentByName(
        panel: ReverseEngineeringWorkspacePanel,
        name: String,
        type: Class<T>,
    ): T = panel.findByType(type).first { it.name == name }

    private fun <T : java.awt.Component> java.awt.Container.findByType(type: Class<T>): List<T> =
        components.flatMap { child ->
            buildList {
                if (type.isInstance(child)) add(type.cast(child))
                if (child is java.awt.Container) addAll(child.findByType(type))
            }
        }
}
