package com.prestoxbasopp.ui

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class XbUiSettingsPanel : JPanel(GridBagLayout()), XbUiSettingsView {
    private val enableSyntaxHighlightingCheckbox = JCheckBox("Enable syntax highlighting")
    private val showInlayHintsCheckbox = JCheckBox("Show inlay hints")
    private val tabSizeSpinner = JSpinner(SpinnerNumberModel(DEFAULT_TAB_SIZE, MIN_TAB_SIZE, MAX_TAB_SIZE, 1))
    private val completionLimitSpinner = JSpinner(
        SpinnerNumberModel(DEFAULT_COMPLETION_LIMIT, MIN_COMPLETION_LIMIT, MAX_COMPLETION_LIMIT, 1),
    )

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
    }

    override fun render(state: XbUiSettingsState) {
        enableSyntaxHighlightingCheckbox.isSelected = state.enableSyntaxHighlighting
        showInlayHintsCheckbox.isSelected = state.showInlayHints
        tabSizeSpinner.value = state.tabSize
        completionLimitSpinner.value = state.completionLimit
    }

    override fun currentState(): XbUiSettingsState {
        return XbUiSettingsState(
            enableSyntaxHighlighting = enableSyntaxHighlightingCheckbox.isSelected,
            showInlayHints = showInlayHintsCheckbox.isSelected,
            tabSize = (tabSizeSpinner.value as Number).toInt(),
            completionLimit = (completionLimitSpinner.value as Number).toInt(),
        )
    }

    fun snapshot(): String {
        val state = currentState()
        return buildString {
            append("enableSyntaxHighlighting=").append(state.enableSyntaxHighlighting).append('\n')
            append("showInlayHints=").append(state.showInlayHints).append('\n')
            append("tabSize=").append(state.tabSize).append('\n')
            append("completionLimit=").append(state.completionLimit)
        }
    }

    private fun GridBagConstraints.withRow(row: Int): GridBagConstraints {
        return (clone() as GridBagConstraints).apply {
            gridy = row
        }
    }

    companion object {
        private const val DEFAULT_TAB_SIZE = XbUiSettingsState().tabSize
        private const val DEFAULT_COMPLETION_LIMIT = XbUiSettingsState().completionLimit
        private const val MIN_TAB_SIZE = XbUiSettingsStore.MIN_TAB_SIZE
        private const val MAX_TAB_SIZE = XbUiSettingsStore.MAX_TAB_SIZE
        private const val MIN_COMPLETION_LIMIT = XbUiSettingsStore.MIN_COMPLETION_LIMIT
        private const val MAX_COMPLETION_LIMIT = XbUiSettingsStore.MAX_COMPLETION_LIMIT
    }
}
