package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel

class UltraDbfMasterToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = UltraDbfMasterPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "Ultra DBF Master", false)
        toolWindow.contentManager.addContent(content)
    }
}

class UltraDbfMasterPanel(
    private val project: Project,
    initialDbfPath: String? = null,
) : JPanel(BorderLayout()) {
    private val pathField = JTextField()
    private val tableView = JTable()
    private val status = JLabel("Import a Level-5 DBF file to begin.")
    private val showDeletedToggle = JCheckBox("Show deleted records", true)
    private val tabs = JBTabbedPane()
    private val cardPanel = JPanel(BorderLayout())
    private val cardForm = JPanel(GridLayout(0, 2, 8, 8))
    private val filterPanel = JPanel(GridLayout(0, 2, 8, 8))
    private val filterInputs = linkedMapOf<String, JTextField>()
    private val cardPageLabel = JLabel("Record 0 / 0", SwingConstants.CENTER)
    private var cardPage = 0
    private var importedFile: File? = null
    private var model: UltraDbfEditorModel? = null

    init {
        val controls = JPanel(BorderLayout()).apply {
            add(pathField, BorderLayout.CENTER)
            add(JButton("Import").apply { addActionListener { importFromPath() } }, BorderLayout.EAST)
        }
        val actions = JPanel().apply {
            add(showDeletedToggle.apply { addActionListener { refreshTable() } })
            add(JButton("Add row").apply { addActionListener { addRow() } })
            add(JButton("Toggle deleted").apply { addActionListener { toggleDeleted() } })
            add(JButton("Save").apply { addActionListener { saveChanges() } })
            add(JButton("Export CSV").apply { addActionListener { exportCsvPreview() } })
            add(JButton("Fullscreen").apply { addActionListener { openFullscreenDialog() } })
        }

        cardPanel.add(cardPageLabel, BorderLayout.NORTH)
        cardPanel.add(JBScrollPane(cardForm), BorderLayout.CENTER)
        cardPanel.add(
            JPanel().apply {
                add(JButton("Prev").apply { addActionListener { moveCardPage(-1) } })
                add(JButton("Next").apply { addActionListener { moveCardPage(1) } })
            },
            BorderLayout.SOUTH,
        )

        tabs.addTab("Table View", JBScrollPane(tableView))
        tabs.addTab("Card View", cardPanel)
        tabs.addTab("Filter View", JBScrollPane(filterPanel))
        tabs.addTab("Reverse Engineering", ReverseEngineeringWorkspacePanel(::reverseEngineeringInput))

        add(controls, BorderLayout.NORTH)
        add(tabs, BorderLayout.CENTER)
        add(JPanel(BorderLayout()).apply {
            add(actions, BorderLayout.NORTH)
            add(status, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)

        if (!initialDbfPath.isNullOrBlank()) {
            pathField.text = initialDbfPath
            importFromPath()
        }
    }

    private fun importFromPath() {
        try {
            val file = File(pathField.text.trim())
            require(file.exists()) { "File does not exist: ${file.path}" }
            val parsed = UltraDbfCodec.parse(file.readBytes())
            importedFile = file
            model = UltraDbfEditorModel(parsed)
            XbDbfModuleCatalogService().registerImport(project, file.toPath(), parsed)
            cardPage = 0
            refreshTable()
            status.text = "Loaded ${parsed.records.size} records and ${parsed.fields.size} fields from ${file.name}."
        } catch (ex: Exception) {
            status.text = "Import failed: ${ex.message}"
            Messages.showErrorDialog(project, ex.message ?: "Unknown error", "Ultra DBF Master")
        }
    }

    private fun addRow() {
        val editorModel = model ?: return
        editorModel.addRecord()
        cardPage = editorModel.records(showDeletedToggle.isSelected).lastIndex.coerceAtLeast(0)
        refreshTable()
        status.text = "Added a new row."
    }

    private fun toggleDeleted() {
        val selectedRow = tableView.selectedRow
        val editorModel = model ?: return
        if (selectedRow !in 0 until tableView.rowCount) return
        val sourceRows = editorModel.filteredRecords(showDeletedToggle.isSelected, activeFilters())
        val record = sourceRows[selectedRow]
        val absoluteIndex = editorModel.records(true).indexOf(record)
        if (absoluteIndex >= 0) {
            editorModel.toggleDeleted(absoluteIndex)
            refreshTable()
        }
    }

    private fun refreshTable() {
        val editorModel = model ?: return
        val filters = activeFilters()
        tableView.model = UltraDbfTableModel(editorModel, showDeletedToggle.isSelected, filters)
        refreshFilterView(editorModel)
        refreshCardView(editorModel, filters)
    }

    private fun refreshCardView(editorModel: UltraDbfEditorModel, filters: Map<String, String>) {
        cardForm.removeAll()
        val visibleRecords = editorModel.filteredRecords(showDeletedToggle.isSelected, filters)
        val visibleIndex = editorModel.visibleRecordIndex(showDeletedToggle.isSelected, filters, cardPage)
        if (visibleIndex < 0) {
            cardPage = 0
            cardPageLabel.text = "Record 0 / 0"
            cardForm.revalidate()
            cardForm.repaint()
            return
        }
        val absoluteRecord = editorModel.records(true).indexOf(visibleRecords[visibleIndex])
        val record = visibleRecords[visibleIndex]
        cardPage = visibleIndex
        cardPageLabel.text = "Record ${visibleIndex + 1} / ${visibleRecords.size}"
        editorModel.fields().forEach { field ->
            cardForm.add(JLabel(field.name))
            cardForm.add(createEditorComponent(field, record.values[field.name].orEmpty(), absoluteRecord))
        }
        cardForm.revalidate()
        cardForm.repaint()
    }

    private fun createEditorComponent(field: DbfFieldDescriptor, value: String, absoluteRecord: Int): JPanel {
        val wrapper = JPanel(BorderLayout())
        when (field.type) {
            DbfFieldType.Logical -> {
                val combo = JComboBox(arrayOf("", "Y", "N", "T", "F", "?"))
                combo.selectedItem = value
                combo.addActionListener {
                    model?.updateValue(absoluteRecord, field.name, combo.selectedItem?.toString().orEmpty())
                    refreshTable()
                }
                wrapper.add(combo, BorderLayout.CENTER)
            }

            DbfFieldType.Memo -> {
                val unknown = JRadioButton("External memo pointer")
                unknown.isSelected = value.isNotBlank()
                unknown.addActionListener {
                    val next = if (unknown.isSelected) "0000000001" else ""
                    model?.updateValue(absoluteRecord, field.name, next)
                    refreshTable()
                }
                wrapper.add(unknown, BorderLayout.CENTER)
            }

            else -> {
                val textField = JTextField(value)
                textField.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) = sync()
                    override fun removeUpdate(e: DocumentEvent?) = sync()
                    override fun changedUpdate(e: DocumentEvent?) = sync()
                    private fun sync() {
                        model?.updateValue(absoluteRecord, field.name, textField.text)
                    }
                })
                wrapper.add(textField, BorderLayout.CENTER)
            }
        }
        return wrapper
    }

    private fun refreshFilterView(editorModel: UltraDbfEditorModel) {
        if (filterInputs.keys == editorModel.fields().map { it.name }) return
        filterPanel.removeAll()
        filterInputs.clear()
        editorModel.fields().forEach { field ->
            val input = JTextField()
            filterInputs[field.name] = input
            filterPanel.add(JLabel("${field.name} contains"))
            filterPanel.add(input)
        }
        filterPanel.add(JLabel())
        filterPanel.add(
            JPanel().apply {
                add(JButton("Apply").apply { addActionListener { cardPage = 0; refreshTable() } })
                add(JButton("Clear").apply {
                    addActionListener {
                        filterInputs.values.forEach { it.text = "" }
                        cardPage = 0
                        refreshTable()
                    }
                })
            },
        )
        filterPanel.revalidate()
        filterPanel.repaint()
    }

    private fun moveCardPage(delta: Int) {
        val editorModel = model ?: return
        val visible = editorModel.filteredRecords(showDeletedToggle.isSelected, activeFilters())
        if (visible.isEmpty()) return
        cardPage = (cardPage + delta).coerceIn(0, visible.lastIndex)
        refreshTable()
    }

    private fun activeFilters(): Map<String, String> = filterInputs.mapValues { it.value.text }

    private fun saveChanges() {
        val file = importedFile ?: return
        val editorModel = model ?: return
        try {
            file.writeBytes(UltraDbfCodec.serialize(editorModel.snapshot()))
            status.text = "Saved ${file.name}."
        } catch (ex: Exception) {
            status.text = "Save failed: ${ex.message}"
            Messages.showErrorDialog(project, ex.message ?: "Unknown error", "Ultra DBF Master")
        }
    }


    private fun exportCsvPreview() {
        val editorModel = model ?: return
        val fields = editorModel.fields()
        val header = fields.joinToString(",") { it.name }
        val rows = editorModel.records(showDeletedToggle.isSelected).joinToString("\n") { record ->
            fields.joinToString(",") { field ->
                val raw = record.values[field.name].orEmpty().replace("\"", "\"\"")
                "\"$raw\""
            }
        }
        Messages.showInfoMessage(project, "$header\n$rows", "Ultra DBF Master CSV Preview")
    }

    private fun openFullscreenDialog() {
        val dialog = JDialog().apply {
            title = "Ultra DBF Master (Fullscreen)"
            isModal = false
            layout = BorderLayout()
            add(JBScrollPane(tableView), BorderLayout.CENTER)
            isUndecorated = true
            val screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
            setBounds(screen)
        }
        dialog.isVisible = true
    }

    private fun reverseEngineeringInput(): ReverseEngineeringInput? {
        val file = importedFile ?: return null
        val snapshot = model?.snapshot() ?: return null
        return ReverseEngineeringInput(
            tableName = file.nameWithoutExtension,
            sourcePath = file.path,
            table = snapshot,
        )
    }
}

private class UltraDbfTableModel(
    private val editorModel: UltraDbfEditorModel,
    private val includeDeleted: Boolean,
    filters: Map<String, String>,
) : AbstractTableModel() {
    private val visibleRecords = editorModel.filteredRecords(includeDeleted, filters)
    private val fields = editorModel.fields()

    override fun getRowCount(): Int = visibleRecords.size

    override fun getColumnCount(): Int = fields.size + 1

    override fun getColumnName(column: Int): String = if (column == 0) "DEL" else fields[column - 1].name

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val record = visibleRecords[rowIndex]
        if (columnIndex == 0) return if (record.deleted) "*" else " "
        val field = fields[columnIndex - 1]
        return record.values[field.name].orEmpty()
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex > 0

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0) return
        val row = visibleRecords[rowIndex]
        val absoluteRow = editorModel.records(true).indexOf(row)
        if (absoluteRow < 0) return
        val field = fields[columnIndex - 1]
        editorModel.updateValue(absoluteRow, field.name, aValue?.toString().orEmpty())
        fireTableCellUpdated(rowIndex, columnIndex)
    }
}
