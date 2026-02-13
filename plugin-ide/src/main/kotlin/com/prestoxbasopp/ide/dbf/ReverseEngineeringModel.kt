package com.prestoxbasopp.ide.dbf

enum class ReverseEngineeringTab(val title: String) {
    Overview("Overview"),
    Inputs("Inputs"),
    Metadata("Metadata (Phase 1 Results)"),
    ModelMapping("Model Mapping"),
    Relations("Relations"),
    ApiSurface("API Surface"),
    GenerationOutput("Generation & Output"),
    Preview("Preview"),
    Validation("Validation"),
    RunLogs("Run/Logs"),
}

enum class ApiProfile {
    READ_ONLY,
    CRUD_BASIC,
    CRUD_RELATIONAL,
    FULL,
}

enum class RelationCardinality {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
}

data class DbfFieldMetadata(
    val originalFieldName: String,
    val inferredType: DbfFieldType,
    val width: Int,
    val precision: Int,
    val nullableHint: Boolean,
    val defaultValueHint: String?,
    val indexingHint: String?,
)

data class DbfRelationMetadata(
    val sourceTable: String,
    val targetTable: String,
    val sourceFields: List<String>,
    val targetFields: List<String>,
    val cardinality: RelationCardinality,
    val cascadePolicy: String?,
)

data class DbfTableMetadata(
    val tableName: String,
    val sourcePath: String,
    val checksum: String,
    val fields: List<DbfFieldMetadata>,
    val candidatePrimaryKey: String?,
    val candidateForeignKeys: List<DbfRelationMetadata>,
    val warnings: List<String>,
)

data class DbfMetadataBundle(
    val schemaVersion: String,
    val engineVersion: String,
    val tables: List<DbfTableMetadata>,
)

data class TableGenerationConfig(
    val tableName: String,
    val className: String,
    val includeFields: Set<String>,
    val aliasByField: Map<String, String>,
    val readOnly: Boolean = false,
)

data class ReverseEngineerConfig(
    val schemaVersion: String,
    val engineVersion: String,
    val profile: ApiProfile,
    val outputDir: String,
    val generateMethodAliases: Boolean,
    val relations: List<DbfRelationMetadata>,
    val tableConfigs: List<TableGenerationConfig>,
)

data class GeneratedMethod(val methodName: String, val alias: String?)

data class GeneratedClassArtifact(
    val className: String,
    val source: String,
    val methods: List<GeneratedMethod>,
    val macros: List<String>,
)

data class DbfGenerationReport(
    val schemaVersion: String,
    val engineVersion: String,
    val generatedFileCount: Int,
    val warnings: List<String>,
)

object ReverseEngineeringWorkflow {
    fun defaultTabs(): List<ReverseEngineeringTab> = ReverseEngineeringTab.entries

    fun extractMetadata(tableName: String, sourcePath: String, table: DbfTable): DbfTableMetadata {
        val fields = table.fields.map { field ->
            DbfFieldMetadata(
                originalFieldName = field.name,
                inferredType = field.type,
                width = field.length,
                precision = field.decimalCount,
                nullableHint = table.records.any { it.values[field.name].isNullOrBlank() },
                defaultValueHint = inferDefaultValueHint(field, table),
                indexingHint = inferIndexHint(field),
            )
        }
        val warnings = buildList {
            if (table.records.isEmpty()) add("No records found for table '$tableName'.")
            fields.filter { it.width == 0 }.forEach { add("Field '${it.originalFieldName}' has width 0.") }
        }
        return DbfTableMetadata(
            tableName = tableName,
            sourcePath = sourcePath,
            checksum = computeChecksum(table),
            fields = fields,
            candidatePrimaryKey = inferPrimaryKey(table.fields),
            candidateForeignKeys = inferForeignKeys(tableName, table.fields),
            warnings = warnings,
        )
    }

    fun toBundle(vararg tables: DbfTableMetadata): DbfMetadataBundle = DbfMetadataBundle(
        schemaVersion = "1.0.0",
        engineVersion = "1.0.0",
        tables = tables.toList(),
    )

