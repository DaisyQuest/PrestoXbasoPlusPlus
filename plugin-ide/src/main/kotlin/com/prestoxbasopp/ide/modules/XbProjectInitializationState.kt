package com.prestoxbasopp.ide.modules

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

interface XbProjectInitializationState {
    fun isInitialized(project: Project): Boolean
    fun markInitialized(project: Project)
}

class XbPropertiesInitializationState : XbProjectInitializationState {
    override fun isInitialized(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(KEY, false)
    }

    override fun markInitialized(project: Project) {
        PropertiesComponent.getInstance(project).setValue(KEY, true)
    }

    companion object {
        private const val KEY = "com.prestoxbasopp.xbasepp.initialized"
    }
}
