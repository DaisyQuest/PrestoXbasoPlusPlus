package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import javax.swing.Icon

object DbfFileType : LanguageFileType(PlainTextLanguage.INSTANCE) {
    override fun getName(): String = "DBF File"

    override fun getDescription(): String = "dBASE database file"

    override fun getDefaultExtension(): String = "dbf"

    override fun getIcon(): Icon? = null
}
