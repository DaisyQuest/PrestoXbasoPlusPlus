package com.prestoxbasopp.ide.modules

class XbModuleCandidateConsolidator {
    fun consolidate(candidates: List<XbModuleCandidate>): List<XbModuleCandidate> {
        if (candidates.isEmpty()) return emptyList()

        val grouped = candidates.groupBy { it.rootPath.normalize().toAbsolutePath() }
        return grouped.values.map { group ->
            group.maxByOrNull { candidatePriority(it) } ?: group.first()
        }.sortedBy { it.suggestedName }
    }

    private fun candidatePriority(candidate: XbModuleCandidate): Int {
        return when {
            candidate.reason.startsWith("Detected") -> 2
            candidate.reason.contains("sources", ignoreCase = true) -> 1
            else -> 0
        }
    }
}
