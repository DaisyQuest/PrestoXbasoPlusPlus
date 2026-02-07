package com.prestoxbasopp.ide.modules

import java.nio.file.Path

class XbModuleSourceDetectionStrategy(
    private val sourceFinder: XbModuleSourceFinder,
    private val namingPolicy: XbModuleNamingPolicy,
) : XbModuleDetectionStrategy {
    override fun detectCandidates(baseDir: Path): List<XbModuleCandidate> {
        return sourceFinder.findSourceRoots(baseDir).map { root ->
            XbModuleCandidate(
                rootPath = root,
                suggestedName = namingPolicy.suggestName(root),
                reason = "Contains Xbase++ sources",
            )
        }
    }
}
