package com.prestoxbasopp.ide.modules

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class XbModuleDiscoveryService(
    private val strategies: List<XbModuleDetectionStrategy>,
    private val consolidator: XbModuleCandidateConsolidator,
) {
    companion object {
        fun createDefault(project: Project): XbModuleDiscoveryService {
            val markers = setOf("project.xpj", "xbasepp.yml", "xbasepp.yaml")
            val markerFinder = XbModuleMarkerFinder(markers)
            val namingPolicy = XbModuleNamingPolicy()
            val strategy = XbModuleMarkerDetectionStrategy(markerFinder, namingPolicy)
            val sourceFinder = XbModuleSourceFinder(setOf("xb", "prg", "ch"))
            val sourceStrategy = XbModuleSourceDetectionStrategy(sourceFinder, namingPolicy)
            val consolidator = XbModuleCandidateConsolidator()
            return XbModuleDiscoveryService(listOf(strategy, sourceStrategy), consolidator)
        }
    }

    fun discover(baseDir: Path): List<XbModuleCandidate> =
        consolidator.consolidate(strategies.flatMap { it.detectCandidates(baseDir) })
}
