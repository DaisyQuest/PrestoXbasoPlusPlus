package com.prestoxbasopp.ui

import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTable
import javax.swing.DefaultCellEditor
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel
import javax.swing.table.AbstractTableModel

class XbUiSettingsPanel(
    private val navigateToEditorThemeSettings: (() -> Unit)? = null,
) : JPanel(GridBagLayout()), XbUiSettingsView {
    private val enableSyntaxHighlightingCheckbox = JCheckBox("Enable syntax highlighting")
    private val showInlayHintsCheckbox = JCheckBox("Show inlay hints")
    private val tabSizeSpinner = JSpinner(SpinnerNumberModel(DEFAULT_TAB_SIZE, MIN_TAB_SIZE, MAX_TAB_SIZE, 1))
    private val completionLimitSpinner = JSpinner(
        SpinnerNumberModel(DEFAULT_COMPLETION_LIMIT, MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT, 1),
    )
    private val styleSelectors: Map<XbHighlightCategory, JComboBox<XbHighlightCategory>> =
        XbHighlightCategory.entries.associateWith {
            JComboBox(XbHighlightCategory.entries.toTypedArray())
        }
    private val overrideTableModel = OverrideTableModel()
    private val overrideTable = JTable(overrideTableModel).apply {
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        fillsViewportHeight = true
        columnModel.getColumn(1).cellEditor = DefaultCellEditor(JComboBox(XbHighlightCategory.entries.toTypedArray()))
    }

    init {
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            weightx = 1.0
            insets = Insets(4, 4, 4, 4)
        }

        add(enableSyntaxHighlightingCheckbox, constraints.withRow(0))
        add(showInlayHintsCheckbox, constraints.withRow(1))
        add(JLabel("Tab size"), constraints.withRow(2))
        add(tabSizeSpinner, constraints.withRow(3))
        add(JLabel("Completion limit"), constraints.withRow(4))
        add(completionLimitSpinner, constraints.withRow(5))
        add(JLabel("Highlight style mapping (source → target):"), constraints.withRow(6))

        var row = 7
        XbHighlightCategory.entries.forEach { category ->
            add(JLabel(category.name), constraints.withRow(row))
            row++
            add(styleSelectors.getValue(category), constraints.withRow(row))
            row++
        }

        add(JLabel("Manual word overrides"), constraints.withRow(row))
        row++
        add(JScrollPane(overrideTable), constraints.withRow(row).withWeight(1.0).withFill(GridBagConstraints.BOTH))
        row++
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(JButton("Add override").apply {
                    addActionListener { overrideTableModel.addEmptyRow() }
                })
                add(JButton("Remove selected").apply {
                    addActionListener {
                        val selectedRows = overrideTable.selectedRows
                            .asSequence()
                            .map { overrideTable.convertRowIndexToModel(it) }
                            .toList()
                        overrideTableModel.removeRows(selectedRows)
                    }
                })
            },
            constraints.withRow(row),
        )
        row++
        add(
            JButton("Open Editor Color Scheme Settings").apply {
                addActionListener { navigateToEditorThemeSettings?.invoke() }
            },
            constraints.withRow(row),
        )
    }

    override fun render(state: XbUiSettingsState) {
        enableSyntaxHighlightingCheckbox.isSelected = state.enableSyntaxHighlighting
        showInlayHintsCheckbox.isSelected = state.showInlayHints
        tabSizeSpinner.value = state.tabSize
        completionLimitSpinner.value = state.completionLimit
        XbHighlightCategory.entries.forEach { category ->
            styleSelectors.getValue(category).selectedItem = state.highlightingPreferences.styleMappings[category] ?: category
        }
        overrideTableModel.setOverrides(state.highlightingPreferences.wordOverrides)
    }

    override fun currentState(): XbUiSettingsState {
        val styleMappings = XbHighlightCategory.entries.associateWith { category ->
            styleSelectors.getValue(category).selectedItem as? XbHighlightCategory ?: category
        }
        return XbUiSettingsState(
            enableSyntaxHighlighting = enableSyntaxHighlightingCheckbox.isSelected,
            showInlayHints = showInlayHintsCheckbox.isSelected,
            tabSize = (tabSizeSpinner.value as Number).toInt(),
            completionLimit = (completionLimitSpinner.value as Number).toInt(),
            highlightingPreferences = XbHighlightingPreferences(
                styleMappings = styleMappings,
                wordOverrides = overrideTableModel.toOverridesMap(),
            ).withNormalizedOverrides(),
        )
    }

    fun snapshot(): String {
        val state = currentState()
        return buildString {
            append("enableSyntaxHighlighting=").append(state.enableSyntaxHighlighting).append('\n')
            append("showInlayHints=").append(state.showInlayHints).append('\n')
            append("tabSize=").append(state.tabSize).append('\n')
            append("completionLimit=").append(state.completionLimit).append('\n')
            append("styleMappings=")
                .append(state.highlightingPreferences.styleMappings.entries.sortedBy { it.key.name }
                    .joinToString(",") { "${it.key.name}->${it.value.name}" })
                .append('\n')
            append("wordOverrides=")
                .append(state.highlightingPreferences.wordOverrides.entries.sortedBy { it.key }
                    .joinToString(",") { "${it.key}=${it.value.name}" })
        }
    }

    internal fun updateOverrideRow(index: Int, word: String, category: XbHighlightCategory) {
        overrideTableModel.updateRow(index, word, category)
    }

    internal fun addOverrideRow(word: String, category: XbHighlightCategory) {
        overrideTableModel.addRow(word, category)
    }

    private fun GridBagConstraints.withRow(row: Int): GridBagConstraints {
        return (clone() as GridBagConstraints).apply {
            gridy = row
        }
    }

    private fun GridBagConstraints.withWeight(weightY: Double): GridBagConstraints {
        return (clone() as GridBagConstraints).apply {
            this.weighty = weightY
        }
    }

    private fun GridBagConstraints.withFill(fillType: Int): GridBagConstraints {
        return (clone() as GridBagConstraints).apply {
            fill = fillType
        }
    }

    private data class OverrideRow(
        var word: String,
        var category: XbHighlightCategory,
    )

    private class OverrideTableModel : AbstractTableModel() {
        private val rows = mutableListOf<OverrideRow>()

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = 2

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Word"
                1 -> "Style"
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> row.word
                1 -> row.category
                else -> ""
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val row = rows[rowIndex]
            when (columnIndex) {
                0 -> row.word = (aValue as? String).orEmpty()
                1 -> row.category = aValue as? XbHighlightCategory ?: row.category
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }

        fun setOverrides(overrides: Map<String, XbHighlightCategory>) {
            rows.clear()
            overrides.entries
                .sortedBy { it.key }
                .forEach { (word, category) -> rows += OverrideRow(word, category) }
            fireTableDataChanged()
        }

        fun toOverridesMap(): Map<String, XbHighlightCategory> {
            return rows.asSequence()
                .map { it.word.trim().lowercase() to it.category }
                .filter { it.first.isNotEmpty() }
                .toMap()
        }

        fun addEmptyRow() {
            addRow("", XbHighlightCategory.entries.first())
        }

        fun addRow(word: String, category: XbHighlightCategory) {
            rows += OverrideRow(word, category)
            fireTableRowsInserted(rows.lastIndex, rows.lastIndex)
        }

        fun removeRows(indices: List<Int>) {
            val distinct = indices.distinct().sortedDescending()
            distinct.forEach { index ->
                if (index in rows.indices) {
                    rows.removeAt(index)
                }
            }
            fireTableDataChanged()
        }

        fun updateRow(index: Int, word: String, category: XbHighlightCategory) {
            if (index !in rows.indices) {
                return
            }
            rows[index] = OverrideRow(word, category)
            fireTableRowsUpdated(index, index)
        }
    }

    companion object {
        private val DEFAULT_TAB_SIZE = XbUiSettingsState().tabSize
        private val DEFAULT_COMPLETION_LIMIT = XbUiSettingsState().completionLimit
        private const val MIN_TAB_SIZE = XbUiSettingsStore.MIN_TAB_SIZE
        private const val MAX_TAB_SIZE = XbUiSettingsStore.MAX_TAB_SIZE
        private const val MIN_COMPLETION_LIMIT = XbUiSettingsStore.MIN_COMPLETION_LIMIT
        private const val MAX_COMPLETION_LIMIT = XbUiSettingsStore.MAX_COMPLETION_LIMIT
    }
}
