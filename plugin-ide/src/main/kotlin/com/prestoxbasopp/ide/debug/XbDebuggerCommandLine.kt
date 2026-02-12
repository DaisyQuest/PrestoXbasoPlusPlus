package com.prestoxbasopp.ide.debug

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import java.io.File

internal data class XbDebuggerLaunchSettings(
    val debuggerExecutablePath: String,
    val targetExecutablePath: String,
    val programParameters: String,
    val sourcePath: String,
    val workingDirectory: String,
    val debugProgramStartup: Boolean,
    val ignoreRestartInfo: Boolean,
    val skipAccessAssignInspection: Boolean,
    val displayFieldVariables: Boolean,
    val displayLocalVariables: Boolean,
    val displayPrivateVariables: Boolean,
    val displayCurrentObjectMembers: Boolean,
    val displayStaticVariables: Boolean,
    val displayPublicVariables: Boolean,
)

internal data class XbDebuggerCommand(
    val executablePath: String,
    val targetExecutablePath: String,
    val targetParameters: List<String>,
    val options: List<String>,
    val workingDirectory: String?,
)

internal object XbDebuggerCommandLine {
    fun from(configuration: XbDebuggerRunConfiguration): XbDebuggerCommand =
        from(
            XbDebuggerLaunchSettings(
                debuggerExecutablePath = configuration.debuggerExecutablePath,
                targetExecutablePath = configuration.targetExecutablePath,
                programParameters = configuration.programParameters,
                sourcePath = configuration.sourcePath,
                workingDirectory = configuration.workingDirectory,
                debugProgramStartup = configuration.debugProgramStartup,
                ignoreRestartInfo = configuration.ignoreRestartInfo,
                skipAccessAssignInspection = configuration.skipAccessAssignInspection,
                displayFieldVariables = configuration.displayFieldVariables,
                displayLocalVariables = configuration.displayLocalVariables,
                displayPrivateVariables = configuration.displayPrivateVariables,
                displayCurrentObjectMembers = configuration.displayCurrentObjectMembers,
                displayStaticVariables = configuration.displayStaticVariables,
                displayPublicVariables = configuration.displayPublicVariables,
            ),
        )

    fun from(settings: XbDebuggerLaunchSettings): XbDebuggerCommand {
        val options = buildList {
            if (settings.debugProgramStartup) add("/i")
            if (settings.ignoreRestartInfo) add("/n")
            if (settings.sourcePath.isNotBlank()) add("/s:${settings.sourcePath}")
            if (settings.skipAccessAssignInspection) add("/na")
            if (settings.displayFieldVariables) add("/mf")
            if (settings.displayLocalVariables) add("/ml")
            if (settings.displayPrivateVariables) add("/mp")
            if (settings.displayCurrentObjectMembers) add("/ms")
            if (settings.displayStaticVariables) add("/mt")
            if (settings.displayPublicVariables) add("/mu")
        }

        return XbDebuggerCommand(
            executablePath = settings.debuggerExecutablePath,
            targetExecutablePath = settings.targetExecutablePath,
            targetParameters = ParametersListUtil.parse(settings.programParameters, false, true),
            options = options,
            workingDirectory = settings.workingDirectory.takeIf { it.isNotBlank() },
        )
    }

    fun toGeneralCommandLine(command: XbDebuggerCommand): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
            .withExePath(command.executablePath)
            .withParameters(command.options)
            .withParameters(command.targetExecutablePath)
            .withParameters(command.targetParameters)

        command.workingDirectory?.let { commandLine.withWorkDirectory(File(it)) }
        return commandLine
    }
}
