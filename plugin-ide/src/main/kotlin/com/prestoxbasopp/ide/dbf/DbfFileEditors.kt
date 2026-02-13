package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class DbfFileEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.extension.equals("dbf", ignoreCase = true)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        DbfFileEditor(project, file)

    override fun getEditorTypeId(): String = "dbf-ultra-master-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

private class DbfFileEditor(
    project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {
    private val panel = UltraDbfMasterPanel(project, initialDbfPath = file.path)

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel

    override fun getName(): String = "DBF Editor"

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() = Unit
}
