package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class XbDbfModuleCatalogServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads empty metadata when no catalog exists`() {
        val service = XbDbfModuleCatalogService(catalogFileName = "catalog.json")

        val metadata = service.loadAsCompletionMetadata(tempDir)

        assertThat(metadata.tables).isEmpty()
    }

    @Test
    fun `registers imports and exposes completion table metadata`() {
        val service = XbDbfModuleCatalogService(catalogFileName = "catalog.json")
        val dbfFile = tempDir.resolve("CUSTOMERS.dbf")
        dbfFile.toFile().writeText("placeholder")

        service.registerImport(tempDir, "SalesModule", dbfFile, sampleTable())

        val metadata = service.loadAsCompletionMetadata(tempDir)
        assertThat(metadata.tables).hasSize(1)
        assertThat(metadata.tables.first().name).isEqualTo("CUSTOMERS")
        assertThat(metadata.tables.first().columns.map { it.name }).containsExactly("NAME", "FLAG", "WHEN", "TOTAL", "MEMO")
    }

    @Test
    fun `handles malformed catalog files gracefully`() {
        val catalogPath = tempDir.resolve("catalog.json")
        catalogPath.toFile().writeText("{not json}")
        val service = XbDbfModuleCatalogService(catalogFileName = "catalog.json")

        val metadata = service.loadAsCompletionMetadata(tempDir)

        assertThat(metadata.tables).isEmpty()
    }

    private fun sampleTable(): DbfTable = DbfTable(
        header = DbfHeader(3, 124, 1, 1, 2, 97, 20, 0, 0, false, 0),
        fields = listOf(
            DbfFieldDescriptor("NAME", DbfFieldType.Character, 10, 0, 0, false),
            DbfFieldDescriptor("FLAG", DbfFieldType.Logical, 1, 0, 0, false),
            DbfFieldDescriptor("WHEN", DbfFieldType.Date, 8, 0, 0, false),
            DbfFieldDescriptor("TOTAL", DbfFieldType.Numeric, 8, 2, 0, false),
            DbfFieldDescriptor("MEMO", DbfFieldType.Memo, 10, 0, 0, false),
        ),
        records = mutableListOf(),
    )
}
