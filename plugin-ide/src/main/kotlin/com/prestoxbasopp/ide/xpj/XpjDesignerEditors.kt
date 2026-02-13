package com.prestoxbasopp.ide.xpj

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class XpjProjectFileEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.extension.equals("xpj", ignoreCase = true)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val designer = XpjDesignerFileEditor(project, file)
        return TextEditorWithPreview(textEditor, designer, "XPJ Visual Designer", Layout.SHOW_EDITOR_AND_PREVIEW, false)
    }

    override fun getEditorTypeId(): String = "xpj-visual-designer"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class XpjDesignerFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {
    private val parser = XpjProjectParser()
    private val serializer = XpjProjectSerializer()
    private var modified = false
    private val editorModel: XpjEditorModel
    private val component: JPanel

    init {
        val content = String(file.contentsToByteArray())
        editorModel = XpjEditorModel(parser.parse(content))
        val panel = XpjEditorPanel(
            model = editorModel,
            rootPath = file.parent.toNioPath(),
            onDirtyChanged = { modified = true },
        )

        component = JPanel(BorderLayout()).apply {
            add(panel, BorderLayout.CENTER)
            add(JButton("Apply Changes to XPJ").apply { addActionListener { applyChanges() } }, BorderLayout.SOUTH)
        }
    }

    private fun applyChanges() {
        val text = serializer.serialize(editorModel.snapshot())
        WriteCommandAction.runWriteCommandAction(project) {
            file.setBinaryContent(text.toByteArray())
        }
        modified = false
    }

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun getName(): String = "XPJ Visual Designer"

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = modified

    override fun isValid(): Boolean = file.isValid

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() = Unit
}
