package com.prestoxbasopp.ide.dbf

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.prestoxbasopp.ide.XbCompletionMetadata
import com.prestoxbasopp.ide.XbTableColumnMetadata
import com.prestoxbasopp.ide.XbTableMetadata
import java.nio.file.Files
import java.nio.file.Path

private data class XbDbfModuleTable(
    val tableName: String = "",
    val fields: List<DbfFieldDescriptorSnapshot> = emptyList(),
)

private data class DbfFieldDescriptorSnapshot(
    val name: String = "",
    val type: String = "",
    val length: Int = 0,
)

private typealias XbDbfModuleCatalog = MutableMap<String, MutableList<XbDbfModuleTable>>

class XbDbfModuleCatalogService(
    private val gson: Gson = Gson(),
    private val catalogFileName: String = "xbasepp.dbf-modules.json",
) {
    fun registerImport(project: Project, dbfFile: Path, table: DbfTable) {
        val moduleName = resolveModuleName(project, dbfFile)
        val projectBasePath = project.basePath?.let(Path::of) ?: return
        registerImport(projectBasePath, moduleName, dbfFile, table)
    }

    fun registerImport(projectBasePath: Path, moduleName: String, dbfFile: Path, table: DbfTable) {
        val catalogPath = projectBasePath.resolve(catalogFileName)
        val catalog = loadCatalog(catalogPath)
        val tables = catalog.getOrPut(moduleName) { mutableListOf() }
        val tableName = dbfFile.fileName.toString().substringBeforeLast('.').ifBlank { "UNKNOWN_TABLE" }
        val snapshot = XbDbfModuleTable(
            tableName = tableName,
            fields = table.fields.map { DbfFieldDescriptorSnapshot(it.name, it.type.name, it.length) },
        )
        val existing = tables.indexOfFirst { it.tableName.equals(tableName, ignoreCase = true) }
        if (existing >= 0) {
            tables[existing] = snapshot
        } else {
            tables += snapshot
        }
        saveCatalog(catalogPath, catalog)
    }

    fun loadAsCompletionMetadata(projectBasePath: Path?): XbCompletionMetadata {
        if (projectBasePath == null) return XbCompletionMetadata()
        val catalogPath = projectBasePath.resolve(catalogFileName)
        if (!Files.exists(catalogPath)) return XbCompletionMetadata()
        val catalog = loadCatalog(catalogPath)
        val tables = catalog.values.flatten().map { table ->
            XbTableMetadata(
                name = table.tableName,
                columns = table.fields.map { field ->
                    XbTableColumnMetadata(
                        name = field.name,
                        type = field.type,
                        length = field.length,
                    )
                },
            )
        }
        return XbCompletionMetadata(tables = tables)
    }

    private fun resolveModuleName(project: Project, dbfFile: Path): String {
        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(dbfFile)
        val module = virtualFile?.let { ModuleUtilCore.findModuleForFile(it, project) }
        return module?.name ?: "PROJECT"
    }

    private fun loadCatalog(path: Path): XbDbfModuleCatalog {
        if (!Files.exists(path)) return linkedMapOf()
        return try {
            Files.newBufferedReader(path).use { reader ->
                val type = object : TypeToken<LinkedHashMap<String, MutableList<XbDbfModuleTable>>>() {}.type
                gson.fromJson<XbDbfModuleCatalog>(reader, type) ?: linkedMapOf()
            }
        } catch (_: JsonSyntaxException) {
            linkedMapOf()
        }
    }

    private fun saveCatalog(path: Path, catalog: XbDbfModuleCatalog) {
        Files.newBufferedWriter(path).use { writer ->
            gson.toJson(catalog, writer)
        }
    }
}
