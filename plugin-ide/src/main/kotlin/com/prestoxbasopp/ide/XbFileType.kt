package com.prestoxbasopp.ide

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object XbFileType : LanguageFileType(XbLanguage) {
    override fun getName(): String = "Xbase++ File"

    override fun getDescription(): String = "Xbase++ source file"

    override fun getDefaultExtension(): String = "xb"

    override fun getIcon(): Icon? = null
}
