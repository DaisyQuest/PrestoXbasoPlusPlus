package com.prestoxbasopp.ide.modules

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.roots.ModifiableRootModel

class XbModuleBuilder : com.intellij.openapi.module.ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = ModuleTypeManager.getInstance().findByID(XbModuleType.ID)
        ?: XbModuleType()

    override fun setupRootModel(model: ModifiableRootModel) {
        val path = contentEntryPath ?: return
        val entry = model.addContentEntry(path)
        entry.addSourceFolder(path, false)
        model.inheritSdk()
    }
}
