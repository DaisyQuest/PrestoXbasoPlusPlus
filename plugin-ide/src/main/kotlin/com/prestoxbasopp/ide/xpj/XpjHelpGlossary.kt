package com.prestoxbasopp.ide.xpj

data class XpjGlossaryItem(
    val key: String,
    val summary: String,
    val details: String,
)

object XpjHelpGlossary {
    val definitions: List<XpjGlossaryItem> = listOf(
        XpjGlossaryItem("DEBUG", "Build with debug symbols", "YES builds debug-enabled targets, NO creates release binaries."),
        XpjGlossaryItem("GUI", "Windowed vs text-mode application", "Use GUI=YES for graphical output and GUI=NO for text mode applications."),
        XpjGlossaryItem("TARGET_DIR", "Deployment directory", "Controls where built binaries are deployed. Target-level values override project-level."),
        XpjGlossaryItem("INCLUDE", "Header include paths", "Adds framework/tool-level .ch search paths to INCLUDE for the pbuild process."),
        XpjGlossaryItem("INCLUDE_FROM", "Import target from another project", "Includes a target from another XPJ and optionally remaps the target name."),
        XpjGlossaryItem("COMPILE_FLAGS", "Compiler flags", "Contains xpp.exe switches except restricted /o and /b flags."),
        XpjGlossaryItem("LINK_FLAGS", "Linker flags", "Extra linker arguments not already modeled by DEBUG/GUI switches."),
        XpjGlossaryItem("INTERMEDIATE_DEBUG", "Debug intermediates directory", "Where debug OBJ/RES files are emitted; overrides OBJ_DIR behavior."),
        XpjGlossaryItem("INTERMEDIATE_RELEASE", "Release intermediates directory", "Where release OBJ/RES files are emitted; overrides OBJ_DIR behavior."),
        XpjGlossaryItem("OBJ_FORMAT", "Object format selection", "Select COFF or OMF, affecting compiler switches and linker selection."),
        XpjGlossaryItem("DEPENDS_ON", "Target dependency ordering", "Declares other targets that must build first; cycles terminate the build."),
        XpjGlossaryItem("PRE_BUILD / POST_BUILD", "Custom shell commands", "Commands executed before/after build. Prefix with '-' to ignore command failures."),
        XpjGlossaryItem("PRE_CLEAN / POST_CLEAN", "Custom clean commands", "Commands executed before/after cleaning. Prefix with '-' to ignore command failures."),
        XpjGlossaryItem("RUNPARAMETERS", "Workbench run arguments", "Arguments passed from Xbase++ Workbench when running/debugging the target."),
    )

    val macros: List<XpjGlossaryItem> = listOf(
        XpjGlossaryItem("$(ASSETS_PATH)", "Asset repository path", "Expands to the fully qualified path of the assets repository."),
        XpjGlossaryItem("$(DEBUG_PATH)", "Debug intermediate path", "Expands to fully qualified debug intermediate directory."),
        XpjGlossaryItem("$(RELEASE_PATH)", "Release intermediate path", "Expands to fully qualified release intermediate directory."),
        XpjGlossaryItem("$(TARGET_PATH)", "Target deployment path", "Expands to fully qualified directory where binary is deployed."),
    )

    val commandLineOptions: List<XpjGlossaryItem> = listOf(
        XpjGlossaryItem("/a", "Rebuild all", "Forces full compile + link regardless of timestamps."),
        XpjGlossaryItem("/c", "Clean", "Deletes intermediates and target binaries."),
        XpjGlossaryItem("/d<id>=<val>", "Override definition", "Temporarily overrides XPJ definitions for one command invocation."),
        XpjGlossaryItem("/g[:name]", "Generate dependency-expanded XPJ", "Analyzes dependencies and updates (or creates) XPJ structure."),
        XpjGlossaryItem("/l", "Skip DEF/LIB regeneration", "Suppresses automatic DEF/LIB recreation for DLLs."),
        XpjGlossaryItem("/n", "Dry run", "Prints required build steps without executing tools."),
        XpjGlossaryItem("/q", "Quiet mode", "Suppresses non-essential output and forwards /q to invoked tools."),
        XpjGlossaryItem("/t:<target>", "Build specific target", "Builds only one target section by name."),
        XpjGlossaryItem("/v", "Verbose mode", "Enables verbose ProjectBuilder logging."),
    )

    fun fullHelpText(): String = buildString {
        appendLine("XPJ Visual Editor — Capability Glossary")
        appendLine()
        appendLine("This editor provides structured XPJ editing with section-aware forms, direct source references, target-level definition overrides, and macro-aware value entry.")
        appendLine("Use the Structure tab to add sections/definitions/files, the Definitions tab to inspect key build settings, and this Help tab for reference.")
        appendLine()
        appendLine("Definitions")
        definitions.forEach { appendGlossaryItem(it) }
        appendLine()
        appendLine("Macros")
        macros.forEach { appendGlossaryItem(it) }
        appendLine()
        appendLine("PBUILD command line options")
        commandLineOptions.forEach { appendGlossaryItem(it) }
    }

    private fun StringBuilder.appendGlossaryItem(item: XpjGlossaryItem) {
        appendLine("- ${item.key}: ${item.summary}")
        appendLine("  ${item.details}")
    }
}
