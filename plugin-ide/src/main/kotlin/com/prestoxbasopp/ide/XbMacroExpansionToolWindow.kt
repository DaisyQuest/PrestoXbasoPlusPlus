package com.prestoxbasopp.ide

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class XbMacroExpansionToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = XbMacroExpansionPanel()
        val controller = XbMacroExpansionToolWindowController(project, panel)
        val content = ContentFactory.getInstance().createContent(panel.component, "", false)
        content.setDisposer(controller)
        toolWindow.contentManager.addContent(content)
    }
}

private class XbMacroExpansionToolWindowController(
    private val project: Project,
    private val panel: XbMacroExpansionPanel,
    private val presenter: XbMacroExpansionPresenter = XbMacroExpansionPresenter(),
) : Disposable {
    private val connection = project.messageBus.connect(this)
    private var activeDocument: Document? = null
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            refreshFromCurrentEditor()
        }
    }

    init {
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                refreshFromCurrentEditor()
            }
        })
        refreshFromCurrentEditor()
    }

    override fun dispose() {
        activeDocument?.removeDocumentListener(documentListener)
        activeDocument = null
    }

    private fun refreshFromCurrentEditor() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        updateDocumentListener(editor?.document)
        val presentation = buildPresentation(file, editor)
        panel.render(presentation)
    }

    private fun updateDocumentListener(document: Document?) {
        if (activeDocument == document) {
            return
        }
        activeDocument?.removeDocumentListener(documentListener)
        activeDocument = document
        activeDocument?.addDocumentListener(documentListener)
    }

    private fun buildPresentation(file: VirtualFile?, editor: Editor?): XbMacroExpansionPresentation {
        return ApplicationManager.getApplication().runReadAction<XbMacroExpansionPresentation> {
            val fileName = file?.name
            val filePath = file?.path
            val text = editor?.document?.text
            presenter.present(fileName, filePath, text, project.basePath)
        }
    }
}
