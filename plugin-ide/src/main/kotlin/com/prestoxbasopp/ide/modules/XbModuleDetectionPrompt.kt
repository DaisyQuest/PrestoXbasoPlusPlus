package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

interface XbModuleDetectionPrompt {
    fun confirmInitialization(project: Project, candidates: List<XbModuleCandidate>): Boolean
}

class XbIdeaModuleDetectionPrompt(
    private val messageBuilder: XbModulePromptMessageBuilder,
) : XbModuleDetectionPrompt {
    override fun confirmInitialization(project: Project, candidates: List<XbModuleCandidate>): Boolean {
        val message = messageBuilder.buildMessage(candidates)
        val result = Messages.showYesNoDialog(
            project,
            message,
            "XBase++ project detected, run initializer?",
            Messages.getQuestionIcon(),
        )
        return result == Messages.YES
    }
}
