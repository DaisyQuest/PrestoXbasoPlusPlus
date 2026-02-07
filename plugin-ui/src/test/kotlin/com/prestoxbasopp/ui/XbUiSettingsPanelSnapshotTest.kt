package com.prestoxbasopp.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbUiSettingsPanelSnapshotTest {
    @Test
    fun `snapshot captures stable component state`() {
        val panel = XbUiSettingsPanel()
        val state = XbUiSettingsState(
            enableSyntaxHighlighting = false,
            showInlayHints = false,
            tabSize = 6,
            completionLimit = 120,
        )

        panel.render(state)

        assertThat(panel.snapshot()).isEqualTo(
            """
            enableSyntaxHighlighting=false
            showInlayHints=false
            tabSize=6
            completionLimit=120
            """.trimIndent(),
        )
    }

    @Test
    fun `currentState reflects render`() {
        val panel = XbUiSettingsPanel()
        val state = XbUiSettingsState(
            enableSyntaxHighlighting = true,
            showInlayHints = false,
            tabSize = 2,
            completionLimit = 10,
        )

        panel.render(state)

        assertThat(panel.currentState()).isEqualTo(state)
    }
}
