package com.prestoxbasopp.ide.modules

import java.nio.file.Path

class XbModuleMarkerDetectionStrategy(
    private val markerFinder: XbModuleMarkerFinder,
    private val namingPolicy: XbModuleNamingPolicy,
) : XbModuleDetectionStrategy {
    override fun detectCandidates(baseDir: Path): List<XbModuleCandidate> {
        val markers = markerFinder.findMarkers(baseDir)
        return markers.map { marker ->
            val moduleRoot = marker.parent ?: baseDir
            XbModuleCandidate(
                rootPath = moduleRoot,
                suggestedName = namingPolicy.suggestName(moduleRoot),
                reason = "Detected ${marker.name}",
            )
        }
    }
}
