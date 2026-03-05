package com.prestoxbasopp.ide

import com.intellij.application.options.CodeStyle
import com.intellij.psi.codeStyle.CodeStyleSettingsManager

interface XbCodeStyleTabSynchronizer {
    fun sync(tabSize: Int)
}

interface XbIndentOptionsTarget {
    var indentSize: Int
    var tabSize: Int
    var continuationIndentSize: Int
}

private class XbLanguageIndentOptionsTarget : XbIndentOptionsTarget {
    private val indentOptions = CodeStyle.getDefaultSettings().getLanguageIndentOptions(XbLanguage)

    override var indentSize: Int
        get() = indentOptions.INDENT_SIZE
        set(value) {
            indentOptions.INDENT_SIZE = value
        }

    override var tabSize: Int
        get() = indentOptions.TAB_SIZE
        set(value) {
            indentOptions.TAB_SIZE = value
        }

    override var continuationIndentSize: Int
        get() = indentOptions.CONTINUATION_INDENT_SIZE
        set(value) {
            indentOptions.CONTINUATION_INDENT_SIZE = value
        }
}

class XbIdeCodeStyleTabSynchronizer(
    private val indentOptionsTargetProvider: () -> XbIndentOptionsTarget = { XbLanguageIndentOptionsTarget() },
    private val changeNotifier: () -> Unit = { CodeStyleSettingsManager.getInstance().notifyCodeStyleSettingsChanged() },
) : XbCodeStyleTabSynchronizer {
    override fun sync(tabSize: Int) {
        if (tabSize <= 0) {
            return
        }
        val target = indentOptionsTargetProvider()
        target.indentSize = tabSize
        target.tabSize = tabSize
        target.continuationIndentSize = tabSize
        changeNotifier()
    }
}
