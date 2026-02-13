package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.AbstractTableModel

class UltraDbfMasterToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = UltraDbfMasterPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "Ultra DBF Master", false)
        toolWindow.contentManager.addContent(content)
    }
}

class UltraDbfMasterPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val pathField = JTextField()
    private val tableView = JTable()
    private val status = JLabel("Import a Level-5 DBF file to begin.")
    private val showDeletedToggle = JCheckBox("Show deleted records", true)
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

        add(controls, BorderLayout.NORTH)
        add(JBScrollPane(tableView), BorderLayout.CENTER)
        add(JPanel(BorderLayout()).apply {
            add(actions, BorderLayout.NORTH)
            add(status, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
    }

    private fun importFromPath() {
        try {
            val file = File(pathField.text.trim())
            require(file.exists()) { "File does not exist: ${file.path}" }
            val parsed = UltraDbfCodec.parse(file.readBytes())
            importedFile = file
            model = UltraDbfEditorModel(parsed)
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
        refreshTable()
        status.text = "Added a new row."
    }

    private fun toggleDeleted() {
        val selectedRow = tableView.selectedRow
        val editorModel = model ?: return
        if (selectedRow !in 0 until tableView.rowCount) return
        val sourceRows = editorModel.records(showDeletedToggle.isSelected)
        val record = sourceRows[selectedRow]
        val absoluteIndex = editorModel.records(true).indexOf(record)
        if (absoluteIndex >= 0) {
            editorModel.toggleDeleted(absoluteIndex)
            refreshTable()
        }
    }

    private fun refreshTable() {
        val editorModel = model ?: return
        tableView.model = UltraDbfTableModel(editorModel, showDeletedToggle.isSelected)
    }

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
}

private class UltraDbfTableModel(
    private val editorModel: UltraDbfEditorModel,
    private val includeDeleted: Boolean,
) : AbstractTableModel() {
    private val visibleRecords = editorModel.records(includeDeleted)
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
