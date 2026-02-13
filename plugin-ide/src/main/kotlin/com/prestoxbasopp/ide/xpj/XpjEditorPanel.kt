package com.prestoxbasopp.ide.xpj

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.nio.file.Path
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel

class XpjEditorPanel(
    private val model: XpjEditorModel,
    private val rootPath: Path,
    private val onDirtyChanged: () -> Unit,
) : JPanel(BorderLayout()) {
    private val sectionModel = DefaultListModel<String>()
    private val sectionList = JList(sectionModel)
    private val entriesTableModel = EntriesTableModel(emptyList())
    private val entriesTable = JTable(entriesTableModel)
    private val suggestionProvider = XpjFileSuggestions(rootPath)
    private val suggestionsBox = JComboBox<String>()
    private val helpTextArea = JTextArea(XpjHelpGlossary.fullHelpText())

    init {
        populateSections()
        add(buildContent(), BorderLayout.CENTER)
    }

    fun refresh() {
        populateSections()
    }

    private fun buildContent(): JTabbedPane {
        val tabs = JTabbedPane()
        tabs.addTab("Structure", buildStructureTab())
        tabs.addTab("Definitions", buildDefinitionsTab())
        tabs.addTab("Help Glossary", JScrollPane(helpTextArea.apply { isEditable = false; lineWrap = true; wrapStyleWord = true }))
        return tabs
    }

    private fun buildStructureTab(): JPanel {
        sectionList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        sectionList.addListSelectionListener {
            val selected = sectionList.selectedValue
            entriesTableModel.entries = selected?.let { model.section(it)?.entries } ?: emptyList()
        }

        val sectionPanel = JPanel(BorderLayout())
        sectionPanel.add(JLabel("Sections"), BorderLayout.NORTH)
        sectionPanel.add(JScrollPane(sectionList), BorderLayout.CENTER)

        val addSectionButton = JButton("Add Section")
        addSectionButton.addActionListener {
            val sectionName = JOptionPane.showInputDialog(this, "Section name", "Add section", JOptionPane.PLAIN_MESSAGE)
            if (!sectionName.isNullOrBlank()) {
                model.addSection(sectionName)
                onDirtyChanged()
                populateSections(select = sectionName)
            }
        }
        sectionPanel.add(addSectionButton, BorderLayout.SOUTH)

        val entriesPanel = JPanel(BorderLayout())
        entriesPanel.add(JLabel("Section Entries"), BorderLayout.NORTH)
        entriesPanel.add(JScrollPane(entriesTable), BorderLayout.CENTER)
        entriesPanel.add(buildEntryControls(), BorderLayout.SOUTH)

        return JPanel(BorderLayout()).apply {
            add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sectionPanel, entriesPanel).apply { resizeWeight = 0.3 }, BorderLayout.CENTER)
        }
    }

    private fun buildEntryControls(): JPanel {
        val panel = JPanel(BorderLayout())
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT))
        val addDefinition = JButton("Add Definition")
        val addSource = JButton("Add Source")
        val removeEntry = JButton("Remove Selected")

        addDefinition.addActionListener {
            val selectedSection = sectionList.selectedValue ?: return@addActionListener
            val key = JOptionPane.showInputDialog(this, "Definition key", "DEBUG") ?: return@addActionListener
            val value = JOptionPane.showInputDialog(this, "Definition value", "YES") ?: return@addActionListener
            model.addDefinition(selectedSection, key, value)
            onDirtyChanged()
            refreshEntries()
        }

        addSource.addActionListener {
            val selectedSection = sectionList.selectedValue ?: return@addActionListener
            val chosen = chooseFileReference() ?: return@addActionListener
            model.addReference(selectedSection, chosen)
            onDirtyChanged()
            refreshEntries()
        }

        removeEntry.addActionListener {
            val selectedSection = sectionList.selectedValue ?: return@addActionListener
            val selectedRow = entriesTable.selectedRow
            if (selectedRow >= 0) {
                model.removeEntry(selectedSection, selectedRow)
                onDirtyChanged()
                refreshEntries()
            }
        }

        buttons.add(addDefinition)
        buttons.add(addSource)
        buttons.add(removeEntry)
        panel.add(buttons, BorderLayout.NORTH)

        val suggestionField = JTextField()
        val suggestionPanel = JPanel(BorderLayout())
        suggestionPanel.add(JLabel("Quick add source with smart suggestions"), BorderLayout.NORTH)
        suggestionsBox.preferredSize = Dimension(380, 26)
        suggestionsBox.isEditable = false
        suggestionPanel.add(suggestionsBox, BorderLayout.CENTER)
        val applySuggestion = JButton("Add Suggested File")
        applySuggestion.addActionListener {
            val selectedSection = sectionList.selectedValue ?: return@addActionListener
            val selected = suggestionsBox.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: return@addActionListener
            model.addReference(selectedSection, selected)
            onDirtyChanged()
            refreshEntries()
        }

        suggestionField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = refreshSuggestions(suggestionField.text)
            override fun removeUpdate(e: DocumentEvent?) = refreshSuggestions(suggestionField.text)
            override fun changedUpdate(e: DocumentEvent?) = refreshSuggestions(suggestionField.text)
        })

        val suggestionControls = JPanel(FlowLayout(FlowLayout.LEFT))
        suggestionControls.add(JLabel("Type file name:"))
        suggestionControls.add(suggestionField)
        suggestionControls.add(applySuggestion)

        val south = JPanel(BorderLayout())
        south.add(suggestionControls, BorderLayout.NORTH)
        south.add(suggestionPanel, BorderLayout.SOUTH)
        panel.add(south, BorderLayout.SOUTH)

        return panel
    }

    private fun chooseFileReference(): String? {
        val chooser = JFileChooser(rootPath.toFile())
        val result = chooser.showOpenDialog(this)
        if (result != JFileChooser.APPROVE_OPTION) {
            return null
        }
        return rootPath.relativize(chooser.selectedFile.toPath()).toString().replace('\\', '/')
    }

    private fun refreshSuggestions(prefix: String) {
        SwingUtilities.invokeLater {
            suggestionsBox.removeAllItems()
            suggestionProvider.suggest(prefix).forEach { suggestionsBox.addItem(it) }
        }
    }

    private fun refreshEntries() {
        entriesTableModel.entries = model.section(sectionList.selectedValue ?: return)?.entries ?: emptyList()
    }

    private fun buildDefinitionsTab(): JPanel {
        val tableModel = object : AbstractTableModel() {
            private var rows: List<Pair<String, String>> = emptyList()

            init {
                rebuild()
            }

            fun rebuild() {
                rows = model.snapshot().sections.flatMap { section ->
                    section.entries.filterIsInstance<XpjEntry.Definition>().map { "${section.name}.${it.key}" to it.value }
                }
                fireTableDataChanged()
            }

            override fun getRowCount(): Int = rows.size
            override fun getColumnCount(): Int = 2
            override fun getColumnName(column: Int): String = if (column == 0) "Definition" else "Value"
            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = if (columnIndex == 0) rows[rowIndex].first else rows[rowIndex].second
        }

        val table = JTable(tableModel)
        return JPanel(BorderLayout()).apply {
            add(JLabel("All definitions (section-qualified)"), BorderLayout.NORTH)
            add(JScrollPane(table), BorderLayout.CENTER)
            add(JButton("Refresh").apply { addActionListener { tableModel.rebuild() } }, BorderLayout.SOUTH)
        }
    }

    private fun populateSections(select: String? = null) {
        sectionModel.removeAllElements()
        model.sectionNames().forEach(sectionModel::addElement)
        val target = select ?: sectionModel.elements().toList().firstOrNull()
        if (!target.isNullOrBlank()) {
            sectionList.setSelectedValue(target, true)
            refreshEntries()
        }
    }

    private class EntriesTableModel(initialEntries: List<XpjEntry>) : AbstractTableModel() {
        var entries: List<XpjEntry> = initialEntries
            set(value) {
                field = value
                fireTableDataChanged()
            }

        override fun getRowCount(): Int = entries.size
        override fun getColumnCount(): Int = 2
        override fun getColumnName(column: Int): String = if (column == 0) "Type" else "Value"

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val entry = entries[rowIndex]
            return when (entry) {
                is XpjEntry.Definition -> if (columnIndex == 0) "Definition" else "${entry.key} = ${entry.value}"
                is XpjEntry.Reference -> if (columnIndex == 0) "Reference" else entry.value
            }
        }
    }
}
