package com.prestoxbasopp.ide.modules

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import javax.swing.Icon

class XbModuleType : ModuleType<XbModuleBuilder>(ID) {
    override fun createModuleBuilder(): XbModuleBuilder = XbModuleBuilder()

    override fun getName(): String = "XBase++ Module"

    override fun getDescription(): String = "XBase++ module with tooling support."

    override fun getNodeIcon(isOpened: Boolean): Icon = AllIcons.Nodes.Module

    companion object {
        const val ID = "XBASEPP_MODULE"
    }
}
