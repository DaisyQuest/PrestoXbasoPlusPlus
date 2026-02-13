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
        assertThat(dog.macros).contains("#define DOG_METHOD_INSERT \"insert\"", "#define DOG_ALIAS_INSERT \"i\"")
        assertThat(dog.source).contains("VAR NAME // alias: n")
        assertThat(dog.source).contains("CLASS METHOD load(...)")
        assertThat(dog.source).contains("METHOD getName() INLINE ::NAME")
        assertThat(dog.source).contains("METHOD setName(value) INLINE (::NAME := value)")
        assertThat(dog.source).contains("METHOD save()")
        assertThat(dog.source).contains("METHOD remove()")
        assertThat(dog.source).contains("METHOD normalizeForPersistence()")
        assertThat(dog.source).contains("METHOD Dog:getPrimaryKeyValue()")
        assertThat(dog.source).contains("RETURN ::ID")
        assertThat(dog.source).contains("CLASS METHOD Dog:insert(entity, options)")
        assertThat(dog.source).contains("CLASS METHOD Dog:update(entity, options)")
        assertThat(dog.source).contains("CLASS METHOD Dog:upsert(entity, options)")
        assertThat(dog.source).contains("CLASS METHOD Dog:delete(entityOrKey, options)")
        assertThat(dog.source).contains("CLASS METHOD Dog:openRepository(options)")
        assertThat(dog.source).contains("METHOD Dog:init(data)")
        assertThat(dog.source).contains("#define DOG_FIELD_NAME \"NAME\"")
        assertThat(dog.source).contains("VAR ID")

        val cat = artifacts.single { it.className == "Cat" }
        assertThat(cat.methods.map { it.methodName }).containsExactly("load", "findBy")
        assertThat(cat.methods.mapNotNull { it.alias }).containsExactly("l", "f")
        assertThat(cat.source).contains("#define CAT_ALIAS_LOAD \"l\"", "#define CAT_METHOD_FINDBY \"findBy\"")
        assertThat(cat.source).contains("METHOD refresh()")
        assertThat(cat.source).contains("METHOD Cat:getPrimaryKeyValue()")
        assertThat(cat.source).contains("RETURN ::ID")
        assertThat(cat.source).doesNotContain("METHOD save()")
        assertThat(cat.source).doesNotContain("METHOD remove()")
    }

    @Test
    fun `generate emits production persistence details including coercion and repository resolution`() {
        val metadata = ReverseEngineeringWorkflow.toBundle(
            DbfTableMetadata(
                tableName = "INVOICE",
                sourcePath = "fixtures/invoice.dbf",
                checksum = "xyz",
                fields = listOf(
                    DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, "PRIMARY"),
                    DbfFieldMetadata("AMOUNT", DbfFieldType.Numeric, 10, 2, false, null, null),
                    DbfFieldMetadata("PAID", DbfFieldType.Logical, 1, 0, false, null, null),
                    DbfFieldMetadata("DUE_DATE", DbfFieldType.Date, 8, 0, true, null, null),
                    DbfFieldMetadata("NOTE", DbfFieldType.Character, 20, 0, true, null, null),
                ),
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
            generateMethodAliases = true,
            relations = emptyList(),
            tableConfigs = emptyList(),
        )

        val artifact = ReverseEngineeringWorkflow.generate(metadata, config).first.single()

        assertThat(artifact.source).contains("payload[\"AMOUNT\"] := Val(value)")
        assertThat(artifact.source).contains("payload[\"PAID\"] := iif(Upper(AllTrim(value)) == \"T\"")
        assertThat(artifact.source).contains("payload[\"DUE_DATE\"] := iif(Empty(value), NIL, CToD(value))")
        assertThat(artifact.source).contains("payload[\"NOTE\"] := iif(Empty(value), NIL, AllTrim(value))")
        assertThat(artifact.source).contains("provider := DaoRepositoryProvider():default()")
        assertThat(artifact.source).contains("LOCAL rows := repo:findBy(::tableName(), iif(ValType(criteria) == \"H\", criteria, {=>}), options)")
    }

    @Test
    fun `generate handles table without candidate primary key`() {
        val metadata = ReverseEngineeringWorkflow.toBundle(
            DbfTableMetadata(
                tableName = "LOGS",
                sourcePath = "fixtures/logs.dbf",
                checksum = "nopk",
                fields = listOf(DbfFieldMetadata("MESSAGE", DbfFieldType.Character, 40, 0, false, null, null)),
                candidatePrimaryKey = null,
                candidateForeignKeys = emptyList(),
                warnings = emptyList(),
            ),
        )
        val config = ReverseEngineerConfig(
            schemaVersion = "1.0.0",
            engineVersion = "1.0.0",
            profile = ApiProfile.READ_ONLY,
            outputDir = "out",
            generateMethodAliases = false,
            relations = emptyList(),
            tableConfigs = emptyList(),
        )

        val artifact = ReverseEngineeringWorkflow.generate(metadata, config).first.single()

        assertThat(artifact.source).contains("METHOD Logs:getPrimaryKeyValue()")
        assertThat(artifact.source).contains("RETURN NIL")
        assertThat(artifact.source).doesNotContain("VAR ID")
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
        assertThat(artifacts.single().macros.joinToString("\n")).doesNotContain("ALIAS")
        assertThat(artifacts.single().source).contains("CLASS METHOD load(...)")
        assertThat(artifacts.single().source).doesNotContain("METHOD l(...)")
        assertThat(report.warnings.single()).contains("Skipped table 'BAD'")
    }

    @Test
    fun `generate includes relation instance helpers inferred from metadata and carries schema version`() {
        val metadata = ReverseEngineeringWorkflow.toBundle(
            DbfTableMetadata(
                tableName = "DOG",
                sourcePath = "fixtures/dog.dbf",
                checksum = "abc",
                fields = listOf(
                    DbfFieldMetadata("ID", DbfFieldType.Numeric, 8, 0, false, null, "PRIMARY"),
                    DbfFieldMetadata("OWNER_ID", DbfFieldType.Numeric, 8, 0, false, null, "FOREIGN_KEY"),
                ),
                candidatePrimaryKey = "ID",
                candidateForeignKeys = listOf(
                    DbfRelationMetadata("DOG", "OWNER", listOf("OWNER_ID"), listOf("ID"), RelationCardinality.MANY_TO_ONE, null),
                ),
                warnings = emptyList(),
            ),
        )
        val config = ReverseEngineerConfig(
            schemaVersion = "2.5.0",
            engineVersion = "1.0.0",
            profile = ApiProfile.FULL,
            outputDir = "out",
            generateMethodAliases = true,
            relations = listOf(
                DbfRelationMetadata("DOG", "OWNER", listOf("OWNER_ID"), listOf("ID"), RelationCardinality.MANY_TO_ONE, null),
            ),
            tableConfigs = emptyList(),
        )

        val (artifacts, report) = ReverseEngineeringWorkflow.generate(metadata, config)

        assertThat(report.schemaVersion).isEqualTo("2.5.0")
        assertThat(artifacts.single().source).contains("METHOD bindOwner(target) INLINE (::OWNER_ID := target:ID)")
        assertThat(artifacts.single().source).contains("METHOD unbindOwner() INLINE (::OWNER_ID := NIL)")
    }

    private fun table(fields: List<DbfFieldDescriptor>, records: MutableList<DbfRecord>) = DbfTable(
        header = DbfHeader(3, 124, 2, 1, records.size, 100, 10, 0, 0, false, 0),
        fields = fields,
        records = records,
    )
}
