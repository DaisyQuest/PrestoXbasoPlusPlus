package com.prestoxbasopp.ide.dbf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReverseEngineeringModelTest {
    @Test
    fun `default tabs include all reverse engineering workflow tabs in order`() {
        assertThat(ReverseEngineeringWorkflow.defaultTabs().map { it.title }).containsExactly(
            "Overview",
            "Inputs",
            "Metadata (Phase 1 Results)",
            "Model Mapping",
            "Relations",
            "API Surface",
            "Generation & Output",
            "Preview",
            "Validation",
            "Run/Logs",
        )
    }

    @Test
    fun `extract metadata infers primary key foreign keys defaults and warnings`() {
        val table = table(
            fields = listOf(
                DbfFieldDescriptor("ID", DbfFieldType.Numeric, 8, 0, 0, false),
                DbfFieldDescriptor("OWNER_ID", DbfFieldType.Numeric, 8, 0, 0, false),
                DbfFieldDescriptor("NAME", DbfFieldType.Character, 20, 0, 0, false),
            ),
            records = mutableListOf(
                DbfRecord(false, mutableMapOf("ID" to "1", "OWNER_ID" to "3", "NAME" to "DOG")),
                DbfRecord(false, mutableMapOf("ID" to "2", "OWNER_ID" to "", "NAME" to "DOG")),
            ),
        )

        val metadata = ReverseEngineeringWorkflow.extractMetadata("DOG_TABLE", "fixtures/dog.dbf", table)

        assertThat(metadata.candidatePrimaryKey).isEqualTo("ID")
        assertThat(metadata.fields.single { it.originalFieldName == "OWNER_ID" }.nullableHint).isTrue()
        assertThat(metadata.fields.single { it.originalFieldName == "NAME" }.defaultValueHint).isEqualTo("DOG")
        assertThat(metadata.fields.single { it.originalFieldName == "ID" }.indexingHint).isEqualTo("PRIMARY")
        val relation = metadata.candidateForeignKeys.single()
        assertThat(relation.targetTable).isEqualTo("OWNER")
        assertThat(relation.sourceFields).containsExactly("OWNER_ID")
        assertThat(relation.cardinality).isEqualTo(RelationCardinality.MANY_TO_ONE)
        assertThat(metadata.warnings).isEmpty()
    }

    @Test
    fun `extract metadata warns for empty table and width zero`() {
        val table = table(
            fields = listOf(DbfFieldDescriptor("ZERO", DbfFieldType.Character, 0, 0, 0, false)),
            records = mutableListOf(),
        )

        val metadata = ReverseEngineeringWorkflow.extractMetadata("EMPTY", "fixtures/empty.dbf", table)

        assertThat(metadata.warnings).anySatisfy { assertThat(it).contains("No records") }
        assertThat(metadata.warnings).anySatisfy { assertThat(it).contains("width 0") }
    }

    @Test
    fun `generate produces read-only and relational method surfaces respecting aliases and table overrides`() {
        val metadata = ReverseEngineeringWorkflow.toBundle(
            DbfTableMetadata(
                tableName = "DOG_TABLE",
                sourcePath = "fixtures/dog.dbf",
                checksum = "abc",
                fields = listOf(
                    DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, "PRIMARY"),
                    DbfFieldMetadata("NAME", DbfFieldType.Character, 20, 0, false, null, null),
                ),
                candidatePrimaryKey = "ID",
                candidateForeignKeys = emptyList(),
                warnings = emptyList(),
            ),
            DbfTableMetadata(
                tableName = "CAT_TABLE",
                sourcePath = "fixtures/cat.dbf",
                checksum = "def",
                fields = listOf(DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, "PRIMARY")),
                candidatePrimaryKey = "ID",
                candidateForeignKeys = emptyList(),
                warnings = emptyList(),
            ),
        )
        val config = ReverseEngineerConfig(
            schemaVersion = "1.0.0",
            engineVersion = "1.0.0",
            profile = ApiProfile.CRUD_RELATIONAL,
            outputDir = "out",
            generateMethodAliases = true,
            relations = listOf(
                DbfRelationMetadata("DOG_TABLE", "OWNER", listOf("OWNER_ID"), listOf("ID"), RelationCardinality.MANY_TO_ONE, null),
            ),
            tableConfigs = listOf(
                TableGenerationConfig(
                    tableName = "DOG_TABLE",
                    className = "Dog",
                    includeFields = setOf("NAME"),
                    aliasByField = mapOf("NAME" to "n"),
                    readOnly = false,
                ),
                TableGenerationConfig(
                    tableName = "CAT_TABLE",
                    className = "Cat",
                    includeFields = setOf("ID"),
                    aliasByField = emptyMap(),
                    readOnly = true,
                ),
            ),
        )

        val (artifacts, report) = ReverseEngineeringWorkflow.generate(metadata, config)

        assertThat(report.generatedFileCount).isEqualTo(2)
        val dog = artifacts.single { it.className == "Dog" }
        assertThat(dog.methods.map { it.methodName }).contains("insert", "update", "upsert", "delete", "getOwner", "addOwner", "removeOwner")
        assertThat(dog.methods.mapNotNull { it.alias }).contains("l", "f", "i", "u", "us", "d")
        assertThat(dog.source).contains("VAR NAME // alias: n")
        assertThat(dog.source).doesNotContain("VAR ID")

        val cat = artifacts.single { it.className == "Cat" }
        assertThat(cat.methods.map { it.methodName }).containsExactly("load", "findBy")
        assertThat(cat.methods.mapNotNull { it.alias }).containsExactly("l", "f")
    }

    @Test
    fun `generate skips blank class names and supports disabled aliases`() {
        val metadata = ReverseEngineeringWorkflow.toBundle(
            DbfTableMetadata(
                tableName = "DOG",
                sourcePath = "fixtures/dog.dbf",
                checksum = "abc",
                fields = listOf(DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, null)),
                candidatePrimaryKey = "ID",
                candidateForeignKeys = emptyList(),
                warnings = emptyList(),
            ),
            DbfTableMetadata(
                tableName = "BAD",
                sourcePath = "fixtures/bad.dbf",
                checksum = "def",
                fields = listOf(DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, null)),
                candidatePrimaryKey = "ID",
                candidateForeignKeys = emptyList(),
                warnings = emptyList(),
            ),
        )
        val config = ReverseEngineerConfig(
            schemaVersion = "1.0.0",
            engineVersion = "1.0.0",
            profile = ApiProfile.FULL,
            outputDir = "out",
            generateMethodAliases = false,
            relations = emptyList(),
            tableConfigs = listOf(
                TableGenerationConfig("BAD", "", setOf("ID"), emptyMap()),
            ),
        )

        val (artifacts, report) = ReverseEngineeringWorkflow.generate(metadata, config)

        assertThat(artifacts).hasSize(1)
        assertThat(artifacts.single().className).isEqualTo("Dog")
        assertThat(artifacts.single().methods.map { it.alias }).doesNotContainAnyElementsOf(listOf("l", "f", "i"))
        assertThat(report.warnings.single()).contains("Skipped table 'BAD'")
    }

    private fun table(fields: List<DbfFieldDescriptor>, records: MutableList<DbfRecord>) = DbfTable(
        header = DbfHeader(3, 124, 2, 1, records.size, 100, 10, 0, 0, false, 0),
        fields = fields,
        records = records,
    )
}
