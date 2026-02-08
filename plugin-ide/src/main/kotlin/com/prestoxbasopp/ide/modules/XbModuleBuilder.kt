package com.prestoxbasopp.ide.modules

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import java.nio.file.Paths

class XbModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = ModuleTypeManager.getInstance().findByID(XbModuleType.ID)
        ?: XbModuleType()

    override fun setupRootModel(model: ModifiableRootModel) {
        val path = contentEntryPath ?: return
        val contentRoot = Paths.get(path)
        Files.createDirectories(contentRoot)
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(contentRoot.toString()) ?: return
        val entry = model.addContentEntry(virtualFile)
        entry.addSourceFolder(virtualFile, false)
        model.inheritSdk()
    }
}
