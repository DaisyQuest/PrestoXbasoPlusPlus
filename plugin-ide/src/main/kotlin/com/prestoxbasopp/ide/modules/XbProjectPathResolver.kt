package com.prestoxbasopp.ide.modules

import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths

interface XbProjectPathResolver {
    fun resolveBasePath(project: Project): Path?
}

class XbDefaultProjectPathResolver : XbProjectPathResolver {
    override fun resolveBasePath(project: Project): Path? {
        val basePath = project.basePath ?: return null
        return Paths.get(basePath)
    }
}
