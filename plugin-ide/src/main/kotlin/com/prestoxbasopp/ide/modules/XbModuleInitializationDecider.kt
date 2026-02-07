package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project

class XbModuleInitializationDecider(
    private val state: XbProjectInitializationState,
    private val registry: XbModuleRegistry,
) {
    fun shouldPrompt(project: Project): Boolean {
        return !state.isInitialized(project) && !registry.hasXbModules(project)
    }
}
