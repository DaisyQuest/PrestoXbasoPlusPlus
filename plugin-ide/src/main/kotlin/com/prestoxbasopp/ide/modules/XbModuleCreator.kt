package com.prestoxbasopp.ide.modules

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager

interface XbModuleCreator {
    fun createModules(project: Project, candidates: List<XbModuleCandidate>): List<Module>
}

class XbIdeaModuleCreator(
    private val nameResolver: XbModuleNameResolver,
    private val filePathFactory: XbModuleFilePathFactory,
) : XbModuleCreator {
    override fun createModules(project: Project, candidates: List<XbModuleCandidate>): List<Module> {
        if (candidates.isEmpty()) return emptyList()

        val moduleManager = ModuleManager.getInstance(project)
        val existingNames = moduleManager.modules.map { it.name }.toMutableSet()
        val created = mutableListOf<Module>()

        runWriteAction {
            val model = moduleManager.modifiableModel
            candidates.forEach { candidate ->
                val resolvedName = nameResolver.resolve(candidate.suggestedName, existingNames)
                existingNames.add(resolvedName)
                val moduleFile = filePathFactory.moduleFilePath(project, resolvedName)
                val module = model.newModule(moduleFile.toString(), XbModuleType.ID)
                created.add(module)
                val rootModel = ModuleRootManager.getInstance(module).modifiableModel
                rootModel.addContentEntry(candidate.rootPath.toString())
                rootModel.inheritSdk()
                rootModel.commit()
            }
            model.commit()
        }
        return created
    }
}
