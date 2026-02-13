package com.prestoxbasopp.ide.dbf

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.wm.ToolWindowManager

class ReverseEngineerDbfAction : AnAction("Reverse Engineer DBF") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = ToolWindowManager.getInstance(project)
        val toolWindow = manager.getToolWindow("Ultra DBF Master")
        toolWindow?.show()

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile != null && virtualFile.extension.equals("dbf", ignoreCase = true)) {
            OpenFileDescriptor(project, virtualFile).navigate(true)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
