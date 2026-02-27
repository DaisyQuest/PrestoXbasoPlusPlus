package com.prestoxbasopp.ui

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
import javax.swing.JTextArea
import javax.swing.SpinnerNumberModel

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
    private val overrideTextArea = JTextArea(8, 48)

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

        add(JLabel("Manual word overrides (one per line: word=STYLE)"), constraints.withRow(row))
        row++
        add(JScrollPane(overrideTextArea), constraints.withRow(row))
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
        overrideTextArea.text = state.highlightingPreferences.wordOverrides.entries
            .sortedBy { it.key }
            .joinToString("\n") { (word, style) -> "$word=${style.name}" }
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
                wordOverrides = parseOverrides(overrideTextArea.text),
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

    private fun parseOverrides(rawText: String): Map<String, XbHighlightCategory> {
        return rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val delimiter = line.indexOf('=')
                if (delimiter <= 0 || delimiter == line.lastIndex) {
                    return@mapNotNull null
                }
                val word = line.substring(0, delimiter).trim().lowercase()
                val styleName = line.substring(delimiter + 1).trim().uppercase()
                val style = XbHighlightCategory.entries.find { it.name == styleName } ?: return@mapNotNull null
                word to style
            }
            .toMap()
    }

    private fun GridBagConstraints.withRow(row: Int): GridBagConstraints {
        return (clone() as GridBagConstraints).apply {
            gridy = row
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
