package com.prestoxbasopp.ide

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JTabbedPane
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class XbMacroExpansionPanel {
    private val macroTableModel = XbMacroExpansionTableModel()
    private val includesTableModel = XbHeaderIncludeTableModel()
    private val definitionsTableModel = XbHeaderDefinitionTableModel()
    private val conflictsTableModel = XbHeaderConflictTableModel()

    private val macroTable = createReadOnlyTable(macroTableModel, "No macro expansions to display.")
    private val includesTable = createReadOnlyTable(includesTableModel, "No header includes to display.")
    private val definitionsTable = createReadOnlyTable(definitionsTableModel, "No header definitions to display.")
    private val conflictsTable = createReadOnlyTable(conflictsTableModel, "No conflicting definitions to display.")

    private val messageLabel = JBLabel()
    private val headerInsightMessageLabel = JBLabel()

    val component: JPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(8)
        add(messageLabel, BorderLayout.NORTH)
        add(
            JTabbedPane().apply {
                addTab("Macros", JBScrollPane(macroTable))
                addTab(
                    "Header Insight",
                    JPanel(BorderLayout()).apply {
                        add(headerInsightMessageLabel, BorderLayout.NORTH)
                        add(
                            JTabbedPane().apply {
                                addTab("Includes", JBScrollPane(includesTable))
                                addTab("Definitions", JBScrollPane(definitionsTable))
                                addTab("Conflicts", JBScrollPane(conflictsTable))
                            },
                            BorderLayout.CENTER,
                        )
                    },
                )
            },
            BorderLayout.CENTER,
        )
    }

    fun render(presentation: XbMacroExpansionPresentation) {
        macroTableModel.entries = presentation.entries
        includesTableModel.entries = presentation.headerInsight.includes
        definitionsTableModel.entries = presentation.headerInsight.definitions
        conflictsTableModel.entries = presentation.headerInsight.conflicts

        messageLabel.text = presentation.message ?: ""
        messageLabel.isVisible = presentation.message != null

        headerInsightMessageLabel.text = presentation.headerInsight.message ?: ""
        headerInsightMessageLabel.isVisible = presentation.headerInsight.message != null
    }

    private fun createReadOnlyTable(model: AbstractTableModel, emptyText: String): JBTable {
        return JBTable(model).apply {
            setShowGrid(true)
            rowSelectionAllowed = false
            cellSelectionEnabled = false
            autoCreateRowSorter = true
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            tableHeader.reorderingAllowed = false
            this.emptyText.text = emptyText
        }
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

class XbHeaderIncludeTableModel : AbstractTableModel() {
    var entries: List<XbHeaderIncludeEntry> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String = when (column) {
        0 -> "Include"
        1 -> "Resolved Path"
        2 -> "Status"
        else -> ""
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = entries[rowIndex]
        return when (columnIndex) {
            0 -> entry.includeTarget
            1 -> entry.resolvedPath ?: ""
            2 -> entry.status.displayName
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}

class XbHeaderDefinitionTableModel : AbstractTableModel() {
    var entries: List<XbHeaderDefinitionEntry> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = 4

    override fun getColumnName(column: Int): String = when (column) {
        0 -> "Name"
        1 -> "Value"
        2 -> "Kind"
        3 -> "Source"
        else -> ""
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = entries[rowIndex]
        return when (columnIndex) {
            0 -> entry.name
            1 -> entry.value
            2 -> entry.kind.displayName
            3 -> entry.sourceFile
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}

class XbHeaderConflictTableModel : AbstractTableModel() {
    var entries: List<XbHeaderConflictEntry> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = 6

    override fun getColumnName(column: Int): String = when (column) {
        0 -> "Name"
        1 -> "First Value"
        2 -> "First Source"
        3 -> "Second Value"
        4 -> "Second Source"
        5 -> "Status"
        else -> ""
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = entries[rowIndex]
        return when (columnIndex) {
            0 -> entry.name
            1 -> entry.firstValue
            2 -> entry.firstSourceFile
            3 -> entry.secondValue
            4 -> entry.secondSourceFile
            5 -> entry.status.displayName
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}
