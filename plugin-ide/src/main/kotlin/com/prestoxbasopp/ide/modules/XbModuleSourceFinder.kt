package com.prestoxbasopp.ide.modules

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.streams.toList

class XbModuleSourceFinder(private val extensions: Set<String>) {
    private val excludedRoots = setOf(".idea", "build", "out", ".gradle")

    fun findSourceRoots(baseDir: Path): List<Path> {
        return try {
            Files.walk(baseDir)
                .filter { path ->
                    Files.isRegularFile(path) &&
                        extensions.contains(path.extension.lowercase()) &&
                        !isExcluded(path)
                }
                .mapNotNull { path -> path.parent }
                .toList()
        } catch (ex: IOException) {
            emptyList()
        }
    }

    private fun isExcluded(path: Path): Boolean {
        return path.iterator().asSequence().any { excludedRoots.contains(it.name) }
    }
}
