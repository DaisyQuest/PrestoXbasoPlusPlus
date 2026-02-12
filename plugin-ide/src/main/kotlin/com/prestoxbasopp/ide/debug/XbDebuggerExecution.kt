package com.prestoxbasopp.ide.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment

class XbDebuggerRunProfileState(
    environment: ExecutionEnvironment,
    private val configuration: XbDebuggerRunConfiguration,
) : CommandLineState(environment) {

    init {
        val resolver = XbDebuggerSourceResolver(environment.project, configuration.workingDirectory, configuration.sourcePath)
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project)
        consoleBuilder.addFilter(XbDebuggerSourceNavigationFilter(environment.project, resolver))
        setConsoleBuilder(consoleBuilder)
    }

    @Throws(ExecutionException::class)
    override fun startProcess(): OSProcessHandler {
        val command = XbDebuggerCommandLine.from(configuration)
        val commandLine = XbDebuggerCommandLine.toGeneralCommandLine(command)

        val breakpoints = XbDebuggerBreakpointCollector.collect(environment.project)
        if (breakpoints.isNotEmpty()) {
            val payload = breakpoints.joinToString(separator = System.lineSeparator()) { "${it.filePath}:${it.lineOneBased}" }
            commandLine.withEnvironment("XPPDBG_IDEA_BREAKPOINTS", payload)
        }

        return OSProcessHandler(commandLine)
    }
}
