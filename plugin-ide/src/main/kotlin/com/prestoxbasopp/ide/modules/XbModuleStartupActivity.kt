package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class XbModuleStartupActivity : StartupActivity.Background {
    override fun runActivity(project: Project) {
        val initializer = createInitializer(project)
        initializer.initializeIfNeeded(project)
    }

    private fun createInitializer(project: Project): XbModuleInitializer {
        val discoveryService = XbModuleDiscoveryService.createDefault(project)
        val initializationState = XbPropertiesInitializationState()
        val decider = XbModuleInitializationDecider(initializationState, XbIdeaModuleRegistry())
        val prompt = XbIdeaModuleDetectionPrompt(XbModulePromptMessageBuilder())
        val creator = XbIdeaModuleCreator(XbModuleNameResolver(), XbModuleFilePathFactory())
        val pathResolver = XbDefaultProjectPathResolver()
        return XbModuleInitializer(
            discoveryService,
            decider,
            prompt,
            creator,
            initializationState,
            pathResolver,
        )
    }
}
