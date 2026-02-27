package com.prestoxbasopp.ui

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
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
            highlightingPreferences = XbHighlightingPreferences(
                styleMappings = XbHighlightCategory.entries.associateWith {
                    if (it == XbHighlightCategory.IDENTIFIER) XbHighlightCategory.KEYWORD else it
                },
                wordOverrides = mapOf("myfunc" to XbHighlightCategory.FUNCTION_CALL),
            ),
        )

        panel.render(state)

        assertThat(panel.snapshot()).contains(
            "enableSyntaxHighlighting=false",
            "showInlayHints=false",
            "tabSize=6",
            "completionLimit=120",
            "IDENTIFIER->KEYWORD",
            "wordOverrides=myfunc=FUNCTION_CALL",
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
            highlightingPreferences = XbHighlightingPreferences(
                wordOverrides = mapOf("special" to XbHighlightCategory.STRING),
            ),
        )

        panel.render(state)

        assertThat(panel.currentState()).isEqualTo(state)
    }

    @Test
    fun `currentState normalizes override rows and ignores blanks`() {
        val panel = XbUiSettingsPanel()
        panel.render(
            XbUiSettingsState(
                highlightingPreferences = XbHighlightingPreferences(
                    wordOverrides = mapOf("ok" to XbHighlightCategory.STRING),
                ),
            ),
        )

        panel.updateOverrideRow(0, "  MixedCase  ", XbHighlightCategory.DATE)
        panel.updateOverrideRow(99, "ignored", XbHighlightCategory.ERROR)

        assertThat(panel.currentState().highlightingPreferences.wordOverrides)
            .containsEntry("mixedcase", XbHighlightCategory.DATE)
            .doesNotContainKey("ignored")
    }

    @Test
    fun `currentState keeps last duplicate override entry`() {
        val panel = XbUiSettingsPanel()
        panel.render(XbUiSettingsState())
        panel.addOverrideRow("dupe", XbHighlightCategory.KEYWORD)
        panel.addOverrideRow("DUPE", XbHighlightCategory.STRING)

        assertThat(panel.currentState().highlightingPreferences.wordOverrides)
            .containsOnly(entry("dupe", XbHighlightCategory.STRING))
    }
}
