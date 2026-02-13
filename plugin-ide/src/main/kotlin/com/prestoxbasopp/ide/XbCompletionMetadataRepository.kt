package com.prestoxbasopp.ide

import com.prestoxbasopp.ide.dbf.XbDbfModuleCatalogService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

fun interface XbCompletionMetadataProvider {
    fun load(projectBasePath: Path?): XbCompletionMetadata
}

class XbCompletionMetadataRepository(
    private val loader: XbCompletionMetadataLoader = XbCompletionMetadataLoader(),
    private val dbfCatalogService: XbDbfModuleCatalogService = XbDbfModuleCatalogService(),
    private val resourcePath: String = "/completion/metadata.json",
    private val projectFileName: String = "xbasepp.completion.json",
) : XbCompletionMetadataProvider {
    private val cache = mutableMapOf<Path?, CachedMetadata>()

    override fun load(projectBasePath: Path?): XbCompletionMetadata {
        val dbfMetadata = dbfCatalogService.loadAsCompletionMetadata(projectBasePath)
        val projectFile = projectBasePath?.resolve(projectFileName)
        if (projectFile != null && Files.exists(projectFile)) {
            return loadProjectMetadata(projectFile).merge(dbfMetadata)
        }
        return loadBundledMetadata().merge(dbfMetadata)
    }

    private fun loadProjectMetadata(path: Path): XbCompletionMetadata {
        val lastModified = Files.getLastModifiedTime(path)
        val cached = cache[path]
        if (cached != null && cached.lastModified == lastModified) {
            return cached.metadata
        }
        val metadata = Files.newBufferedReader(path).use { reader -> loader.load(reader) }
        cache[path] = CachedMetadata(metadata, lastModified)
        return metadata
    }

    private fun loadBundledMetadata(): XbCompletionMetadata {
        val cached = cache[null]
        if (cached != null) {
            return cached.metadata
        }
        val metadata = javaClass.getResourceAsStream(resourcePath)?.use { stream ->
            stream.reader().use { reader -> loader.load(reader) }
        } ?: XbCompletionMetadata()
        cache[null] = CachedMetadata(metadata, FileTime.fromMillis(0))
        return metadata
    }

    private data class CachedMetadata(
        val metadata: XbCompletionMetadata,
        val lastModified: FileTime,
    )

    private fun XbCompletionMetadata.merge(overlay: XbCompletionMetadata): XbCompletionMetadata {
        if (overlay.commands.isEmpty() && overlay.tables.isEmpty()) return this
        return XbCompletionMetadata(
            commands = commands + overlay.commands,
            tables = tables + overlay.tables,
        )
    }
}
