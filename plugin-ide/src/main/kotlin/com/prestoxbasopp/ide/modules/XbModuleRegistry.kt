package com.prestoxbasopp.ide.modules

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project

interface XbModuleRegistry {
    fun hasXbModules(project: Project): Boolean
}

class XbIdeaModuleRegistry : XbModuleRegistry {
    override fun hasXbModules(project: Project): Boolean {
        return ModuleManager.getInstance(project).modules.any { module ->
            ModuleType.get(module).id == XbModuleType.ID
        }
    }
}
