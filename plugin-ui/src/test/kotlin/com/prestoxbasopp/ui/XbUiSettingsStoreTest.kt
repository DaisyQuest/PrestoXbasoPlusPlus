package com.prestoxbasopp.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbUiSettingsStoreTest {
    @Test
    fun `load returns defaults when storage empty`() {
        val store = XbUiSettingsStore(InMemoryKeyValueStore())

        val state = store.load()

        assertThat(state).isEqualTo(XbUiSettingsState())
    }

    @Test
    fun `save clamps values into bounds`() {
        val storage = InMemoryKeyValueStore()
        val store = XbUiSettingsStore(storage)

        store.save(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = true,
                tabSize = 99,
                completionLimit = 500,
            ),
        )

        assertThat(store.load()).isEqualTo(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = true,
                tabSize = XbUiSettingsStore.MAX_TAB_SIZE,
                completionLimit = XbUiSettingsStore.MAX_COMPLETION_LIMIT,
            ),
        )
    }

    @Test
    fun `load clamps stored values into bounds`() {
        val storage = InMemoryKeyValueStore()
        val store = XbUiSettingsStore(storage)
        storage.putRawInt("ui.tabSize", 0)
        storage.putRawInt("ui.completionLimit", -10)

        val state = store.load()

        assertThat(state.tabSize).isEqualTo(XbUiSettingsStore.MIN_TAB_SIZE)
        assertThat(state.completionLimit).isEqualTo(XbUiSettingsStore.MIN_COMPLETION_LIMIT)
    }
}
