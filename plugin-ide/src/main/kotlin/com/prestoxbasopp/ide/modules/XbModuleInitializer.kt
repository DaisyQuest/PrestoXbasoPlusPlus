package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import java.nio.file.Path

data class XbModuleInitializationResult(
    val prompted: Boolean,
    val accepted: Boolean,
    val createdModules: Int,
)

class XbModuleInitializer(
    private val discoveryService: XbModuleDiscoveryService,
    private val decider: XbModuleInitializationDecider,
    private val prompt: XbModuleDetectionPrompt,
    private val creator: XbModuleCreator,
    private val state: XbProjectInitializationState,
    private val pathResolver: XbProjectPathResolver,
) {
    fun initializeIfNeeded(project: Project): XbModuleInitializationResult {
        if (!decider.shouldPrompt(project)) {
            return XbModuleInitializationResult(prompted = false, accepted = false, createdModules = 0)
        }

        val basePath = pathResolver.resolveBasePath(project) ?: return XbModuleInitializationResult(false, false, 0)
        val candidates = discoveryService.discover(basePath)
        if (candidates.isEmpty()) {
            return XbModuleInitializationResult(prompted = false, accepted = false, createdModules = 0)
        }

        val accepted = prompt.confirmInitialization(project, candidates)
        if (!accepted) {
            return XbModuleInitializationResult(prompted = true, accepted = false, createdModules = 0)
        }

        val created = creator.createModules(project, candidates)
        state.markInitialized(project)
        return XbModuleInitializationResult(prompted = true, accepted = true, createdModules = created.size)
    }
}
