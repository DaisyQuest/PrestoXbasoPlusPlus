package com.prestoxbasopp.ide.xpj

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.prestoxbasopp.ide.XbLanguage
import javax.swing.Icon

object XpjFileType : LanguageFileType(XbLanguage) {
    override fun getName(): @NlsSafe String = "XPJ Project"

    override fun getDescription(): @NlsContexts.Label String = "Xbase++ Project Builder file"

    override fun getDefaultExtension(): String = "xpj"

    override fun getIcon(): Icon? = null
}
