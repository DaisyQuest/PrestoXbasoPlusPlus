package com.prestoxbasopp.ide.modules

interface XbModuleDetectionStrategy {
    fun detectCandidates(baseDir: java.nio.file.Path): List<XbModuleCandidate>
}
