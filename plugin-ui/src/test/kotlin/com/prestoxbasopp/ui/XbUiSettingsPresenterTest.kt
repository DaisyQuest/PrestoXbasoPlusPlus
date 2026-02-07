package com.prestoxbasopp.ui

import com.prestoxbasopp.core.api.XbLanguageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbUiSettingsPresenterTest {
    @Test
    fun `load renders model state into view`() {
        val view = FakeSettingsView()
        val model = modelWithState(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = true,
                tabSize = 2,
                completionLimit = 99,
            ),
        )
        val presenter = XbUiSettingsPresenter(model, view)

        presenter.load()

        assertThat(view.renderedState).isEqualTo(model.state)
    }

    @Test
    fun `applyChanges persists view state`() {
        val view = FakeSettingsView(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = false,
                tabSize = 6,
                completionLimit = 40,
            ),
        )
        val model = modelWithState(XbUiSettingsState())
        val presenter = XbUiSettingsPresenter(model, view)

        presenter.applyChanges()

        assertThat(model.state).isEqualTo(view.currentState())
    }

    @Test
    fun `isModified compares view and model state`() {
        val model = modelWithState(XbUiSettingsState())
        val view = FakeSettingsView(XbUiSettingsState())
        val presenter = XbUiSettingsPresenter(model, view)

        assertThat(presenter.isModified()).isFalse()

        view.updateState(view.currentState().copy(tabSize = 8))

        assertThat(presenter.isModified()).isTrue()
    }

    @Test
    fun `reset restores defaults in model and view`() {
        val model = modelWithState(
            XbUiSettingsState(
                enableSyntaxHighlighting = false,
                showInlayHints = false,
                tabSize = 2,
                completionLimit = 80,
            ),
        )
        val view = FakeSettingsView()
        val presenter = XbUiSettingsPresenter(model, view)

        presenter.reset()

        assertThat(model.state).isEqualTo(XbUiSettingsState())
        assertThat(view.renderedState).isEqualTo(XbUiSettingsState())
    }

    private fun modelWithState(state: XbUiSettingsState): XbUiSettingsModel {
        val service = object : XbLanguageService {
            override fun languageId(): String = "xbase++"
        }
        val store = XbUiSettingsStore(InMemoryKeyValueStore())
        val model = XbUiSettingsModel(service, store)
        model.updateState(state)
        return model
    }

    private class FakeSettingsView(initialState: XbUiSettingsState = XbUiSettingsState()) : XbUiSettingsView {
        private var state: XbUiSettingsState = initialState
        var renderedState: XbUiSettingsState? = null
            private set

        override fun render(state: XbUiSettingsState) {
            this.state = state
            renderedState = state
        }

        override fun currentState(): XbUiSettingsState {
            return state
        }

        fun updateState(state: XbUiSettingsState) {
            this.state = state
        }
    }
}
