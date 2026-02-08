package com.prestoxbasopp.ide

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler

class XbRenameHandler(
    private val renameService: XbRenameService = XbRenameService(),
) : RenameHandler {
    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false
        if (file !is XbPsiFile) {
            return false
        }
        return renameService.canRename(editor.document.text, editor.caretModel.offset)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        if (file !is XbPsiFile) {
            return
        }
        val offset = editor.caretModel.offset
        performRename(project, editor, offset)
    }

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext) {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return
        val element = elements.firstOrNull() ?: return
        if (element.containingFile !is XbPsiFile) {
            return
        }
        val offset = element.textRange.startOffset
        performRename(project, editor, offset)
    }

    private fun performRename(project: Project, editor: Editor, offset: Int) {
        val source = editor.document.text
        val oldName = renameService.findSymbolName(source, offset) ?: return
        val newName = Messages.showInputDialog(
            project,
            "Rename '$oldName' to:",
            "Rename",
            Messages.getQuestionIcon(),
            oldName,
            null,
        ) ?: return
        val result = renameService.rename(source, offset, newName)
        if (result.errors.isNotEmpty()) {
            Messages.showErrorDialog(project, result.errors.joinToString("\n"), "Rename Failed")
            return
        }
        if (result.updatedText == source) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(result.updatedText)
        }
    }
}
