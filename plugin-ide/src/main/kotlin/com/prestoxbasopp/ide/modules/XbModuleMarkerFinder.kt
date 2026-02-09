package com.prestoxbasopp.ide.modules

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.toList

open class XbModuleMarkerFinder(private val markerFiles: Set<String>) {
    open fun findMarkers(baseDir: Path): List<Path> {
        return try {
            Files.walk(baseDir)
                .filter { path -> path.isRegularFile() && markerFiles.contains(path.name) }
                .toList()
        } catch (ex: IOException) {
            emptyList()
        }
    }
}
