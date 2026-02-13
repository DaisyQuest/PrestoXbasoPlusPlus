package com.prestoxbasopp.ide.dbf

class UltraDbfEditorModel(private val table: DbfTable) {
    fun fields(): List<DbfFieldDescriptor> = table.fields

    fun records(includeDeleted: Boolean = true): List<DbfRecord> =
        if (includeDeleted) table.records else table.records.filterNot { it.deleted }

    fun updateValue(row: Int, fieldName: String, value: String) {
        table.records[row].values[fieldName] = value
    }

    fun addRecord(defaultValues: Map<String, String> = emptyMap()) {
        val baseline = table.fields.associate { it.name to "" }.toMutableMap()
        defaultValues.forEach { (key, value) -> if (baseline.containsKey(key)) baseline[key] = value }
        table.records += DbfRecord(deleted = false, values = baseline)
    }

    fun toggleDeleted(row: Int) {
        val current = table.records[row]
        table.records[row] = current.copy(deleted = !current.deleted)
    }

    fun completionSuggestions(field: DbfFieldDescriptor, prefix: String): List<String> {
        val normalizedPrefix = prefix.trim()
        val staticSuggestions = when (field.type) {
            DbfFieldType.Logical -> listOf("Y", "N", "T", "F", "?")
            DbfFieldType.Date -> listOf("YYYYMMDD")
            DbfFieldType.Numeric, DbfFieldType.FloatingPoint -> listOf("0", "-1", "3.14")
            DbfFieldType.Memo -> listOf("0000000001", "0000000002")
            DbfFieldType.Character -> emptyList()
        }
        val learned = table.records
            .mapNotNull { it.values[field.name] }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return (staticSuggestions + learned)
            .distinct()
            .filter { normalizedPrefix.isEmpty() || it.startsWith(normalizedPrefix, ignoreCase = true) }
            .sortedBy { it.length }
    }

    fun filteredRecords(includeDeleted: Boolean, filters: Map<String, String>): List<DbfRecord> {
        val normalized = filters
            .mapValues { (_, value) -> value.trim() }
            .filterValues { it.isNotEmpty() }
        if (normalized.isEmpty()) {
            return records(includeDeleted)
        }
        return records(includeDeleted).filter { record ->
            normalized.all { (fieldName, expected) ->
                record.values[fieldName]
                    ?.contains(expected, ignoreCase = true)
                    ?: false
            }
        }
    }

    fun visibleRecordIndex(
        includeDeleted: Boolean,
        filters: Map<String, String>,
        pageIndex: Int,
    ): Int {
        val records = filteredRecords(includeDeleted, filters)
        if (records.isEmpty()) return -1
        return pageIndex.coerceIn(0, records.lastIndex)
    }

    fun snapshot(): DbfTable = table.copy(records = table.records.map { it.copy(values = it.values.toMutableMap()) }.toMutableList())
}