    fun generate(metadata: DbfMetadataBundle, config: ReverseEngineerConfig): Pair<List<GeneratedClassArtifact>, DbfGenerationReport> {
        val warnings = mutableListOf<String>()
        val generated = metadata.tables.mapNotNull { table ->
            val override = config.tableConfigs.firstOrNull { it.tableName.equals(table.tableName, ignoreCase = true) }
            val className = override?.className ?: table.tableName.toClassName()
            if (className.isBlank()) {
                warnings += "Skipped table '${table.tableName}' due to blank class name."
                return@mapNotNull null
            }
            val effectiveProfile = if (override?.readOnly == true) ApiProfile.READ_ONLY else config.profile
            val methods = methodsForProfile(effectiveProfile, table, config.generateMethodAliases, config.relations)
            val selectedFields = override?.includeFields?.ifEmpty { table.fields.map { it.originalFieldName }.toSet() }
                ?: table.fields.map { it.originalFieldName }.toSet()
            val requiredPersistenceFields = setOfNotNull(table.candidatePrimaryKey)
            val effectiveFields = selectedFields + requiredPersistenceFields
            val aliasByField = override?.aliasByField.orEmpty()
            val macros = buildMacros(className, effectiveFields, methods)
            val source = renderClass(className, table, effectiveProfile, effectiveFields, aliasByField, methods, macros)
            GeneratedClassArtifact(className, source, methods, macros)
        }
        return generated to DbfGenerationReport(
            schemaVersion = config.schemaVersion,
            engineVersion = config.engineVersion,
            generatedFileCount = generated.size,
            warnings = warnings,
        )
    }

    private fun methodsForProfile(
        profile: ApiProfile,
        table: DbfTableMetadata,
        aliasesEnabled: Boolean,
        relations: List<DbfRelationMetadata>,
    ): List<GeneratedMethod> {
        val rel = relations.filter { it.sourceTable.equals(table.tableName, ignoreCase = true) }
        val methods = mutableListOf(
            GeneratedMethod("load", aliasIf("l", aliasesEnabled)),
            GeneratedMethod("findBy", aliasIf("f", aliasesEnabled)),
        )
        if (profile != ApiProfile.READ_ONLY) {
            methods += listOf(
                GeneratedMethod("insert", aliasIf("i", aliasesEnabled)),
                GeneratedMethod("update", aliasIf("u", aliasesEnabled)),
                GeneratedMethod("upsert", aliasIf("us", aliasesEnabled)),
                GeneratedMethod("delete", aliasIf("d", aliasesEnabled)),
            )
        }
        if (profile == ApiProfile.CRUD_RELATIONAL || profile == ApiProfile.FULL) {
            rel.forEach { relation ->
                methods += GeneratedMethod("get${relation.targetTable.toClassName()}", null)
                methods += GeneratedMethod("add${relation.targetTable.toClassName()}", null)
                methods += GeneratedMethod("remove${relation.targetTable.toClassName()}", null)
            }
        }
        return methods
    }

    private fun inferPrimaryKey(fields: List<DbfFieldDescriptor>): String? {
        val names = fields.map { it.name }
        return names.firstOrNull { it.equals("ID", ignoreCase = true) }
            ?: names.firstOrNull { it.equals("NF", ignoreCase = true) }
            ?: names.firstOrNull { it.endsWith("_ID", ignoreCase = true) }
    }

    private fun inferForeignKeys(tableName: String, fields: List<DbfFieldDescriptor>): List<DbfRelationMetadata> =
        fields.filter { it.name.endsWith("_ID", ignoreCase = true) && !it.name.equals("ID", ignoreCase = true) }
            .map { field ->
                val target = field.name.removeSuffix("_ID")
                DbfRelationMetadata(
                    sourceTable = tableName,
                    targetTable = target,
                    sourceFields = listOf(field.name),
                    targetFields = listOf("ID"),
                    cardinality = RelationCardinality.MANY_TO_ONE,
                    cascadePolicy = null,
                )
            }

