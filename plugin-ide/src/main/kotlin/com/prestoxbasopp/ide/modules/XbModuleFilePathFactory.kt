package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths

class XbModuleFilePathFactory {
    fun moduleFilePath(project: Project, moduleName: String): Path {
        val basePath = project.basePath ?: "."
        return Paths.get(basePath, ".idea", "$moduleName.iml")
    }
}
