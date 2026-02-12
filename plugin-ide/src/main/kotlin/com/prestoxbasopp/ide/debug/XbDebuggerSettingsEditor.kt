package com.prestoxbasopp.ide.debug

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class XbDebuggerSettingsEditor(project: Project) : com.intellij.openapi.options.SettingsEditor<XbDebuggerRunConfiguration>() {
    private val debuggerExecutableField = TextFieldWithBrowseButton()
    private val targetExecutableField = TextFieldWithBrowseButton()
    private val sourcePathField = JTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val parametersField = JTextField()

    private val debugProgramStartup = JCheckBox("Debug Program Startup (/i)")
    private val ignoreRestartInfo = JCheckBox("Ignore restart info (/n)")
    private val skipAccessAssignInspection = JCheckBox("Disable ACCESS/ASSIGN inspection (/na)")
    private val displayFieldVariables = JCheckBox("Display FIELD variables (/mf)")
    private val displayLocalVariables = JCheckBox("Display LOCAL variables (/ml)")
    private val displayPrivateVariables = JCheckBox("Display PRIVATE variables (/mp)")
    private val displayCurrentObjectMembers = JCheckBox("Display members of current object (/ms)")
    private val displayStaticVariables = JCheckBox("Display STATIC variables (/mt)")
    private val displayPublicVariables = JCheckBox("Display PUBLIC variables (/mu)")

    private val panel: JPanel

    init {
        debuggerExecutableField.addBrowseFolderListener(
            "Xbase++ Debugger executable",
            "Select your local XPPDBG executable. The debugger is not bundled with this plugin.",
            project,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
        )
        targetExecutableField.addBrowseFolderListener(
            "Target executable",
            "Select the compiled XBase++ EXE to debug.",
            project,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
        )
        workingDirectoryField.addBrowseFolderListener(
            "Working directory",
            "Select the process working directory.",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )

        val warning = javax.swing.JLabel("Debugger executable must be configured. XPPDBG is never bundled.")
        warning.foreground = com.intellij.ui.JBColor.RED

        panel = JPanel(BorderLayout())
        panel.add(
            FormBuilder.createFormBuilder()
                .addComponent(warning)
                .addLabeledComponent("Debugger executable", debuggerExecutableField)
                .addLabeledComponent("Target executable", targetExecutableField)
                .addLabeledComponent("Program parameters", parametersField)
                .addLabeledComponent("Source path (; separated)", sourcePathField)
                .addLabeledComponent("Working directory", workingDirectoryField)
                .addComponent(debugProgramStartup)
                .addComponent(ignoreRestartInfo)
                .addComponent(skipAccessAssignInspection)
                .addComponent(displayFieldVariables)
                .addComponent(displayLocalVariables)
                .addComponent(displayPrivateVariables)
                .addComponent(displayCurrentObjectMembers)
                .addComponent(displayStaticVariables)
                .addComponent(displayPublicVariables)
                .panel,
            BorderLayout.CENTER,
        )

        debuggerExecutableField.textField.document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    warning.isVisible = StringUtil.isEmptyOrSpaces(debuggerExecutableField.text)
                }
            },
        )
    }

    override fun resetEditorFrom(configuration: XbDebuggerRunConfiguration) {
        debuggerExecutableField.text = configuration.debuggerExecutablePath
        targetExecutableField.text = configuration.targetExecutablePath
        parametersField.text = configuration.programParameters
        sourcePathField.text = configuration.sourcePath
        workingDirectoryField.text = configuration.workingDirectory
        debugProgramStartup.isSelected = configuration.debugProgramStartup
        ignoreRestartInfo.isSelected = configuration.ignoreRestartInfo
        skipAccessAssignInspection.isSelected = configuration.skipAccessAssignInspection
        displayFieldVariables.isSelected = configuration.displayFieldVariables
        displayLocalVariables.isSelected = configuration.displayLocalVariables
        displayPrivateVariables.isSelected = configuration.displayPrivateVariables
        displayCurrentObjectMembers.isSelected = configuration.displayCurrentObjectMembers
        displayStaticVariables.isSelected = configuration.displayStaticVariables
        displayPublicVariables.isSelected = configuration.displayPublicVariables
    }

    override fun applyEditorTo(configuration: XbDebuggerRunConfiguration) {
        configuration.debuggerExecutablePath = debuggerExecutableField.text.trim()
        configuration.targetExecutablePath = targetExecutableField.text.trim()
        configuration.programParameters = parametersField.text.trim()
        configuration.sourcePath = sourcePathField.text.trim()
        configuration.workingDirectory = workingDirectoryField.text.trim()
        configuration.debugProgramStartup = debugProgramStartup.isSelected
        configuration.ignoreRestartInfo = ignoreRestartInfo.isSelected
        configuration.skipAccessAssignInspection = skipAccessAssignInspection.isSelected
        configuration.displayFieldVariables = displayFieldVariables.isSelected
        configuration.displayLocalVariables = displayLocalVariables.isSelected
        configuration.displayPrivateVariables = displayPrivateVariables.isSelected
        configuration.displayCurrentObjectMembers = displayCurrentObjectMembers.isSelected
        configuration.displayStaticVariables = displayStaticVariables.isSelected
        configuration.displayPublicVariables = displayPublicVariables.isSelected
    }

    override fun createEditor(): JComponent = panel
}