    private fun inferDefaultValueHint(field: DbfFieldDescriptor, table: DbfTable): String? {
        val nonBlank = table.records.mapNotNull { it.values[field.name]?.trim() }.filter { it.isNotEmpty() }
        if (nonBlank.isEmpty()) return null
        return nonBlank.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    private fun inferIndexHint(field: DbfFieldDescriptor): String? = when {
        field.name.equals("ID", ignoreCase = true) -> "PRIMARY"
        field.name.endsWith("_ID", ignoreCase = true) -> "FOREIGN_KEY"
        else -> null
    }

    private fun computeChecksum(table: DbfTable): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(table.header.versionByte.toByte())
        table.fields.forEach {
            digest.update(it.name.toByteArray())
            digest.update(it.type.code.code.toByte())
            digest.update(it.length.toByte())
        }
        table.records.forEach { record ->
            digest.update(if (record.deleted) 1 else 0)
            record.values.toSortedMap().forEach { (k, v) ->
                digest.update(k.toByteArray())
                digest.update(v.toByteArray())
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun renderClass(
        className: String,
        table: DbfTableMetadata,
        profile: ApiProfile,
        includedFields: Set<String>,
        aliasByField: Map<String, String>,
        methods: List<GeneratedMethod>,
        macros: List<String>,
    ): String {
        val primaryKey = table.candidatePrimaryKey
            ?: table.fields.firstOrNull { it.indexingHint.equals("PRIMARY", ignoreCase = true) }?.originalFieldName
        val fieldMetadataByName = table.fields.associateBy { it.originalFieldName }
        val macrosSection = macros.joinToString("\n")
        val fieldsSection = includedFields.sorted().joinToString("\n") { field ->
            val aliasComment = aliasByField[field]?.let { " // alias: $it" }.orEmpty()
            "    VAR $field$aliasComment"
        }
        val classMethodsSection = methods.filter { it.methodName in setOf("load", "findBy", "insert", "update", "upsert", "delete") }.joinToString("\n") { method ->
            val aliasLine = method.alias?.let { "\n    METHOD $it(...) INLINE ::${method.methodName}(...)" }.orEmpty()
            "    CLASS METHOD ${method.methodName}(...)$aliasLine"
        }
        val fieldHelpersSection = includedFields.sorted().joinToString("\n") { field ->
            val accessor = field.lowercase().replaceFirstChar { it.titlecase() }
            """
    METHOD get$accessor() INLINE ::$field
    METHOD set$accessor(value) INLINE (::${field} := value)
""".trimIndent()
        }
        val persistenceInstanceMethods = if (profile == ApiProfile.READ_ONLY) {
            """
    METHOD refresh()
    METHOD getPrimaryKeyValue()
""".trimIndent()
        } else {
            """
    METHOD save()
    METHOD remove()
    METHOD refresh()
    METHOD getPrimaryKeyValue()
    METHOD normalizeForPersistence()
""".trimIndent()
        }
        val relationMethods = table.candidateForeignKeys.joinToString("\n") { relation ->
            val targetClass = relation.targetTable.toClassName()
            """
    METHOD bind$targetClass(target) INLINE (::${relation.sourceFields.first()} := target:ID)
    METHOD unbind$targetClass() INLINE (::${relation.sourceFields.first()} := NIL)
""".trimIndent()
        }
        return """
$macrosSection

CLASS $className
$fieldsSection
    METHOD init(data)
$classMethodsSection
$fieldHelpersSection
$persistenceInstanceMethods
$relationMethods
ENDCLASS

METHOD $className:init(data)
    LOCAL payload := iif(ValType(data) == "H", data, {=>})
${includedFields.sorted().joinToString("\n") { "    ::$it := iif(HHasKey(payload, \"$it\"), payload[\"$it\"], NIL)" }}
RETURN Self

METHOD $className:getPrimaryKeyValue()
${if (primaryKey != null) "    RETURN ::$primaryKey" else "    RETURN NIL"}

METHOD $className:refresh()
    LOCAL key := ::getPrimaryKeyValue()
    IF Empty(key)
        RETURN NIL
    ENDIF
    RETURN ${className}:load(key, {=>})

${if (profile != ApiProfile.READ_ONLY) {
            """
METHOD $className:save()
    RETURN ${className}:upsert(Self, {=>})

METHOD $className:remove()
    RETURN ${className}:delete(::getPrimaryKeyValue(), {=>})

METHOD $className:normalizeForPersistence()
    LOCAL payload := {=>}
    LOCAL value
${includedFields.sorted().joinToString("\n") { field ->
                val metadata = fieldMetadataByName[field]
                val converter = when (metadata?.inferredType) {
                    DbfFieldType.Numeric, DbfFieldType.FloatingPoint -> "Val"
                    DbfFieldType.Logical -> "iif(Upper(AllTrim(value)) == \"T\" .OR. Upper(AllTrim(value)) == \"Y\" .OR. value == .T., .T., .F.)"
                    DbfFieldType.Date -> "CToD"
                    else -> "AllTrim"
                }
                val assignmentClause = if (metadata?.inferredType == DbfFieldType.Date) {
                    """
    DO CASE
    CASE ValType(value) == "D"
        payload["$field"] := value
    CASE ValType(value) == "C" .AND. !Empty(value)
        payload["$field"] := CToD(value)
    OTHERWISE
        payload["$field"] := NIL
    ENDCASE
""".trimIndent()
                } else if (metadata?.inferredType == DbfFieldType.Numeric || metadata?.inferredType == DbfFieldType.FloatingPoint) {
                    "    payload[\"$field\"] := iif(Empty(value), NIL, $converter(value))"
                } else if (metadata?.nullableHint == true) {
                    "    payload[\"$field\"] := iif(Empty(value), NIL, $converter(value))"
                } else {
                    "    payload[\"$field\"] := $converter(value)"
                }
                """
    value := ::$field
$assignmentClause
""".trimIndent()
            }}
    RETURN payload

CLASS METHOD $className:insert(entity, options)
    LOCAL repo := ::openRepository(options)
    LOCAL payload := iif(ValType(entity) == "O", entity:normalizeForPersistence(), entity)
    RETURN repo:insert(::tableName(), payload)

CLASS METHOD $className:update(entity, options)
    LOCAL repo := ::openRepository(options)
    LOCAL payload := iif(ValType(entity) == "O", entity:normalizeForPersistence(), entity)
    LOCAL key := NIL
    IF ValType(entity) == "O"
        key := entity:getPrimaryKeyValue()
    ELSEIF ValType(entity) == "H"
        IF ValType(options) == "H" .AND. HHasKey(options, "key")
            key := options["key"]
${if (primaryKey != null) {
                "        ELSEIF HHasKey(payload, \"$primaryKey\")\n            key := payload[\"$primaryKey\"]"
            } else ""}
        ENDIF
    ENDIF
    IF Empty(key)
        RETURN .F.
    ENDIF
    RETURN repo:update(::tableName(), key, payload)

CLASS METHOD $className:upsert(entity, options)
    LOCAL payload := iif(ValType(entity) == "O", entity:normalizeForPersistence(), entity)
    LOCAL repo := ::openRepository(options)
    LOCAL key := NIL
    IF ValType(entity) == "O"
        key := entity:getPrimaryKeyValue()
    ELSEIF ValType(entity) == "H"
        IF ValType(options) == "H" .AND. HHasKey(options, "key")
            key := options["key"]
${if (primaryKey != null) {
                "        ELSEIF HHasKey(payload, \"$primaryKey\")\n            key := payload[\"$primaryKey\"]"
            } else ""}
        ENDIF
    ENDIF
    IF Empty(key)
        RETURN repo:insert(::tableName(), payload)
    ENDIF
    RETURN repo:update(::tableName(), key, payload)

CLASS METHOD $className:delete(entityOrKey, options)
    LOCAL repo := ::openRepository(options)
    LOCAL key := iif(ValType(entityOrKey) == "O", entityOrKey:getPrimaryKeyValue(), entityOrKey)
    IF Empty(key)
        RETURN .F.
    ENDIF
    RETURN repo:delete(::tableName(), key)
""".trimIndent()
        } else ""}

CLASS METHOD $className:load(id, options)
    LOCAL repo := ::openRepository(options)
    LOCAL raw := repo:load(::tableName(), id)
    IF Empty(raw)
        RETURN NIL
    ENDIF
    RETURN $className():init(raw)

CLASS METHOD $className:findBy(criteria, options)
    LOCAL repo := ::openRepository(options)
    LOCAL rows := repo:findBy(::tableName(), iif(ValType(criteria) == "H", criteria, {=>}), options)
    LOCAL entities := {}
    AEval(rows, {|row| AAdd(entities, $className():init(row)) })
    RETURN entities

CLASS METHOD $className:tableName()
    RETURN "${table.tableName}"

CLASS METHOD $className:openRepository(options)
    LOCAL provider := iif(ValType(options) == "H" .AND. HHasKey(options, "repository"), options["repository"], NIL)
    IF provider == NIL
        provider := DaoRepositoryProvider():default()
    ENDIF
    RETURN provider
""".trim()
    }

    private fun buildMacros(
        className: String,
        includedFields: Set<String>,
        methods: List<GeneratedMethod>,
    ): List<String> = buildList {
        add("/* Generated API macros for $className */")
        includedFields.sorted().forEach { field ->
            add("#define ${className.uppercase()}_FIELD_${field.uppercase()} \"$field\"")
        }
        methods.forEach { method ->
            add("#define ${className.uppercase()}_METHOD_${method.methodName.uppercase()} \"${method.methodName}\"")
            if (method.alias != null) {
                add("#define ${className.uppercase()}_ALIAS_${method.methodName.uppercase()} \"${method.alias}\"")
            }
        }
    }

    private fun aliasIf(alias: String, enabled: Boolean): String? = alias.takeIf { enabled }

    private fun String.toClassName(): String = split('_', '-', ' ')
        .filter { it.isNotBlank() }
        .joinToString("") { token -> token.lowercase().replaceFirstChar { c -> c.titlecase() } }
}
