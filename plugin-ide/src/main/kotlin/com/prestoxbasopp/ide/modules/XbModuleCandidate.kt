package com.prestoxbasopp.ide.modules

import java.nio.file.Path

data class XbModuleCandidate(
    val rootPath: Path,
    val suggestedName: String,
    val reason: String,
) {
    fun displayLabel(): String = "$suggestedName (${rootPath.toAbsolutePath()})"
}
