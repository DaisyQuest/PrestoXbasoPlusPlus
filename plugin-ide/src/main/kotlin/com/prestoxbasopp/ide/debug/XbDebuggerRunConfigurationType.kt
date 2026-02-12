package com.prestoxbasopp.ide.debug

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class XbDebuggerRunConfigurationType : ConfigurationTypeBase(
    ID,
    "XBase++ Debugger",
    "Launches XPPDBG with a user-provided debugger executable and target XBase++ program.",
    AllIcons.RunConfigurations.Application,
) {
    init {
        addFactory(XbDebuggerConfigurationFactory(this))
    }

    companion object {
        const val ID: String = "XBASEPP_DEBUGGER_CONFIGURATION"
    }
}

class XbDebuggerConfigurationFactory(type: XbDebuggerRunConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "XBase++ Debugger"

    override fun createTemplateConfiguration(project: Project): XbDebuggerRunConfiguration =
        XbDebuggerRunConfiguration(project, this, "XBase++ Debugger")
}
