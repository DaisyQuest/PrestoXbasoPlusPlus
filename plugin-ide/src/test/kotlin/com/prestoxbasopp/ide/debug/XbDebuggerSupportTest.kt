package com.prestoxbasopp.ide.debug

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XbDebuggerSupportTest {
    @Test
    fun `command builder maps all debugger flags and parameters`() {
        val command = XbDebuggerCommandLine.from(
            XbDebuggerLaunchSettings(
                debuggerExecutablePath = "C:/xbase/XPPDBG.EXE",
                targetExecutablePath = "C:/app/myapp.exe",
                programParameters = "--flag \"quoted value\"",
                sourcePath = "src;lib",
                workingDirectory = "C:/app",
                debugProgramStartup = true,
                ignoreRestartInfo = true,
                skipAccessAssignInspection = true,
                displayFieldVariables = true,
                displayLocalVariables = true,
                displayPrivateVariables = true,
                displayCurrentObjectMembers = true,
                displayStaticVariables = true,
                displayPublicVariables = true,
            ),
        )

        assertThat(command.executablePath).isEqualTo("C:/xbase/XPPDBG.EXE")
        assertThat(command.targetExecutablePath).isEqualTo("C:/app/myapp.exe")
        assertThat(command.targetParameters).containsExactly("--flag", "quoted value")
        assertThat(command.options).containsExactly(
            "/i", "/n", "/s:src;lib", "/na", "/mf", "/ml", "/mp", "/ms", "/mt", "/mu",
        )
        assertThat(command.workingDirectory).isEqualTo("C:/app")
    }

    @Test
    fun `command builder omits optional flags when disabled`() {
        val command = XbDebuggerCommandLine.from(
            XbDebuggerLaunchSettings(
                debuggerExecutablePath = "xppdbg",
                targetExecutablePath = "app.exe",
                programParameters = "",
                sourcePath = "",
                workingDirectory = "",
                debugProgramStartup = false,
                ignoreRestartInfo = false,
                skipAccessAssignInspection = false,
                displayFieldVariables = false,
                displayLocalVariables = false,
                displayPrivateVariables = false,
                displayCurrentObjectMembers = false,
                displayStaticVariables = false,
                displayPublicVariables = false,
            ),
        )

        assertThat(command.options).isEmpty()
        assertThat(command.workingDirectory).isNull()
        assertThat(command.targetParameters).isEmpty()
    }

    @Test
    fun `output parser understands supported location formats`() {
        val breakpoint = XbDebuggerOutputParser.parseLocation("BREAKPOINT src/main.prg:42")
        val stoppedAt = XbDebuggerOutputParser.parseLocation("Stopped at APP\\ENTRY.PRG(7)")
        val generic = XbDebuggerOutputParser.parseLocation("at module/file.prg:12")

        assertThat(breakpoint).isEqualTo(XbSourceLocation("src/main.prg", 42))
        assertThat(stoppedAt).isEqualTo(XbSourceLocation("APP\\ENTRY.PRG", 7))
        assertThat(generic).isEqualTo(XbSourceLocation("module/file.prg", 12))
    }

    @Test
    fun `output parser rejects invalid location text`() {
        assertThat(XbDebuggerOutputParser.parseLocation("no location here")).isNull()
        assertThat(XbDebuggerOutputParser.parseLocation("BREAKPOINT file.prg:0")).isNull()
        assertThat(XbDebuggerOutputParser.parseLocation("BREAKPOINT :11")).isNull()
    }
}
