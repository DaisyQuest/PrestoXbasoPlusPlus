package com.prestoxbasopp.ide.debug

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

class XbDebuggerRunConfiguration(
    project: Project,
    factory: XbDebuggerConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<RunConfigurationOptions>(project, factory, name) {

    var debuggerExecutablePath: String = ""
    var targetExecutablePath: String = ""
    var programParameters: String = ""
    var sourcePath: String = ""
    var workingDirectory: String = ""
    var debugProgramStartup: Boolean = false
    var ignoreRestartInfo: Boolean = false
    var skipAccessAssignInspection: Boolean = false
    var displayFieldVariables: Boolean = false
    var displayLocalVariables: Boolean = false
    var displayPrivateVariables: Boolean = false
    var displayCurrentObjectMembers: Boolean = false
    var displayStaticVariables: Boolean = false
    var displayPublicVariables: Boolean = false

    override fun getConfigurationEditor(): SettingsEditor<out com.intellij.execution.configurations.RunConfiguration> =
        XbDebuggerSettingsEditor(project)

    override fun checkConfiguration() {
        if (debuggerExecutablePath.isBlank()) {
            throw RuntimeConfigurationError(
                "Debugger executable path is required. Point to your local XPPDBG executable.",
            )
        }
        if (targetExecutablePath.isBlank()) {
            throw RuntimeConfigurationError("Target executable path is required.")
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        XbDebuggerRunProfileState(environment, this)

    public override fun clone(): XbDebuggerRunConfiguration {
        val clone = super.clone() as XbDebuggerRunConfiguration
        XmlSerializerUtil.copyBean(this, clone)
        return clone
    }
}
