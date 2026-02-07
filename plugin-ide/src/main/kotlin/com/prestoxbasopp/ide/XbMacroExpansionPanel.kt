package com.prestoxbasopp.ide

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class XbMacroExpansionPanel {
    private val tableModel = XbMacroExpansionTableModel()
    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowSelectionAllowed = false
        cellSelectionEnabled = false
        autoCreateRowSorter = true
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        tableHeader.reorderingAllowed = false
        emptyText.text = "No macro expansions to display."
    }
    private val messageLabel = JBLabel()
    val component: JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(8)
        add(messageLabel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
    }

    fun render(presentation: XbMacroExpansionPresentation) {
        tableModel.entries = presentation.entries
        messageLabel.text = presentation.message ?: ""
        messageLabel.isVisible = presentation.message != null
    }
}

class XbMacroExpansionTableModel : AbstractTableModel() {
    var entries: List<XbMacroExpansionEntry> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = 4

    override fun getColumnName(column: Int): String = when (column) {
        0 -> "Macro"
        1 -> "Raw"
        2 -> "Expanded"
        3 -> "Status"
        else -> ""
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = entries[rowIndex]
        return when (columnIndex) {
            0 -> entry.name
            1 -> entry.rawValue
            2 -> entry.expandedValue
            3 -> entry.status.displayName
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}
