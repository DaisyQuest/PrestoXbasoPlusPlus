package com.prestoxbasopp.ide

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.prestoxbasopp.ui.XbUiSettingsModel
import com.prestoxbasopp.ui.XbUiSettingsPanel
import com.prestoxbasopp.ui.XbUiSettingsPresenter
import com.prestoxbasopp.ui.XbUiSettingsStore
import javax.swing.JComponent

class XbSettingsConfigurable(
    private val model: XbUiSettingsModel = XbUiSettingsModel(
        XbIdeLanguageService(),
        XbUiSettingsStore.defaultsStore(),
    ),
    private val panelFactory: ((() -> Unit) -> XbUiSettingsPanel) = { navigateAction ->
        XbUiSettingsPanel(navigateAction)
    },
) : Configurable {
    private var presenter: XbUiSettingsPresenter? = null
    private var panel: XbUiSettingsPanel? = null

    override fun getDisplayName(): String = model.displayName()

    override fun createComponent(): JComponent {
        val createdPanel = panelFactory { navigateToEditorColorSettings() }
        panel = createdPanel
        presenter = XbUiSettingsPresenter(model, createdPanel).also { it.load() }
        return createdPanel
    }

    override fun isModified(): Boolean = presenter?.isModified() ?: false

    override fun apply() {
        presenter?.applyChanges()
    }

    override fun reset() {
        presenter?.reset()
    }

    override fun disposeUIResources() {
        presenter = null
        panel = null
    }

    private fun navigateToEditorColorSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            null,
            "Editor.Color Scheme.Xbase++",
        )
    }
}
